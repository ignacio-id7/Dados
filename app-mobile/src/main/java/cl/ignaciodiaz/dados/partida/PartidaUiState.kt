package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.Puntaje

// Lo que la pantalla necesita para dibujarse: el estado de la partida, qué acciones
// el motor aceptaría ahora mismo, el desglose de puntaje del jugador en turno y la
// previsualización de puntaje de cada categoría libre. Todo pedido al motor:
// PartidaUiState no decide nada, solo empaqueta lo que devuelve.
data class PartidaUiState(
    val estado: EstadoPartida,
    val accionesLegales: List<Accion>,
    val puntaje: Puntaje,
    val previsualizaciones: Map<CategoriaId, Int?>
)
