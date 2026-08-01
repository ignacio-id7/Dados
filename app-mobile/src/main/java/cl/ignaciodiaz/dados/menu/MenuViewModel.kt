package cl.ignaciodiaz.dados.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import cl.ignaciodiaz.dados.persistencia.RepositorioPartida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

// Sostiene el estado de la pantalla de menú: si hay una partida guardada y si esa
// partida está sin terminar (decisión 46). No decide ninguna regla del juego, solo le
// pregunta al repositorio y, para saber si la partida guardada terminó, al motor.
class MenuViewModel(
    private val repositorioPartida: RepositorioPartida,
    private val motor: MotorPartida = MotorPartida(presetClasico(), Random.Default)
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MenuUiState(cargando = true, hayPartidaGuardada = false, partidaGuardadaSinTerminar = false)
    )
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        refrescar()
    }

    // Vuelve a consultar el repositorio. Necesario porque, a diferencia de
    // PartidaViewModel, este ViewModel puede seguir vivo cuando el jugador vuelve al
    // menú con el botón atrás después de jugar: sin refrescar, "Continuar" quedaría
    // con la respuesta de la primera vez que se armó el menú.
    fun refrescar() {
        viewModelScope.launch {
            val estadoGuardado = repositorioPartida.cargar()
            _uiState.value = MenuUiState(
                cargando = false,
                hayPartidaGuardada = estadoGuardado != null,
                partidaGuardadaSinTerminar = estadoGuardado != null && !motor.partidaTerminada(estadoGuardado)
            )
        }
    }

    // Botón "Partida nueva" del menú (decisión 46). Borra la partida guardada de
    // verdad, no solo el estado en memoria: si no, "Continuar" seguiría apareciendo.
    // Es suspend, no viewModelScope.launch, para que quien la llama (la pantalla)
    // pueda esperar a que termine antes de navegar a la partida.
    suspend fun partidaNueva() {
        repositorioPartida.borrar()
        _uiState.value = MenuUiState(cargando = false, hayPartidaGuardada = false, partidaGuardadaSinTerminar = false)
    }
}
