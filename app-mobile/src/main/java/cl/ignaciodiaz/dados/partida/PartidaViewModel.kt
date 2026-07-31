package cl.ignaciodiaz.dados.partida

import androidx.lifecycle.ViewModel
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.motor.ResultadoAccion
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

// Sostiene el EstadoPartida de una partida de un jugador y traduce toques en Accion.
// No decide legalidad, no calcula puntajes, no cuenta lanzamientos, no determina fin
// de partida: todo eso lo responde MotorPartida. Si se borra esta clase, no se pierde
// ninguna regla del juego.
//
// El motor se recibe por constructor con valor por defecto: Kotlin genera un
// constructor sin argumentos cuando todos los parámetros lo tienen, así que
// viewModel() lo sigue instanciando solo, y los tests pueden inyectar un motor con
// Random(semilla) para ser deterministas.
class PartidaViewModel(
    private val motor: MotorPartida = MotorPartida(presetClasico(), Random.Default)
) : ViewModel() {

    // Ids de categoría en el orden del RuleSet. No cambia durante la vida del
    // ViewModel: no hace falta que viaje dentro del StateFlow.
    val categorias: List<CategoriaId> = motor.categorias

    private val estadoInicial = EstadoPartida(
        jugadores = listOf(Jugador(nombre = "Jugador 1")),
        indiceTurno = 0,
        tiradaActual = null
    )

    private val _uiState = MutableStateFlow(construirUiState(estadoInicial))
    val uiState: StateFlow<PartidaUiState> = _uiState.asStateFlow()

    fun despachar(accion: Accion) {
        when (val resultado = motor.aplicar(_uiState.value.estado, accion)) {
            is ResultadoAccion.Exito -> _uiState.value = construirUiState(resultado.estado)
            is ResultadoAccion.Rechazada -> Unit
        }
    }

    private fun construirUiState(estado: EstadoPartida) = PartidaUiState(
        estado = estado,
        accionesLegales = motor.accionesLegales(estado),
        puntaje = motor.puntaje(estado.jugadorEnTurno),
        previsualizaciones = categorias.associateWith { id -> motor.puntajeSiSeAnotara(estado, id) }
    )
}
