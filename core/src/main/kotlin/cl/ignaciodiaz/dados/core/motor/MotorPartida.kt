package cl.ignaciodiaz.dados.core.motor

import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.Dado
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.RuleSet
import cl.ignaciodiaz.dados.core.modelo.Seccion
import cl.ignaciodiaz.dados.core.modelo.Tirada
import kotlin.random.Random

// Recibe un estado y una acción, y devuelve el estado nuevo o el motivo del rechazo.
// No guarda estado propio: el estado entra y sale (decisión 29). Se construye con el
// RuleSet de la partida y la fuente de aleatoriedad para los lanzamientos.
class MotorPartida(
    private val ruleSet: RuleSet,
    private val random: Random
) {

    fun aplicar(estado: EstadoPartida, accion: Accion): ResultadoAccion {
        val motivo = motivoRechazo(estado, accion)
        return if (motivo != null) {
            ResultadoAccion.Rechazada(motivo)
        } else {
            ResultadoAccion.Exito(ejecutar(estado, accion))
        }
    }

    // Todas las acciones que aplicar() aceptaría ahora mismo, en el mismo orden.
    fun accionesLegales(estado: EstadoPartida): List<Accion> = buildList {
        if (esLegal(estado, Accion.Lanzar)) add(Accion.Lanzar)
        estado.tiradaActual?.dados?.indices?.forEach { indice ->
            val accion = Accion.AlternarRetencion(indice)
            if (esLegal(estado, accion)) add(accion)
        }
        ruleSet.categorias.forEach { categoria ->
            val accion = Accion.Anotar(categoria.id)
            if (esLegal(estado, accion)) add(accion)
        }
    }

    // Todos los jugadores tienen tantas anotaciones como categorías tiene el RuleSet.
    fun partidaTerminada(estado: EstadoPartida): Boolean =
        estado.jugadores.all { it.anotaciones.size == ruleSet.numeroDeTurnos }

    // Desglose del puntaje de un jugador: subtotal de sección superior, bonus si
    // alcanza el umbral, subtotal de sección inferior y total. Vive acá porque el
    // motor es quien conoce el RuleSet y por lo tanto la sección de cada categoría.
    fun puntaje(jugador: Jugador): Puntaje {
        val categoriaPorId = ruleSet.categorias.associateBy { it.id }
        val subtotalSuperior = jugador.anotaciones
            .filterKeys { categoriaPorId.getValue(it).seccion == Seccion.SUPERIOR }
            .values.sum()
        val subtotalInferior = jugador.anotaciones
            .filterKeys { categoriaPorId.getValue(it).seccion == Seccion.INFERIOR }
            .values.sum()
        val bonus = ruleSet.bonusSeccionSuperior
            ?.takeIf { subtotalSuperior >= it.umbral }
            ?.valor
            ?: 0
        return Puntaje(
            subtotalSuperior = subtotalSuperior,
            bonus = bonus,
            subtotalInferior = subtotalInferior,
            total = subtotalSuperior + bonus + subtotalInferior
        )
    }

    private fun esLegal(estado: EstadoPartida, accion: Accion): Boolean =
        motivoRechazo(estado, accion) == null

    // Única fuente de verdad de la legalidad: null significa que la acción es legal.
    // Cada causa real de rechazo tiene su propio motivo (CLAUDE.md prohíbe texto de
    // interfaz en :core; el motivo es un dato que cada app traduce).
    private fun motivoRechazo(estado: EstadoPartida, accion: Accion): MotivoRechazo? {
        if (partidaTerminada(estado)) return MotivoRechazo.PARTIDA_TERMINADA

        val tiradaActual = estado.tiradaActual
        return when (accion) {
            is Accion.Lanzar -> when {
                tiradaActual != null && !tiradaActual.puedeRelanzar -> MotivoRechazo.SIN_LANZAMIENTOS_DISPONIBLES
                else -> null
            }
            is Accion.AlternarRetencion -> when {
                tiradaActual == null -> MotivoRechazo.SIN_TIRADA_EN_CURSO
                !tiradaActual.puedeRelanzar -> MotivoRechazo.RETENCION_SIN_RELANZAMIENTO_FUTURO
                accion.indice !in tiradaActual.dados.indices -> MotivoRechazo.INDICE_DE_DADO_FUERA_DE_RANGO
                else -> null
            }
            is Accion.Anotar -> when {
                tiradaActual == null -> MotivoRechazo.SIN_TIRADA_EN_CURSO
                ruleSet.categorias.none { it.id == accion.categoriaId } -> MotivoRechazo.CATEGORIA_DESCONOCIDA
                estado.jugadorEnTurno.anotaciones.containsKey(accion.categoriaId) -> MotivoRechazo.CATEGORIA_YA_USADA
                else -> null
            }
        }
    }

    private fun ejecutar(estado: EstadoPartida, accion: Accion): EstadoPartida = when (accion) {
        is Accion.Lanzar -> estado.copy(tiradaActual = lanzar(estado.tiradaActual))
        is Accion.AlternarRetencion -> estado.copy(
            tiradaActual = alternarRetencion(estado.tiradaActual!!, accion.indice)
        )
        is Accion.Anotar -> anotar(estado, accion.categoriaId)
    }

    private fun lanzar(tiradaActual: Tirada?): Tirada {
        val dados = if (tiradaActual == null) {
            List(Tirada.CANTIDAD_DE_DADOS) { Dado(valor = random.nextInt(1, 7)) }
        } else {
            tiradaActual.dados.map { dado ->
                if (dado.retenido) dado else dado.copy(valor = random.nextInt(1, 7))
            }
        }
        return Tirada(dados, (tiradaActual?.lanzamientos ?: 0) + 1)
    }

    private fun alternarRetencion(tiradaActual: Tirada, indice: Int): Tirada =
        tiradaActual.copy(
            dados = tiradaActual.dados.mapIndexed { i, dado ->
                if (i == indice) dado.copy(retenido = !dado.retenido) else dado
            }
        )

    private fun anotar(estado: EstadoPartida, categoriaId: CategoriaId): EstadoPartida {
        val tiradaActual = estado.tiradaActual!!
        val categoria = ruleSet.categorias.first { it.id == categoriaId }
        val puntaje = if (categoria.esValida(tiradaActual)) categoria.puntaje(tiradaActual) else 0

        val jugadorActual = estado.jugadorEnTurno
        val jugadorAnotado = jugadorActual.copy(
            anotaciones = jugadorActual.anotaciones + (categoriaId to puntaje)
        )
        val jugadores = estado.jugadores.mapIndexed { i, jugador ->
            if (i == estado.indiceTurno) jugadorAnotado else jugador
        }

        return estado.copy(
            jugadores = jugadores,
            indiceTurno = (estado.indiceTurno + 1) % jugadores.size,
            tiradaActual = null
        )
    }
}
