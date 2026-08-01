package cl.ignaciodiaz.dados.menu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cl.ignaciodiaz.dados.core.modelo.Categoria
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.RuleSet
import cl.ignaciodiaz.dados.core.modelo.Seccion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.persistencia.RepositorioPartida
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

// Cubre el menú de inicio (decisiones 44-46): "Continuar" solo con partida guardada,
// "Partida nueva" siempre, y confirmación antes de borrar solo cuando hay algo que
// perder de verdad (partida guardada y sin terminar).
class MenuScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class RepositorioPartidaEnMemoria(private var estado: EstadoPartida? = null) : RepositorioPartida {
        var vecesBorrado = 0
            private set

        override suspend fun guardar(estado: EstadoPartida) {
            this.estado = estado
        }

        override suspend fun cargar(): EstadoPartida? = estado

        override suspend fun borrar() {
            estado = null
            vecesBorrado++
        }
    }

    private fun ruleSetDeUnaCategoria() = RuleSet(
        categorias = listOf(
            Categoria(id = CategoriaId("unica"), seccion = Seccion.SUPERIOR, esValida = { true }, puntaje = { 5 })
        ),
        bonusSeccionSuperior = null
    )

    private fun estadoEnCurso() = EstadoPartida(
        jugadores = listOf(Jugador(nombre = "Jugador 1")),
        indiceTurno = 0,
        tiradaActual = null
    )

    private fun estadoTerminado() = EstadoPartida(
        jugadores = listOf(Jugador(nombre = "Jugador 1", anotaciones = mapOf(CategoriaId("unica") to 5))),
        indiceTurno = 0,
        tiradaActual = null
    )

    private fun motor() = MotorPartida(ruleSetDeUnaCategoria(), Random(1))

    @Test
    fun sinPartidaGuardada_soloMuestraPartidaNueva() {
        val viewModel = MenuViewModel(RepositorioPartidaEnMemoria(), motor())
        composeTestRule.setContent { MenuScreen(viewModel = viewModel, onContinuar = {}, onPartidaNueva = {}) }

        composeTestRule.onNodeWithText("Partida nueva").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Continuar").assertCountEquals(0)
    }

    @Test
    fun conPartidaGuardada_muestraContinuarYPartidaNueva() {
        val viewModel = MenuViewModel(RepositorioPartidaEnMemoria(estadoEnCurso()), motor())
        composeTestRule.setContent { MenuScreen(viewModel = viewModel, onContinuar = {}, onPartidaNueva = {}) }

        composeTestRule.onNodeWithText("Continuar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Partida nueva").assertIsDisplayed()
    }

    @Test
    fun continuar_llamaAlCallbackSinBorrarNada() {
        val repositorio = RepositorioPartidaEnMemoria(estadoEnCurso())
        val viewModel = MenuViewModel(repositorio, motor())
        var continuarLlamado = false
        composeTestRule.setContent {
            MenuScreen(viewModel = viewModel, onContinuar = { continuarLlamado = true }, onPartidaNueva = {})
        }

        composeTestRule.onNodeWithText("Continuar").performClick()

        assertTrue(continuarLlamado)
        assertEquals(0, repositorio.vecesBorrado)
    }

    @Test
    fun partidaNueva_sinPartidaGuardada_navegaDirectoSinConfirmar() {
        val repositorio = RepositorioPartidaEnMemoria()
        val viewModel = MenuViewModel(repositorio, motor())
        var partidaNuevaLlamado = false
        composeTestRule.setContent {
            MenuScreen(viewModel = viewModel, onContinuar = {}, onPartidaNueva = { partidaNuevaLlamado = true })
        }

        composeTestRule.onNodeWithText("Partida nueva").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { partidaNuevaLlamado }
        assertTrue(partidaNuevaLlamado)
    }

    @Test
    fun partidaNueva_conPartidaTerminada_navegaDirectoSinConfirmar() {
        val repositorio = RepositorioPartidaEnMemoria(estadoTerminado())
        val viewModel = MenuViewModel(repositorio, motor())
        var partidaNuevaLlamado = false
        composeTestRule.setContent {
            MenuScreen(viewModel = viewModel, onContinuar = {}, onPartidaNueva = { partidaNuevaLlamado = true })
        }

        composeTestRule.onNodeWithText("Partida nueva").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { partidaNuevaLlamado }
        assertEquals(1, repositorio.vecesBorrado)
    }

    @Test
    fun partidaNueva_conPartidaEnCurso_pideConfirmacionAntesDeBorrar() {
        val repositorio = RepositorioPartidaEnMemoria(estadoEnCurso())
        val viewModel = MenuViewModel(repositorio, motor())
        var partidaNuevaLlamado = false
        composeTestRule.setContent {
            MenuScreen(viewModel = viewModel, onContinuar = {}, onPartidaNueva = { partidaNuevaLlamado = true })
        }

        composeTestRule.onNodeWithText("Partida nueva").performClick()

        // Todavía no borró ni navegó: espera confirmación.
        assertFalse(partidaNuevaLlamado)
        assertEquals(0, repositorio.vecesBorrado)
        composeTestRule.onNodeWithText("Empezar de nuevo").assertIsDisplayed()

        composeTestRule.onNodeWithText("Empezar de nuevo").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { partidaNuevaLlamado }
        assertEquals(1, repositorio.vecesBorrado)
    }

    @Test
    fun partidaNueva_conPartidaEnCurso_cancelarNoBorraNiNavega() {
        val repositorio = RepositorioPartidaEnMemoria(estadoEnCurso())
        val viewModel = MenuViewModel(repositorio, motor())
        var partidaNuevaLlamado = false
        composeTestRule.setContent {
            MenuScreen(viewModel = viewModel, onContinuar = {}, onPartidaNueva = { partidaNuevaLlamado = true })
        }

        composeTestRule.onNodeWithText("Partida nueva").performClick()
        composeTestRule.onNodeWithText("Cancelar").performClick()

        assertFalse(partidaNuevaLlamado)
        assertEquals(0, repositorio.vecesBorrado)
        composeTestRule.onNodeWithText("Partida nueva").assertIsDisplayed()
    }

    // El ViewModel sobrevive mientras la Activity vive, aunque la pantalla salga y
    // vuelva a entrar en composición (botón atrás desde la partida). Sin refrescar en
    // cada entrada, "Continuar" se queda con la respuesta de la primera vez.
    @Test
    fun reingresarAlMenu_muestraContinuarSiSeGuardoUnaPartidaMientrasNoEstabaVisible() {
        val repositorio = RepositorioPartidaEnMemoria()
        val viewModel = MenuViewModel(repositorio, motor())
        var mostrarMenu by mutableStateOf(true)
        composeTestRule.setContent {
            if (mostrarMenu) {
                MenuScreen(viewModel = viewModel, onContinuar = {}, onPartidaNueva = {})
            }
        }
        composeTestRule.onAllNodesWithText("Continuar").assertCountEquals(0)

        // Simula jugar y guardar mientras el menú no está en composición.
        runBlocking { repositorio.guardar(estadoEnCurso()) }
        mostrarMenu = false
        composeTestRule.waitForIdle()
        mostrarMenu = true

        composeTestRule.onNodeWithText("Continuar").assertIsDisplayed()
    }
}
