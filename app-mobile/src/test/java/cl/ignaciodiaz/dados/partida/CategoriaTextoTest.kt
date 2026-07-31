package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriaTextoTest {

    @Test
    fun `traduce los 12 ids del preset Clasico a nombres en espanol`() {
        val esperados = mapOf(
            "ones" to "Unos",
            "twos" to "Doses",
            "threes" to "Treses",
            "fours" to "Cuatros",
            "fives" to "Cincos",
            "sixes" to "Seises",
            "choice" to "Choice",
            "four_dice" to "Four Dice",
            "full_house" to "Full House",
            "small_straight" to "Escalera chica",
            "big_straight" to "Escalera grande",
            "yacht" to "Yacht"
        )

        esperados.forEach { (id, nombre) ->
            assertEquals(nombre, nombreDeCategoria(CategoriaId(id)))
        }
    }

    @Test
    fun `un id desconocido devuelve el texto crudo en vez de lanzar`() {
        assertEquals("categoria_experimental", nombreDeCategoria(CategoriaId("categoria_experimental")))
    }
}
