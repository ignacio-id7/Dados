package cl.ignaciodiaz.dados.core.motor

import cl.ignaciodiaz.dados.core.modelo.EstadoPartida

// El resultado de aplicar una acción. Las jugadas ilegales se reportan así, nunca
// con excepciones (decisión 30).
sealed interface ResultadoAccion {
    data class Exito(val estado: EstadoPartida) : ResultadoAccion
    data class Rechazada(val motivo: MotivoRechazo) : ResultadoAccion
}
