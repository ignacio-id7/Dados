package cl.ignaciodiaz.dados.core.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RuleSetTest {

    private fun categoria(nombre: String) = Categoria(
        id = CategoriaId(nombre),
        seccion = Seccion.SUPERIOR,
        esValida = { true },
        puntaje = { 0 }
    )

    @Test
    fun `el numero de turnos se deriva de la cantidad de categorias`() {
        val ruleSet = RuleSet(
            categorias = listOf(categoria("unos"), categoria("doses"), categoria("treses")),
            bonusSeccionSuperior = null
        )

        assertEquals(3, ruleSet.numeroDeTurnos)
    }

    @Test
    fun `admite no tener bonus de seccion superior`() {
        val ruleSet = RuleSet(categorias = listOf(categoria("unos")), bonusSeccionSuperior = null)

        assertNull(ruleSet.bonusSeccionSuperior)
    }

    @Test
    fun `rechaza una lista de categorias vacia`() {
        assertThrows(IllegalArgumentException::class.java) {
            RuleSet(categorias = emptyList(), bonusSeccionSuperior = null)
        }
    }

    @Test
    fun `rechaza identificadores de categoria repetidos`() {
        assertThrows(IllegalArgumentException::class.java) {
            RuleSet(
                categorias = listOf(categoria("unos"), categoria("unos")),
                bonusSeccionSuperior = null
            )
        }
    }
}
