package cl.ignaciodiaz.dados.core.modelo

// Los 5 dados de la partida y cuántos lanzamientos se han usado en el turno actual.
// El orden de la lista es significativo: el índice de un dado debe seguir apuntando
// al mismo dado entre lanzamientos, porque la interfaz lo referencia por posición.
data class Tirada(
    val dados: List<Dado>,
    val lanzamientos: Int
) {
    init {
        require(dados.size == CANTIDAD_DE_DADOS) {
            "Una tirada debe tener exactamente $CANTIDAD_DE_DADOS dados, pero tiene ${dados.size}"
        }
        require(lanzamientos in 0..MAXIMO_DE_LANZAMIENTOS) {
            "El número de lanzamientos debe estar entre 0 y $MAXIMO_DE_LANZAMIENTOS, pero fue $lanzamientos"
        }
    }

    // Verdadero si todavía queda al menos un lanzamiento disponible en el turno.
    val puedeRelanzar: Boolean
        get() = lanzamientos < MAXIMO_DE_LANZAMIENTOS

    companion object {
        const val CANTIDAD_DE_DADOS = 5
        const val MAXIMO_DE_LANZAMIENTOS = 3
    }
}
