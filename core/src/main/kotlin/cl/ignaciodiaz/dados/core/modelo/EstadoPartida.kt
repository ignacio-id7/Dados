package cl.ignaciodiaz.dados.core.modelo

import kotlinx.serialization.Serializable

// El estado completo de una partida: es lo único que se persiste (decisión 22). Modela una
// lista de jugadores desde el primer commit aunque el MVP use uno solo (decisión 19).
// "tiradaActual" es null cuando el turno todavía no lanzó los dados.
@Serializable
data class EstadoPartida(
    val jugadores: List<Jugador>,
    val indiceTurno: Int,
    val tiradaActual: Tirada?
) {
    init {
        require(jugadores.isNotEmpty()) { "Una partida necesita al menos un jugador" }
        require(indiceTurno in jugadores.indices) {
            "El índice de turno $indiceTurno está fuera de rango para ${jugadores.size} jugador(es)"
        }
    }

    // El jugador al que le corresponde jugar en este momento.
    val jugadorEnTurno: Jugador
        get() = jugadores[indiceTurno]
}
