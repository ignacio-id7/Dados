package cl.ignaciodiaz.dados.partida

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.motor.ResultadoAccion
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import cl.ignaciodiaz.dados.haptica.RetroalimentacionHaptica
import cl.ignaciodiaz.dados.persistencia.RepositorioPartida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

// Sostiene el EstadoPartida de una partida de un jugador y traduce toques en Accion.
// No decide legalidad, no calcula puntajes, no cuenta lanzamientos, no determina fin
// de partida: todo eso lo responde MotorPartida. Si se borra esta clase, no se pierde
// ninguna regla del juego.
//
// El repositorio se recibe por constructor (decisión 41): al iniciar, carga la partida
// guardada y entra directo a ella sin preguntar (decisión 40); si no hay nada guardado
// o el JSON no se pudo leer, cargar() devuelve null y arranca una partida nueva. Guarda
// después de cada acción exitosa. El motor también se recibe por constructor, con valor
// por defecto, para que los tests puedan inyectar uno con Random(semilla) determinista.
//
// retroalimentacionHaptica tiene un valor por defecto sin efecto: la implementación de
// verdad (LocalHapticFeedback, en Compose) la inyecta la factory de PartidaScreen. Solo
// se dispara al lanzar con éxito (decisión 8): ni retener ni anotar vibran.
class PartidaViewModel(
    private val repositorioPartida: RepositorioPartida,
    private val motor: MotorPartida = MotorPartida(presetClasico(), Random.Default),
    private val retroalimentacionHaptica: RetroalimentacionHaptica = RetroalimentacionHaptica {}
) : ViewModel() {

    // Ids de categoría en el orden del RuleSet. No cambia durante la vida del
    // ViewModel: no hace falta que viaje dentro del StateFlow.
    val categorias: List<CategoriaId> = motor.categorias

    private val estadoNuevaPartida = EstadoPartida(
        jugadores = listOf(Jugador(nombre = "Jugador 1")),
        indiceTurno = 0,
        tiradaActual = null
    )

    private val _uiState = MutableStateFlow(construirUiState(estadoNuevaPartida, cargando = true))
    val uiState: StateFlow<PartidaUiState> = _uiState.asStateFlow()

    init {
        recargar()
    }

    // Vuelve a consultar el repositorio (equivalente a MenuViewModel.refrescar()).
    // PartidaScreen la invoca al entrar: como este ViewModel es una única instancia por
    // clase en el ViewModelStore de la Activity, sin esto reingresar a la partida
    // después de "Partida nueva" seguiría mostrando la partida anterior en memoria,
    // aunque el archivo ya esté borrado. Vuelve a mostrar el indicador de carga mientras
    // consulta, para no dibujar por un instante el tablero de la visita anterior.
    fun recargar() {
        _uiState.value = _uiState.value.copy(cargando = true)
        viewModelScope.launch {
            val estadoGuardado = repositorioPartida.cargar()
            _uiState.value = construirUiState(estadoGuardado ?: estadoNuevaPartida, cargando = false)
        }
    }

    fun despachar(accion: Accion) {
        when (val resultado = motor.aplicar(_uiState.value.estado, accion)) {
            is ResultadoAccion.Exito -> {
                _uiState.value = construirUiState(resultado.estado, cargando = false)
                viewModelScope.launch { repositorioPartida.guardar(resultado.estado) }
                if (accion is Accion.Lanzar) retroalimentacionHaptica.alLanzar()
            }
            is ResultadoAccion.Rechazada -> Unit
        }
    }

    // Botón "Partida nueva" del panel de fin de partida (decisión 42). Borra la partida
    // guardada, no solo el estado en memoria: si no, al reabrir la app volvería la
    // partida terminada.
    fun iniciarPartidaNueva() {
        viewModelScope.launch {
            repositorioPartida.borrar()
            _uiState.value = construirUiState(estadoNuevaPartida, cargando = false)
        }
    }

    private fun construirUiState(estado: EstadoPartida, cargando: Boolean) = PartidaUiState(
        estado = estado,
        accionesLegales = motor.accionesLegales(estado),
        puntaje = motor.puntaje(estado.jugadorEnTurno),
        previsualizaciones = categorias.associateWith { id -> motor.puntajeSiSeAnotara(estado, id) },
        cargando = cargando,
        partidaTerminada = motor.partidaTerminada(estado)
    )
}
