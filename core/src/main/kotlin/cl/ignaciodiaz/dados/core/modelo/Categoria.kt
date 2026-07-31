package cl.ignaciodiaz.dados.core.modelo

// Una categoría de la tabla de puntajes. La función de validez decide si una tirada
// califica; la función de puntaje calcula cuánto vale. Ambas se fijan al construir
// el RuleSet: no hay banderas booleanas duplicadas que puedan contradecirlas (decisión 24).
//
// No es data class: la igualdad se basa únicamente en el id, porque los ids ya son
// únicos dentro de un RuleSet (ver RuleSet.init) y comparar lambdas por estructura
// no tiene sentido.
class Categoria(
    val id: CategoriaId,
    val seccion: Seccion,
    val esValida: (Tirada) -> Boolean,
    val puntaje: (Tirada) -> Int
) {
    override fun equals(other: Any?): Boolean =
        other is Categoria && id == other.id

    override fun hashCode(): Int = id.hashCode()
}
