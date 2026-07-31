package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.persistencia.RepositorioPartida

// Repositorio en memoria para tests: no toca disco. Recuerda cada estado guardado,
// en orden, y cuántas veces se borró, para que los tests puedan verificar el efecto
// exacto de cada operación.
class RepositorioPartidaFalso(private var estadoGuardado: EstadoPartida? = null) : RepositorioPartida {

    val estadosGuardados = mutableListOf<EstadoPartida>()

    var vecesBorrado = 0
        private set

    // Acceso directo (no suspend) para que los tests inspeccionen el estado sin
    // necesitar un scope de corrutinas.
    val estadoActual: EstadoPartida?
        get() = estadoGuardado

    override suspend fun guardar(estado: EstadoPartida) {
        estadoGuardado = estado
        estadosGuardados += estado
    }

    override suspend fun cargar(): EstadoPartida? = estadoGuardado

    override suspend fun borrar() {
        estadoGuardado = null
        vecesBorrado++
    }
}
