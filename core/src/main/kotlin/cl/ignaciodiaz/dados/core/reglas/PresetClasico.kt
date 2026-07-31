package cl.ignaciodiaz.dados.core.reglas

import cl.ignaciodiaz.dados.core.modelo.BonusSeccionSuperior
import cl.ignaciodiaz.dados.core.modelo.Categoria
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.RuleSet
import cl.ignaciodiaz.dados.core.modelo.Seccion
import cl.ignaciodiaz.dados.core.modelo.Tirada

// Construye el RuleSet del preset "Clásico": 12 categorías y bonus de sección superior
// 63 -> +35. Reglamento y aclaraciones de validez cerrados en 01-especificacion-juego.md
// (decisiones 7 y 7a). Único preset que expone el MVP.
fun presetClasico(): RuleSet {
    val sumaDeLosCincoDados: (Tirada) -> Int = { tirada -> tirada.dados.sumOf { it.valor } }

    fun cuentasPorValor(tirada: Tirada): Collection<Int> =
        tirada.dados.groupingBy { it.valor }.eachCount().values

    fun categoriaSuperior(id: String, valor: Int) = Categoria(
        id = CategoriaId(id),
        seccion = Seccion.SUPERIOR,
        esValida = { true },
        puntaje = { tirada -> tirada.dados.filter { it.valor == valor }.sumOf { it.valor } }
    )

    val categoriasSuperior = listOf(
        categoriaSuperior("ones", 1),
        categoriaSuperior("twos", 2),
        categoriaSuperior("threes", 3),
        categoriaSuperior("fours", 4),
        categoriaSuperior("fives", 5),
        categoriaSuperior("sixes", 6)
    )

    val choice = Categoria(
        id = CategoriaId("choice"),
        seccion = Seccion.INFERIOR,
        esValida = { true },
        puntaje = sumaDeLosCincoDados
    )

    // Un Yacht (5 iguales) contiene 4 iguales: cuenta como Four Dice válido.
    val fourDice = Categoria(
        id = CategoriaId("four_dice"),
        seccion = Seccion.INFERIOR,
        esValida = { tirada -> cuentasPorValor(tirada).any { it >= 4 } },
        puntaje = sumaDeLosCincoDados
    )

    // Un Yacht contiene 3+2: cuenta como Full House válido. 4+1 no califica: el grupo
    // de 4 no reemplaza al de 2 que exige la combinación.
    val fullHouse = Categoria(
        id = CategoriaId("full_house"),
        seccion = Seccion.INFERIOR,
        esValida = { tirada ->
            val grupos = cuentasPorValor(tirada).sorted()
            grupos == listOf(2, 3) || grupos == listOf(5)
        },
        puntaje = sumaDeLosCincoDados
    )

    // Admite cualquiera de las tres subsecuencias de 4 consecutivos contenida en los
    // 5 dados; no exige que los 5 dados formen exactamente la secuencia.
    val smallStraight = Categoria(
        id = CategoriaId("small_straight"),
        seccion = Seccion.INFERIOR,
        esValida = { tirada ->
            val valores = tirada.dados.map { it.valor }.toSet()
            listOf(1..4, 2..5, 3..6).any { secuencia -> valores.containsAll(secuencia.toList()) }
        },
        puntaje = { 15 }
    )

    // Exige que los 5 dados sean consecutivos (5 valores distintos con rango 4):
    // 1-2-3-4-5 o 2-3-4-5-6. Por construcción, todo Big Straight es Small Straight válido.
    val bigStraight = Categoria(
        id = CategoriaId("big_straight"),
        seccion = Seccion.INFERIOR,
        esValida = { tirada ->
            val valores = tirada.dados.map { it.valor }.toSet()
            valores.size == 5 && (valores.max() - valores.min()) == 4
        },
        puntaje = { 30 }
    )

    val yacht = Categoria(
        id = CategoriaId("yacht"),
        seccion = Seccion.INFERIOR,
        esValida = { tirada -> tirada.dados.map { it.valor }.toSet().size == 1 },
        puntaje = { 50 }
    )

    return RuleSet(
        categorias = categoriasSuperior + listOf(choice, fourDice, fullHouse, smallStraight, bigStraight, yacht),
        bonusSeccionSuperior = BonusSeccionSuperior(umbral = 63, valor = 35)
    )
}
