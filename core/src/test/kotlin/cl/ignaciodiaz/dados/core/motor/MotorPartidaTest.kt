package cl.ignaciodiaz.dados.core.motor

import cl.ignaciodiaz.dados.core.modelo.Categoria
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.Dado
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.RuleSet
import cl.ignaciodiaz.dados.core.modelo.Seccion
import cl.ignaciodiaz.dados.core.modelo.Tirada
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private const val SEMILLA = 42

// RuleSet mínimo para tests de motor que no dependen de las fórmulas del preset
// "Clásico": categorías triviales, siempre válidas, que puntúan 0.
private fun ruleSetDePrueba(numeroDeCategorias: Int) = RuleSet(
    categorias = (1..numeroDeCategorias).map { i ->
        Categoria(
            id = CategoriaId("categoria_$i"),
            seccion = Seccion.SUPERIOR,
            esValida = { true },
            puntaje = { 0 }
        )
    },
    bonusSeccionSuperior = null
)

private fun estadoInicial(numeroDeJugadores: Int) = EstadoPartida(
    jugadores = List(numeroDeJugadores) { Jugador("Jugador ${it + 1}") },
    indiceTurno = 0,
    tiradaActual = null
)

private fun tiradaDePrueba() = Tirada(List(5) { Dado(1) }, lanzamientos = 1)

class MotorPartidaTest {

    // --- Lanzar ---

    @Test
    fun `lanzar con tiradaActual null crea 5 dados sin retener y deja lanzamientos en 1`() {
        val motor = MotorPartida(ruleSetDePrueba(1), Random(SEMILLA))
        val estado = estadoInicial(1)

        val resultado = motor.aplicar(estado, Accion.Lanzar) as ResultadoAccion.Exito
        val tirada = resultado.estado.tiradaActual!!

        assertEquals(5, tirada.dados.size)
        assertEquals(1, tirada.lanzamientos)
        assertTrue(tirada.dados.none { it.retenido })
    }

    @Test
    fun `lanzar relanza solo los dados no retenidos y conserva el valor de los retenidos`() {
        val motor = MotorPartida(ruleSetDePrueba(1), Random(SEMILLA))

        // Réplica independiente de la misma semilla, para calcular a mano los valores
        // que el motor debería producir sin asumir nada del algoritmo interno de Random.
        val generador = Random(SEMILLA)
        val primerosValores = List(5) { generador.nextInt(1, 7) }

        var estado = (motor.aplicar(estadoInicial(1), Accion.Lanzar) as ResultadoAccion.Exito).estado
        estado = (motor.aplicar(estado, Accion.AlternarRetencion(1)) as ResultadoAccion.Exito).estado
        estado = (motor.aplicar(estado, Accion.AlternarRetencion(3)) as ResultadoAccion.Exito).estado

        val segundosValores = primerosValores.mapIndexed { indice, valor ->
            if (indice == 1 || indice == 3) valor else generador.nextInt(1, 7)
        }

        estado = (motor.aplicar(estado, Accion.Lanzar) as ResultadoAccion.Exito).estado
        val tiradaFinal = estado.tiradaActual!!

        assertEquals(segundosValores, tiradaFinal.dados.map { it.valor })
        assertTrue(tiradaFinal.dados[1].retenido)
        assertTrue(tiradaFinal.dados[3].retenido)
        assertFalse(tiradaFinal.dados[0].retenido)
        assertFalse(tiradaFinal.dados[2].retenido)
        assertFalse(tiradaFinal.dados[4].retenido)
    }

    @Test
    fun `el cuarto lanzar es rechazado`() {
        val motor = MotorPartida(ruleSetDePrueba(1), Random(SEMILLA))
        var estado = estadoInicial(1)

        repeat(3) {
            val resultado = motor.aplicar(estado, Accion.Lanzar)
            assertTrue(resultado is ResultadoAccion.Exito)
            estado = (resultado as ResultadoAccion.Exito).estado
        }

        val cuartoLanzar = motor.aplicar(estado, Accion.Lanzar)

        assertEquals(ResultadoAccion.Rechazada(MotivoRechazo.SIN_LANZAMIENTOS_DISPONIBLES), cuartoLanzar)
        assertEquals(3, estado.tiradaActual!!.lanzamientos)
    }

