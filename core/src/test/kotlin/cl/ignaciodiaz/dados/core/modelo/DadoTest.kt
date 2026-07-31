package cl.ignaciodiaz.dados.core.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DadoTest {

    @Test
    fun `acepta valores en el rango 1 a 6`() {
        for (valor in 1..6) {
            assertEquals(valor, Dado(valor).valor)
        }
    }

    @Test
    fun `no esta retenido por defecto`() {
        assertFalse(Dado(3).retenido)
    }

    @Test
    fun `rechaza un valor menor a 1`() {
        assertThrows(IllegalArgumentException::class.java) { Dado(0) }
    }

    @Test
    fun `rechaza un valor mayor a 6`() {
        assertThrows(IllegalArgumentException::class.java) { Dado(7) }
    }
}
