package cl.ignaciodiaz.dados.core.motor

import cl.ignaciodiaz.dados.core.modelo.CategoriaId

// Las tres acciones que un jugador puede intentar en su turno. MotorPartida decide
// si son legales contra el estado recibido; el estado nunca vive dentro del motor
// (decisión 29).
sealed interface Accion {

    // Con tiradaActual == null es el primer tiro del turno: 5 dados nuevos, ninguno
    // retenido. Con una tirada en curso, relanza solo los dados no retenidos.
    data object Lanzar : Accion

    // Invierte la retención del dado en esa posición. El índice apunta siempre al
    // mismo dado porque Tirada conserva el orden.
    data class AlternarRetencion(val indice: Int) : Accion

    // Anota en la categoría indicada. Anotar en una categoría libre siempre es legal:
    // si la tirada no califica, anota 0 (sacrificio). esValida no decide legalidad.
    data class Anotar(val categoriaId: CategoriaId) : Accion
}