    // --- AlternarRetencion ---

    @Test
    fun `alternar retencion es rechazada sin tirada en curso`() {
        val motor = MotorPartida(ruleSetDePrueba(1), Random(SEMILLA))

        val resultado = motor.aplicar(estadoInicial(1), Accion.AlternarRetencion(0))

        assertEquals(ResultadoAccion.Rechazada(MotivoRechazo.SIN_TIRADA_EN_CURSO), resultado)
    }

    @Test
    fun `alternar retencion es rechazada tras el tercer lanzamiento`() {
        val motor = MotorPartida(ruleSetDePrueba(1), Random(SEMILLA))
        var estado = estadoInicial(1)
        repeat(3) {
            estado = (motor.aplicar(estado, Accion.Lanzar) as ResultadoAccion.Exito).estado
        }

        val resultado = motor.aplicar(estado, Accion.AlternarRetencion(0))

        assertEquals(ResultadoAccion.Rechazada(MotivoRechazo.RETENCION_SIN_RELANZAMIENTO_FUTURO), resultado)
    }

    @Test
    fun `alternar retencion es rechazada con un indice de dado fuera de rango`() {
        val motor = MotorPartida(ruleSetDePrueba(1), Random(SEMILLA))
        val estado = (motor.aplicar(estadoInicial(1), Accion.Lanzar) as ResultadoAccion.Exito).estado

        val resultado = motor.aplicar(estado, Accion.AlternarRetencion(5))

        assertEquals(ResultadoAccion.Rechazada(MotivoRechazo.INDICE_DE_DADO_FUERA_DE_RANGO), resultado)
    }

    // --- Anotar ---

