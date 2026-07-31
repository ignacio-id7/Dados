package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.Dado
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.Tirada
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.motor.ResultadoAccion
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

private const val SEMILLA = 7

@OptIn(ExperimentalCoroutinesApi::class)
class PartidaViewModelTest {

    // viewModelScope necesita un Main dispatcher; Unconfined lo ejecuta de inmediato,
    // así que el repositorio falso (que nunca suspende de verdad) termina de cargar
    // antes de que el test siga leyendo uiState.value.
    @Before
    fun ponerDispatcherDePrueba() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restaurarDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sin partida guardada, el estado inicial trae un jugador, sin tirada, y la unica accion legal es lanzar`() {
        val viewModel = PartidaViewModel(RepositorioPartidaFalso(), MotorPartida(presetClasico(), Random(SEMILLA)))

        val uiState = viewModel.uiState.value

        assertFalse(uiState.cargando)
        assertEquals(1, uiState.estado.jugadores.size)
        assertNull(uiState.estado.tiradaActual)
        assertEquals(listOf(Accion.Lanzar), uiState.accionesLegales)
    }

    @Test
    fun `con partida guardada, el estado inicial es el que devuelve el repositorio, no una partida nueva`() {
        val estadoGuardado = EstadoPartida(
            jugadores = listOf(
                Jugador(nombre = "Jugador 1", anotaciones = mapOf(CategoriaId("ones") to 3))
            ),
            indiceTurno = 0,
            tiradaActual = Tirada(
                dados = List(5) { indice -> Dado(valor = 4, retenido = indice == 0) },
                lanzamientos = 1
            )
        )
        val repositorio = RepositorioPartidaFalso(estadoGuardado)

        val viewModel = PartidaViewModel(repositorio, MotorPartida(presetClasico(), Random(SEMILLA)))

        assertFalse(viewModel.uiState.value.cargando)
        assertEquals(estadoGuardado, viewModel.uiState.value.estado)
    }

    @Test
    fun `despachar Lanzar guarda exactamente el estado y las acciones legales que devuelve el motor`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val motorEspejo = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(RepositorioPartidaFalso(), motor)
        val estadoInicial = viewModel.uiState.value.estado

        viewModel.despachar(Accion.Lanzar)

        val esperado = motorEspejo.aplicar(estadoInicial, Accion.Lanzar)
        check(esperado is ResultadoAccion.Exito)
        assertEquals(esperado.estado, viewModel.uiState.value.estado)
        assertEquals(motorEspejo.accionesLegales(esperado.estado), viewModel.uiState.value.accionesLegales)
    }

    @Test
    fun `despachar una accion rechazada no modifica el estado`() {
        val viewModel = PartidaViewModel(RepositorioPartidaFalso(), MotorPartida(presetClasico(), Random(SEMILLA)))
        val estadoAntes = viewModel.uiState.value

        // AlternarRetencion es ilegal sin tirada en curso: el turno recien empieza.
        viewModel.despachar(Accion.AlternarRetencion(0))

        assertEquals(estadoAntes, viewModel.uiState.value)
    }

    @Test
    fun `el puntaje expuesto es el que devuelve motor puntaje para el jugador en turno`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(RepositorioPartidaFalso(), motor)

        val esperado = motor.puntaje(viewModel.uiState.value.estado.jugadorEnTurno)

        assertEquals(esperado, viewModel.uiState.value.puntaje)
    }

    @Test
    fun `las previsualizaciones vienen de motor puntajeSiSeAnotara para cada categoria`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(RepositorioPartidaFalso(), motor)
        val estado = viewModel.uiState.value.estado

        val esperadas = motor.categorias.associateWith { id -> motor.puntajeSiSeAnotara(estado, id) }

        assertEquals(esperadas, viewModel.uiState.value.previsualizaciones)
    }

    @Test
    fun `despachar una accion exitosa guarda el estado nuevo en el repositorio`() {
        val repositorio = RepositorioPartidaFalso()
        val viewModel = PartidaViewModel(repositorio, MotorPartida(presetClasico(), Random(SEMILLA)))

        viewModel.despachar(Accion.Lanzar)

        assertEquals(1, repositorio.estadosGuardados.size)
        assertEquals(viewModel.uiState.value.estado, repositorio.estadosGuardados.single())
    }

    @Test
    fun `despachar una accion rechazada no guarda nada`() {
        val repositorio = RepositorioPartidaFalso()
        val viewModel = PartidaViewModel(repositorio, MotorPartida(presetClasico(), Random(SEMILLA)))

        viewModel.despachar(Accion.AlternarRetencion(0))

        assertTrue(repositorio.estadosGuardados.isEmpty())
    }
}
