package cl.ignaciodiaz.dados.core.reglas

import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.Dado
import cl.ignaciodiaz.dados.core.modelo.RuleSet
import cl.ignaciodiaz.dados.core.modelo.Seccion
import cl.ignaciodiaz.dados.core.modelo.Tirada
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetClasicoTest {

    private fun tirada(vararg valores: Int) = Tirada(valores.map { Dado(it) }, lanzamientos = 1)

    private fun RuleSet.categoria(id: String) = categorias.first { it.id == CategoriaId(id) }

    // --- Estructura del preset ---

    @Test
    fun `tiene las 12 categorias con los ids exactos`() {
        val idsEsperados = listOf(
            "ones", "twos", "threes", "fours", "fives", "sixes",
            "choice", "four_dice", "full_house", "small_straight", "big_straight", "yacht"
        )

        assertEquals(idsEsperados, presetClasico().categorias.map { it.id.valor })
    }

    @Test
    fun `el numero de turnos se deriva de las 12 categorias`() {
        assertEquals(12, presetClasico().numeroDeTurnos)
    }

    @Test
    fun `las seis primeras categorias son de seccion superior y el resto de seccion inferior`() {
        val ruleSet = presetClasico()
        val superiores = listOf("ones", "twos", "threes", "fours", "fives", "sixes")
        val inferiores = listOf("choice", "four_dice", "full_house", "small_straight", "big_straight", "yacht")

        superiores.forEach { id -> assertEquals(Seccion.SUPERIOR, ruleSet.categoria(id).seccion) }
        inferiores.forEach { id -> assertEquals(Seccion.INFERIOR, ruleSet.categoria(id).seccion) }
    }

    // --- Bonus de sección superior: 63 activa, 62 no ---

    @Test
    fun `el bonus de seccion superior es 63 para activar y suma 35`() {
        val bonus = presetClasico().bonusSeccionSuperior!!

        assertEquals(63, bonus.umbral)
        assertEquals(35, bonus.valor)
    }

    // --- Sección superior: solo suma los dados de su propio valor ---

    @Test
    fun `cada categoria de la seccion superior suma solo los dados de su valor`() {
        val ruleSet = presetClasico()
        val t = tirada(1, 1, 2, 3, 6)

        assertEquals(2, ruleSet.categoria("ones").puntaje(t))
        assertEquals(2, ruleSet.categoria("twos").puntaje(t))
        assertEquals(3, ruleSet.categoria("threes").puntaje(t))
        assertEquals(0, ruleSet.categoria("fours").puntaje(t))
        assertEquals(0, ruleSet.categoria("fives").puntaje(t))
        assertEquals(6, ruleSet.categoria("sixes").puntaje(t))
    }

    @Test
    fun `las categorias de seccion superior son validas para cualquier tirada`() {
        val ruleSet = presetClasico()
        val t = tirada(1, 2, 3, 4, 5)

        listOf("ones", "twos", "threes", "fours", "fives", "sixes").forEach { id ->
            assertTrue(ruleSet.categoria(id).esValida(t))
        }
    }

    // --- Choice ---

    @Test
    fun `choice es valida para cualquier tirada y vale la suma de los 5 dados`() {
        val ruleSet = presetClasico()
        val t = tirada(1, 2, 3, 4, 6)

        assertTrue(ruleSet.categoria("choice").esValida(t))
        assertEquals(16, ruleSet.categoria("choice").puntaje(t))
    }

    @Test
    fun `choice, four dice y full house comparten la formula de suma cuando la tirada califica para las tres`() {
        val ruleSet = presetClasico()
        val yacht = tirada(6, 6, 6, 6, 6)

        assertEquals(30, ruleSet.categoria("choice").puntaje(yacht))
        assertEquals(30, ruleSet.categoria("four_dice").puntaje(yacht))
        assertEquals(30, ruleSet.categoria("full_house").puntaje(yacht))
    }

    // --- Four Dice ---

    @Test
    fun `four dice es valida con al menos 4 dados iguales`() {
        val ruleSet = presetClasico()
        val t = tirada(3, 3, 3, 3, 6)

        assertTrue(ruleSet.categoria("four_dice").esValida(t))
        assertEquals(18, ruleSet.categoria("four_dice").puntaje(t))
    }

    @Test
    fun `un yacht es un four dice valido`() {
        val ruleSet = presetClasico()

        assertTrue(ruleSet.categoria("four_dice").esValida(tirada(2, 2, 2, 2, 2)))
    }

    @Test
    fun `four dice no es valida sin 4 dados iguales`() {
        val ruleSet = presetClasico()

        assertFalse(ruleSet.categoria("four_dice").esValida(tirada(1, 2, 3, 4, 5)))
    }

    // --- Full House ---

    @Test
    fun `full house es valida con 3 iguales y 2 iguales`() {
        val ruleSet = presetClasico()
        val t = tirada(2, 2, 2, 5, 5)

        assertTrue(ruleSet.categoria("full_house").esValida(t))
        assertEquals(16, ruleSet.categoria("full_house").puntaje(t))
    }

    @Test
    fun `un yacht es un full house valido`() {
        val ruleSet = presetClasico()

        assertTrue(ruleSet.categoria("full_house").esValida(tirada(4, 4, 4, 4, 4)))
    }

    @Test
    fun `full house no es valida con 4 mas 1`() {
        val ruleSet = presetClasico()

        assertFalse(ruleSet.categoria("full_house").esValida(tirada(3, 3, 3, 3, 5)))
    }

    @Test
    fun `full house no es valida sin ningun grupo de a 3`() {
        val ruleSet = presetClasico()

        assertFalse(ruleSet.categoria("full_house").esValida(tirada(1, 2, 3, 4, 5)))
    }

    // --- Small Straight ---

    @Test
    fun `small straight acepta 1-2-3-4 entre los 5 dados`() {
        val ruleSet = presetClasico()
        val t = tirada(1, 2, 3, 4, 6)

        assertTrue(ruleSet.categoria("small_straight").esValida(t))
        assertEquals(15, ruleSet.categoria("small_straight").puntaje(t))
    }

    @Test
    fun `small straight acepta 2-3-4-5 entre los 5 dados`() {
        val ruleSet = presetClasico()

        assertTrue(ruleSet.categoria("small_straight").esValida(tirada(2, 3, 4, 5, 1)))
    }

    @Test
    fun `small straight acepta 3-4-5-6 entre los 5 dados`() {
        val ruleSet = presetClasico()

        assertTrue(ruleSet.categoria("small_straight").esValida(tirada(3, 4, 5, 6, 1)))
    }

    @Test
    fun `un big straight es un small straight valido`() {
        val ruleSet = presetClasico()

        assertTrue(ruleSet.categoria("small_straight").esValida(tirada(1, 2, 3, 4, 5)))
        assertTrue(ruleSet.categoria("small_straight").esValida(tirada(2, 3, 4, 5, 6)))
    }

    @Test
    fun `small straight no es valida sin 4 consecutivos`() {
        val ruleSet = presetClasico()

        assertFalse(ruleSet.categoria("small_straight").esValida(tirada(1, 1, 2, 2, 3)))
    }

    @Test
    fun `small straight es valida aunque un dado este repetido`() {
        val ruleSet = presetClasico()

        assertTrue(ruleSet.categoria("small_straight").esValida(tirada(1, 2, 3, 4, 4)))
    }

    // --- Big Straight ---

    @Test
    fun `big straight acepta 1-2-3-4-5 y 2-3-4-5-6`() {
        val ruleSet = presetClasico()

        assertTrue(ruleSet.categoria("big_straight").esValida(tirada(1, 2, 3, 4, 5)))
        assertTrue(ruleSet.categoria("big_straight").esValida(tirada(2, 3, 4, 5, 6)))
        assertEquals(30, ruleSet.categoria("big_straight").puntaje(tirada(1, 2, 3, 4, 5)))
    }

    @Test
    fun `big straight exige los 5 consecutivos, no solo 4 de los 5`() {
        val ruleSet = presetClasico()

        assertFalse(ruleSet.categoria("big_straight").esValida(tirada(1, 2, 3, 4, 4)))
    }

    // --- Yacht ---

    @Test
    fun `yacht es valida con los 5 dados iguales`() {
        val ruleSet = presetClasico()
        val t = tirada(5, 5, 5, 5, 5)

        assertTrue(ruleSet.categoria("yacht").esValida(t))
        assertEquals(50, ruleSet.categoria("yacht").puntaje(t))
    }

    @Test
    fun `yacht no es valida si un dado difiere`() {
        val ruleSet = presetClasico()

        assertFalse(ruleSet.categoria("yacht").esValida(tirada(5, 5, 5, 5, 6)))
    }
}
