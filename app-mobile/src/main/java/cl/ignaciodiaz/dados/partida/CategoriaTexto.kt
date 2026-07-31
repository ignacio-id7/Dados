package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.CategoriaId

// Nombres visibles de las 12 categorías del preset "Clásico". :core no conoce texto
// de interfaz (CLAUDE.md); este mapeo vive acá. CategoriaId es un value class sobre
// String, así que el when no puede ser exhaustivo para el compilador: el else
// devuelve el id crudo (sin traducir) en vez de lanzar. Si un preset futuro trae una
// categoría sin nombre mapeado, se ve fea en pantalla en vez de cerrar la app.
fun nombreDeCategoria(id: CategoriaId): String = when (id.valor) {
    "ones" -> "Unos"
    "twos" -> "Doses"
    "threes" -> "Treses"
    "fours" -> "Cuatros"
    "fives" -> "Cincos"
    "sixes" -> "Seises"
    "choice" -> "Choice"
    "four_dice" -> "Four Dice"
    "full_house" -> "Full House"
    "small_straight" -> "Escalera chica"
    "big_straight" -> "Escalera grande"
    "yacht" -> "Yacht"
    else -> id.valor
}
