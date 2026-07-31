package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.persistencia.RepositorioPartida

// Repositorio en memoria para tests: no toca disco. Recuerda cada estado guardado,
// en orden, para que los tests puedan verificar cuántas veces y con qué estado se guardó.
class RepositorioPartidaFalso(private var estadoGuardado: EstadoPartida? = null) : RepositorioPartida {

    val estadosGuardados = mutableListOf<EstadoPartida>()

    override suspend fun guardar(estado: EstadoPartida) {
        estadoGuardado = estado
        estadosGuardados += estado
    }

    override suspend fun cargar(): EstadoPartida? = estadoGuardado
}
