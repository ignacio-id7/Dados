package cl.ignaciodiaz.dados.core.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoriaIdTest {

    @Test
    fun `dos identificadores con el mismo texto son iguales`() {
        assertEquals(CategoriaId("yacht"), CategoriaId("yacht"))
    }

    @Test
    fun `identificadores con texto distinto no son iguales`() {
        assertNotEquals(CategoriaId("yacht"), CategoriaId("full-house"))
    }
}
