package cl.ignaciodiaz.dados.partida

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cl.ignaciodiaz.dados.core.modelo.Categoria
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.RuleSet
import cl.ignaciodiaz.dados.core.modelo.Seccion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import cl.ignaciodiaz.dados.persistencia.RepositorioPartida
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

// Cubre el progreso del bonus (decisión 43) y el panel de fin de partida (decisión 42):
// aparece sobre el tablero cuando el motor decide que la partida terminó, se puede
// cerrar y reabrir, y "Partida nueva" borra la partida guardada de verdad.
class PartidaScreenFinDePartidaTest {

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

    // RuleSet de una sola categoría: alcanza para terminar la partida sin anotar las
    // 12 categorías del preset Clásico.
    private fun ruleSetDeUnaCategoria() = RuleSet(
        categorias = listOf(
            Categoria(id = CategoriaId("unica"), seccion = Seccion.SUPERIOR, esValida = { true }, puntaje = { 5 })
        ),
        bonusSeccionSuperior = null
    )

    private fun estadoTerminado() = EstadoPartida(
        jugadores = listOf(Jugador(nombre = "Jugador 1", anotaciones = mapOf(CategoriaId("unica") to 5))),
        indiceTurno = 0,
        tiradaActual = null
    )

    @Test
    fun subtotalSuperiorMuestraElProgresoHaciaElUmbral() {
        val viewModel = PartidaViewModel(RepositorioPartidaEnMemoria(), MotorPartida(presetClasico(), Random(1)))
        composeTestRule.setContent { PartidaScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithText("Subtotal superior: 0/63").assertIsDisplayed()
    }

    @Test
    fun subtotalSuperiorSinUmbral_muestraSoloElSubtotal() {
        val viewModel = PartidaViewModel(RepositorioPartidaEnMemoria(), MotorPartida(ruleSetDeUnaCategoria(), Random(1)))
        composeTestRule.setContent { PartidaScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithText("Subtotal superior: 0").assertIsDisplayed()
    }

    @Test
    fun partidaTerminada_muestraElPanelConElPuntajeFinal() {
        val motor = MotorPartida(ruleSetDeUnaCategoria(), Random(1))
        val viewModel = PartidaViewModel(RepositorioPartidaEnMemoria(estadoTerminado()), motor)
        composeTestRule.setContent { PartidaScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithText("Partida terminada").assertIsDisplayed()
        // "Subtotal inferior" solo lo muestra el panel: ResumenPuntaje (siempre visible
        // detrás) no lo incluye, así que confirma que es el desglose del panel.
        composeTestRule.onNodeWithText("Subtotal inferior: 0").assertIsDisplayed()
    }

    @Test
    fun cerrarElPanel_loOcultaYPuedeReabrirse() {
        val motor = MotorPartida(ruleSetDeUnaCategoria(), Random(1))
        val viewModel = PartidaViewModel(RepositorioPartidaEnMemoria(estadoTerminado()), motor)
        composeTestRule.setContent { PartidaScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithText("Cerrar").performClick()
        composeTestRule.onNodeWithText("Partida terminada").assertDoesNotExist()

        composeTestRule.onNodeWithText("Ver resultado final").performClick()
        composeTestRule.onNodeWithText("Partida terminada").assertIsDisplayed()
    }

    @Test
    fun partidaNueva_borraElRepositorioYReiniciaElEstado() {
        val motor = MotorPartida(ruleSetDeUnaCategoria(), Random(1))
        val repositorio = RepositorioPartidaEnMemoria(estadoTerminado())
        val viewModel = PartidaViewModel(repositorio, motor)
        composeTestRule.setContent { PartidaScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithText("Partida nueva").performClick()

        composeTestRule.onNodeWithText("Partida terminada").assertDoesNotExist()
        composeTestRule.runOnIdle {
            assertEquals(1, repositorio.vecesBorrado)
        }
    }
}
