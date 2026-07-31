package cl.ignaciodiaz.dados.core.modelo

// Un jugador de la partida: su nombre y las categorías que ya anotó, con el puntaje
// obtenido en cada una.
data class Jugador(
    val nombre: String,
    val anotaciones: Map<CategoriaId, Int> = emptyMap()
) {
    // No es el puntaje final de la partida: le falta el bonus de sección superior,
    // que depende del RuleSet y por lo tanto lo calcula el motor, no el jugador.
    val sumaDeAnotaciones: Int
        get() = anotaciones.values.sum()
}
