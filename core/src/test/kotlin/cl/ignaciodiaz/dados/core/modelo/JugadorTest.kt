package cl.ignaciodiaz.dados.core.modelo

import org.junit.Assert.assertEquals
import org.junit.Test

class JugadorTest {

    @Test
    fun `un jugador nuevo no tiene anotaciones`() {
        val jugador = Jugador(nombre = "Ignacio")

        assertEquals(0, jugador.sumaDeAnotaciones)
    }

    @Test
    fun `la suma de anotaciones suma las anotaciones`() {
        val jugador = Jugador(
            nombre = "Ignacio",
            anotaciones = mapOf(
                CategoriaId("unos") to 3,
                CategoriaId("yacht") to 50
            )
        )

        assertEquals(53, jugador.sumaDeAnotaciones)
    }
}
