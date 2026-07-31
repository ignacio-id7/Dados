package cl.ignaciodiaz.dados.persistencia

import cl.ignaciodiaz.dados.core.modelo.EstadoPartida

// Guarda y recupera la partida en curso (decisión 12). cargar() nunca lanza: si no hay
// partida guardada o el JSON no se puede leer, devuelve null y la app empieza una
// partida nueva (decisión 40), en vez de caerse.
interface RepositorioPartida {
    suspend fun guardar(estado: EstadoPartida)
    suspend fun cargar(): EstadoPartida?

    // Elimina la partida guardada. La usa "Partida nueva": si solo se reemplazara el
    // estado en memoria, al reabrir la app volvería la partida terminada.
    suspend fun borrar()
}
