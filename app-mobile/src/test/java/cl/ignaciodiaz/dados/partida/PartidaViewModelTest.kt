package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.motor.ResultadoAccion
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

private const val SEMILLA = 7

class PartidaViewModelTest {

    @Test
    fun `el estado inicial trae un jugador, sin tirada, y la unica accion legal es lanzar`() {
        val viewModel = PartidaViewModel(MotorPartida(presetClasico(), Random(SEMILLA)))

        val uiState = viewModel.uiState.value

        assertEquals(1, uiState.estado.jugadores.size)
        assertNull(uiState.estado.tiradaActual)
        assertEquals(listOf(Accion.Lanzar), uiState.accionesLegales)
    }

    @Test
    fun `despachar Lanzar guarda exactamente el estado y las acciones legales que devuelve el motor`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val motorEspejo = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(motor)
        val estadoInicial = viewModel.uiState.value.estado

        viewModel.despachar(Accion.Lanzar)

        val esperado = motorEspejo.aplicar(estadoInicial, Accion.Lanzar)
        check(esperado is ResultadoAccion.Exito)
        assertEquals(esperado.estado, viewModel.uiState.value.estado)
        assertEquals(motorEspejo.accionesLegales(esperado.estado), viewModel.uiState.value.accionesLegales)
    }

    @Test
    fun `despachar una accion rechazada no modifica el estado`() {
        val viewModel = PartidaViewModel(MotorPartida(presetClasico(), Random(SEMILLA)))
        val estadoAntes = viewModel.uiState.value

        // AlternarRetencion es ilegal sin tirada en curso: el turno recien empieza.
        viewModel.despachar(Accion.AlternarRetencion(0))

        assertEquals(estadoAntes, viewModel.uiState.value)
    }

    @Test
    fun `el puntaje expuesto es el que devuelve motor puntaje para el jugador en turno`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(motor)

        val esperado = motor.puntaje(viewModel.uiState.value.estado.jugadorEnTurno)

        assertEquals(esperado, viewModel.uiState.value.puntaje)
    }

    @Test
    fun `las previsualizaciones vienen de motor puntajeSiSeAnotara para cada categoria`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(motor)
        val estado = viewModel.uiState.value.estado

        val esperadas = motor.categorias.associateWith { id -> motor.puntajeSiSeAnotara(estado, id) }

        assertEquals(esperadas, viewModel.uiState.value.previsualizaciones)
    }
}
