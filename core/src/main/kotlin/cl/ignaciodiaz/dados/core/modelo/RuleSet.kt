package cl.ignaciodiaz.dados.core.modelo

// Bonus de la sección superior: si la suma de esa sección alcanza el umbral, se suma "valor".
// Un RuleSet que no quiera bonus simplemente no lo declara (null).
data class BonusSeccionSuperior(
    val umbral: Int,
    val valor: Int
)

// El conjunto de reglas de una partida: qué categorías existen, cómo se puntúan y si hay
// bonus de sección superior. Es código, no datos: contiene funciones y se construye en
// memoria al arrancar; nunca se persiste (decisión 22).
data class RuleSet(
    val categorias: List<Categoria>,
    val bonusSeccionSuperior: BonusSeccionSuperior?
) {
    init {
        require(categorias.isNotEmpty()) { "Un RuleSet necesita al menos una categoría" }
        require(categorias.map { it.id }.toSet().size == categorias.size) {
            "Los identificadores de las categorías de un RuleSet deben ser únicos"
        }
    }

    // El número de turnos de una partida es el número de categorías: una anotación por turno.
    // Se deriva de la lista, nunca es una constante fija.
    val numeroDeTurnos: Int
        get() = categorias.size
}