    @Test
    fun `anotar en una categoria cuya tirada no califica anota 0 y es legal (sacrificio)`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val tiradaQueNoEsYacht = Tirada(listOf(Dado(1), Dado(2), Dado(3), Dado(4), Dado(6)), lanzamientos = 1)
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaQueNoEsYacht
        )

        val resultado = motor.aplicar(estado, Accion.Anotar(CategoriaId("yacht")))

        assertTrue(resultado is ResultadoAccion.Exito)
        val jugador = (resultado as ResultadoAccion.Exito).estado.jugadores.first()
        assertEquals(0, jugador.anotaciones.getValue(CategoriaId("yacht")))
    }

    @Test
    fun `anotar es rechazada sobre una categoria ya usada por ese jugador`() {
        val ruleSet = ruleSetDePrueba(2)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val categoriaUsada = ruleSet.categorias[0].id
        val jugador = Jugador("Jugador 1", anotaciones = mapOf(categoriaUsada to 0))
        val estado = EstadoPartida(
            jugadores = listOf(jugador),
            indiceTurno = 0,
            tiradaActual = tiradaDePrueba()
        )

        val resultado = motor.aplicar(estado, Accion.Anotar(categoriaUsada))

        assertEquals(ResultadoAccion.Rechazada(MotivoRechazo.CATEGORIA_YA_USADA), resultado)
    }

    @Test
    fun `anotar es rechazada sin tirada en curso`() {
        val ruleSet = ruleSetDePrueba(1)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))

        val resultado = motor.aplicar(estadoInicial(1), Accion.Anotar(ruleSet.categorias[0].id))

        assertEquals(ResultadoAccion.Rechazada(MotivoRechazo.SIN_TIRADA_EN_CURSO), resultado)
    }

    @Test
    fun `anotar es rechazada sobre una categoria desconocida`() {
        val ruleSet = ruleSetDePrueba(1)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaDePrueba()
        )

        val resultado = motor.aplicar(estado, Accion.Anotar(CategoriaId("no_existe")))

        assertEquals(ResultadoAccion.Rechazada(MotivoRechazo.CATEGORIA_DESCONOCIDA), resultado)
    }

    @Test
    fun `anotar avanza el turno, deja tiradaActual en null y pierde la retencion`() {
        val ruleSet = ruleSetDePrueba(2)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val tirada = Tirada(listOf(Dado(1, retenido = true), Dado(2), Dado(3), Dado(4), Dado(5)), lanzamientos = 1)
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1"), Jugador("Jugador 2")),
            indiceTurno = 0,
            tiradaActual = tirada
        )

        val resultado = motor.aplicar(estado, Accion.Anotar(ruleSet.categorias[0].id)) as ResultadoAccion.Exito

        assertNull(resultado.estado.tiradaActual)
        assertEquals(1, resultado.estado.indiceTurno)
    }

    @Test
    fun `con varios jugadores el turno rota correctamente`() {
        val ruleSet = ruleSetDePrueba(1)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val categoriaId = ruleSet.categorias[0].id
        var estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1"), Jugador("Jugador 2"), Jugador("Jugador 3")),
            indiceTurno = 0,
            tiradaActual = tiradaDePrueba()
        )

        // Cada jugador solo tiene una categoría en este RuleSet de prueba, así que el
        // turno avanza aunque la categoría se repita: cada uno anota la suya propia.
        estado = estado.copy(tiradaActual = tiradaDePrueba())
        estado = (motor.aplicar(estado, Accion.Anotar(categoriaId)) as ResultadoAccion.Exito).estado
        assertEquals(1, estado.indiceTurno)

        estado = estado.copy(tiradaActual = tiradaDePrueba())
        estado = (motor.aplicar(estado, Accion.Anotar(categoriaId)) as ResultadoAccion.Exito).estado
        assertEquals(2, estado.indiceTurno)

        estado = estado.copy(tiradaActual = tiradaDePrueba())
        estado = (motor.aplicar(estado, Accion.Anotar(categoriaId)) as ResultadoAccion.Exito).estado
        assertEquals(0, estado.indiceTurno)
    }

    // --- Fin de partida ---

    @Test
    fun `la partida termina cuando todos completaron todas las categorias, ni antes ni despues`() {
        val ruleSet = ruleSetDePrueba(1)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val categoriaId = ruleSet.categorias[0].id
        var estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1"), Jugador("Jugador 2")),
            indiceTurno = 0,
            tiradaActual = tiradaDePrueba()
        )

        assertFalse(motor.partidaTerminada(estado))

        estado = (motor.aplicar(estado, Accion.Anotar(categoriaId)) as ResultadoAccion.Exito).estado
        assertFalse(motor.partidaTerminada(estado))

        estado = estado.copy(tiradaActual = tiradaDePrueba())
        estado = (motor.aplicar(estado, Accion.Anotar(categoriaId)) as ResultadoAccion.Exito).estado
        assertTrue(motor.partidaTerminada(estado))
    }

    @Test
    fun `toda accion es rechazada con la partida terminada`() {
        val ruleSet = ruleSetDePrueba(1)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val categoriaId = ruleSet.categorias[0].id
        var estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaDePrueba()
        )
        estado = (motor.aplicar(estado, Accion.Anotar(categoriaId)) as ResultadoAccion.Exito).estado
        assertTrue(motor.partidaTerminada(estado))

        val motivoEsperado = ResultadoAccion.Rechazada(MotivoRechazo.PARTIDA_TERMINADA)
        assertEquals(motivoEsperado, motor.aplicar(estado, Accion.Lanzar))
        assertEquals(motivoEsperado, motor.aplicar(estado, Accion.AlternarRetencion(0)))
        assertEquals(motivoEsperado, motor.aplicar(estado, Accion.Anotar(categoriaId)))
    }

    // --- Puntaje total ---

    @Test
    fun `el total aplica el bonus con subtotal superior 63 y no lo aplica con 62`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))

        val jugadorConBonus = Jugador(
            nombre = "Jugador 1",
            anotaciones = mapOf(
                CategoriaId("ones") to 3,
                CategoriaId("twos") to 6,
                CategoriaId("threes") to 9,
                CategoriaId("fours") to 12,
                CategoriaId("fives") to 15,
                CategoriaId("sixes") to 18
            )
        )
        val jugadorSinBonus = jugadorConBonus.copy(
            anotaciones = jugadorConBonus.anotaciones + (CategoriaId("ones") to 2)
        )

        val puntajeConBonus = motor.puntaje(jugadorConBonus)
        assertEquals(63, puntajeConBonus.subtotalSuperior)
        assertEquals(35, puntajeConBonus.bonus)
        assertEquals(0, puntajeConBonus.subtotalInferior)
        assertEquals(98, puntajeConBonus.total)
        assertEquals(63, puntajeConBonus.umbralBonus)

        val puntajeSinBonus = motor.puntaje(jugadorSinBonus)
        assertEquals(62, puntajeSinBonus.subtotalSuperior)
        assertEquals(0, puntajeSinBonus.bonus)
        assertEquals(62, puntajeSinBonus.total)
        assertEquals(63, puntajeSinBonus.umbralBonus)
    }

    @Test
    fun `el umbral del bonus es nulo cuando el RuleSet no tiene bonus de seccion superior`() {
        val motor = MotorPartida(ruleSetDePrueba(numeroDeCategorias = 3), Random(SEMILLA))

        val puntaje = motor.puntaje(Jugador("Jugador 1"))

        assertNull(puntaje.umbralBonus)
    }

    // --- categorias ---

    @Test
    fun `categorias expone los ids del RuleSet en su orden`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))

        val idsEsperados = listOf(
            "ones", "twos", "threes", "fours", "fives", "sixes",
            "choice", "four_dice", "full_house", "small_straight", "big_straight", "yacht"
        ).map { CategoriaId(it) }

        assertEquals(idsEsperados, motor.categorias)
    }

    // --- puntajeSiSeAnotara ---

    @Test
    fun `puntaje si se anotara es nulo sin tirada en curso`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))

        assertNull(motor.puntajeSiSeAnotara(estadoInicial(1), CategoriaId("yacht")))
    }

    @Test
    fun `puntaje si se anotara es nulo para una categoria desconocida`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaDePrueba()
        )

        assertNull(motor.puntajeSiSeAnotara(estado, CategoriaId("no_existe")))
    }

    @Test
    fun `puntaje si se anotara es 0 cuando la tirada no califica (sacrificio)`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val tiradaQueNoEsYacht = Tirada(listOf(Dado(1), Dado(2), Dado(3), Dado(4), Dado(6)), lanzamientos = 1)
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaQueNoEsYacht
        )

        assertEquals(0, motor.puntajeSiSeAnotara(estado, CategoriaId("yacht")))
    }

    @Test
    fun `puntaje si se anotara es el puntaje real cuando la tirada califica`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val tiradaYacht = Tirada(List(5) { Dado(4) }, lanzamientos = 1)
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaYacht
        )

        assertEquals(50, motor.puntajeSiSeAnotara(estado, CategoriaId("yacht")))
    }

    // --- accionesLegales ---

    @Test
    fun `en un turno recien iniciado la unica accion legal es lanzar`() {
        val ruleSet = ruleSetDePrueba(2)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))

        assertEquals(listOf(Accion.Lanzar), motor.accionesLegales(estadoInicial(1)))
    }

    @Test
    fun `tras el primer lanzamiento las acciones legales son lanzar, alternar cada dado y anotar cada categoria libre`() {
        val ruleSet = ruleSetDePrueba(2)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val estado = (motor.aplicar(estadoInicial(1), Accion.Lanzar) as ResultadoAccion.Exito).estado

        val esperadas = listOf(Accion.Lanzar) +
            (0..4).map { Accion.AlternarRetencion(it) } +
            ruleSet.categorias.map { Accion.Anotar(it.id) }

        assertEquals(esperadas, motor.accionesLegales(estado))
    }

    @Test
    fun `tras el tercer lanzamiento las unicas acciones legales son anotar`() {
        val ruleSet = ruleSetDePrueba(2)
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        var estado = estadoInicial(1)
        repeat(3) {
            estado = (motor.aplicar(estado, Accion.Lanzar) as ResultadoAccion.Exito).estado
        }

        val esperadas = ruleSet.categorias.map { Accion.Anotar(it.id) }

        assertEquals(esperadas, motor.accionesLegales(estado))
    }
}
