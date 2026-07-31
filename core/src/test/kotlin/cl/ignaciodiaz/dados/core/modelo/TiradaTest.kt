package cl.ignaciodiaz.dados.core.modelo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TiradaTest {

    private fun cincoDados(valor: Int = 1) = List(5) { Dado(valor) }

    @Test
    fun `acepta lanzamientos en el rango 0 a 3`() {
        for (lanzamientos in 0..3) {
            Tirada(cincoDados(), lanzamientos)
        }
    }

    @Test
    fun `rechaza un numero de lanzamientos negativo`() {
        assertThrows(IllegalArgumentException::class.java) { Tirada(cincoDados(), -1) }
    }

    @Test
    fun `rechaza mas de 3 lanzamientos`() {
        assertThrows(IllegalArgumentException::class.java) { Tirada(cincoDados(), 4) }
    }

    @Test
    fun `rechaza una tirada con menos de 5 dados`() {
        assertThrows(IllegalArgumentException::class.java) { Tirada(List(4) { Dado(1) }, 0) }
    }

    @Test
    fun `rechaza una tirada con mas de 5 dados`() {
        assertThrows(IllegalArgumentException::class.java) { Tirada(List(6) { Dado(1) }, 0) }
    }

    @Test
    fun `puede relanzar si quedan lanzamientos disponibles`() {
        assertTrue(Tirada(cincoDados(), 0).puedeRelanzar)
        assertTrue(Tirada(cincoDados(), 2).puedeRelanzar)
    }

    @Test
    fun `no puede relanzar al agotar los 3 lanzamientos`() {
        assertFalse(Tirada(cincoDados(), 3).puedeRelanzar)
    }
}
