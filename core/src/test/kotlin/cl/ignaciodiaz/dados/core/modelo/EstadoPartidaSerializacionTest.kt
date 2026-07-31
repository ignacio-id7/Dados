package cl.ignaciodiaz.dados.core.modelo

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

// El RuleSet nunca se serializa (decisión 22): estos tests solo cubren lo que
// EstadoPartida guarda, que son datos planos.
class EstadoPartidaSerializacionTest {

    @Test
    fun `una partida recien empezada sobrevive un viaje de ida y vuelta por JSON`() {
        val estado = EstadoPartida(
            jugadores = listOf(Jugador(nombre = "Jugador 1")),
            indiceTurno = 0,
            tiradaActual = null
        )

        val textoJson = Json.encodeToString(EstadoPartida.serializer(), estado)
        val restaurado = Json.decodeFromString(EstadoPartida.serializer(), textoJson)

        assertEquals(estado, restaurado)
    }

    @Test
    fun `una partida a medio turno con dados retenidos y anotaciones sobrevive el viaje de ida y vuelta`() {
        val estado = EstadoPartida(
            jugadores = listOf(
                Jugador(
                    nombre = "Jugador 1",
                    anotaciones = mapOf(CategoriaId("ones") to 3, CategoriaId("twos") to 6)
                ),
                Jugador(nombre = "Invitado")
            ),
            indiceTurno = 1,
            tiradaActual = Tirada(
                dados = listOf(
                    Dado(valor = 6, retenido = true),
                    Dado(valor = 6, retenido = true),
                    Dado(valor = 3, retenido = false),
                    Dado(valor = 1, retenido = false),
                    Dado(valor = 5, retenido = true)
                ),
                lanzamientos = 2
            )
        )

        val textoJson = Json.encodeToString(EstadoPartida.serializer(), estado)
        val restaurado = Json.decodeFromString(EstadoPartida.serializer(), textoJson)

        assertEquals(estado, restaurado)
    }
}
