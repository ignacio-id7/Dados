package cl.ignaciodiaz.dados.core.modelo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class EstadoPartidaTest {

    private val jugadores = listOf(Jugador("Jugador 1"), Jugador("Invitado"))

    @Test
    fun `el jugador en turno corresponde al indice de turno`() {
        val estado = EstadoPartida(jugadores = jugadores, indiceTurno = 1, tiradaActual = null)

        assertEquals(jugadores[1], estado.jugadorEnTurno)
    }

    @Test
    fun `la tirada actual puede estar ausente al empezar el turno`() {
        val estado = EstadoPartida(jugadores = jugadores, indiceTurno = 0, tiradaActual = null)

        assertNull(estado.tiradaActual)
    }

    @Test
    fun `rechaza una partida sin jugadores`() {
        assertThrows(IllegalArgumentException::class.java) {
            EstadoPartida(jugadores = emptyList(), indiceTurno = 0, tiradaActual = null)
        }
    }

    @Test
    fun `rechaza un indice de turno fuera de rango`() {
        assertThrows(IllegalArgumentException::class.java) {
            EstadoPartida(jugadores = jugadores, indiceTurno = 2, tiradaActual = null)
        }
    }

    @Test
    fun `rechaza un indice de turno negativo`() {
        assertThrows(IllegalArgumentException::class.java) {
            EstadoPartida(jugadores = jugadores, indiceTurno = -1, tiradaActual = null)
        }
    }
}
