package cl.ignaciodiaz.dados.menu

import cl.ignaciodiaz.dados.core.modelo.Categoria
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.RuleSet
import cl.ignaciodiaz.dados.core.modelo.Seccion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import cl.ignaciodiaz.dados.partida.RepositorioPartidaFalso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

private const val SEMILLA = 11

// RuleSet de una sola categoría: alcanza para tener una partida "terminada" sin
// anotar las 12 categorías del preset Clásico.
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

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    @Before
    fun ponerDispatcherDePrueba() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurarDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sin partida guardada, no hay partida guardada y no pide confirmacion`() {
        val viewModel = MenuViewModel(RepositorioPartidaFalso(), MotorPartida(ruleSetDeUnaCategoria(), Random(SEMILLA)))

        val uiState = viewModel.uiState.value

        assertFalse(uiState.cargando)
        assertFalse(uiState.hayPartidaGuardada)
        assertFalse(uiState.partidaGuardadaSinTerminar)
    }

    @Test
    fun `con partida guardada en curso, hay partida guardada y pide confirmacion`() {
        val repositorio = RepositorioPartidaFalso(estadoEnCurso())
        val viewModel = MenuViewModel(repositorio, MotorPartida(ruleSetDeUnaCategoria(), Random(SEMILLA)))

        val uiState = viewModel.uiState.value

        assertFalse(uiState.cargando)
        assertTrue(uiState.hayPartidaGuardada)
        assertTrue(uiState.partidaGuardadaSinTerminar)
    }

    @Test
    fun `con partida guardada terminada, hay partida guardada pero no pide confirmacion`() {
        val repositorio = RepositorioPartidaFalso(estadoTerminado())
        val viewModel = MenuViewModel(repositorio, MotorPartida(ruleSetDeUnaCategoria(), Random(SEMILLA)))

        val uiState = viewModel.uiState.value

        assertFalse(uiState.cargando)
        assertTrue(uiState.hayPartidaGuardada)
        assertFalse(uiState.partidaGuardadaSinTerminar)
    }

    @Test
    fun `partidaNueva borra el repositorio y deja un estado limpio`() = runTest {
        val repositorio = RepositorioPartidaFalso(estadoEnCurso())
        val viewModel = MenuViewModel(repositorio, MotorPartida(ruleSetDeUnaCategoria(), Random(SEMILLA)))
        check(viewModel.uiState.value.hayPartidaGuardada)

        viewModel.partidaNueva()

        assertEquals(1, repositorio.vecesBorrado)
        assertNull(repositorio.estadoActual)

        val uiState = viewModel.uiState.value
        assertFalse(uiState.cargando)
        assertFalse(uiState.hayPartidaGuardada)
        assertFalse(uiState.partidaGuardadaSinTerminar)
    }

    @Test
    fun `refrescar vuelve a consultar el repositorio`() {
        val repositorio = RepositorioPartidaFalso()
        val viewModel = MenuViewModel(repositorio, MotorPartida(presetClasico(), Random(SEMILLA)))
        check(!viewModel.uiState.value.hayPartidaGuardada)

        // Simula que, mientras el menú ya estaba armado, se jugó y guardó una partida
        // (por ejemplo, al volver de la pantalla de partida con el botón atrás).
        kotlinx.coroutines.runBlocking { repositorio.guardar(estadoEnCurso()) }
        viewModel.refrescar()

        assertTrue(viewModel.uiState.value.hayPartidaGuardada)
    }
}
