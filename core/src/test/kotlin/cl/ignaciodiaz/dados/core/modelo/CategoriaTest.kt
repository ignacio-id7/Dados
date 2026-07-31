package cl.ignaciodiaz.dados.core.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriaTest {

    @Test
    fun `expone su seccion y aplica sus propias funciones de validez y puntaje`() {
        val tirada = Tirada(List(5) { Dado(4) }, 1)
        val categoria = Categoria(
            id = CategoriaId("cuatros"),
            seccion = Seccion.SUPERIOR,
            esValida = { true },
            puntaje = { t -> t.dados.count { it.valor == 4 } * 4 }
        )

        assertEquals(Seccion.SUPERIOR, categoria.seccion)
        assertTrue(categoria.esValida(tirada))
        assertEquals(20, categoria.puntaje(tirada))
    }

    @Test
    fun `dos categorias con el mismo id son iguales aunque sus funciones sean instancias distintas`() {
        val categoriaA = Categoria(
            id = CategoriaId("yacht"),
            seccion = Seccion.INFERIOR,
            esValida = { true },
            puntaje = { 50 }
        )
        val categoriaB = Categoria(
            id = CategoriaId("yacht"),
            seccion = Seccion.INFERIOR,
            esValida = { false },
            puntaje = { 0 }
        )

        assertEquals(categoriaA, categoriaB)
        assertEquals(categoriaA.hashCode(), categoriaB.hashCode())
    }
}
