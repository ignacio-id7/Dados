package cl.ignaciodiaz.dados.core.modelo

import kotlinx.serialization.Serializable

// Representa un dado físico: su valor (1 a 6) y si el jugador lo retuvo entre lanzamientos.
@Serializable
data class Dado(
    val valor: Int,
    val retenido: Boolean = false
) {
    init {
        require(valor in 1..6) { "El valor de un dado debe estar entre 1 y 6, pero fue $valor" }
    }
}
