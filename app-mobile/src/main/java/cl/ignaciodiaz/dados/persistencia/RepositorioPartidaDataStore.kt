package cl.ignaciodiaz.dados.persistencia

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

// Serializa EstadoPartida a JSON para guardarlo en DataStore. Si el archivo no existe,
// DataStore nunca llama a readFrom y entrega defaultValue directamente. Si el archivo
// existe pero no se puede leer (JSON corrupto, truncado por un proceso muerto a mitad
// de escritura, etc.), readFrom atrapa el error acá mismo y devuelve null en vez de
// propagarlo: así el repositorio nunca hace caer la app (decisión 12).
private object SerializadorEstadoPartida : Serializer<EstadoPartida?> {
    override val defaultValue: EstadoPartida? = null

    override suspend fun readFrom(input: InputStream): EstadoPartida? = try {
        Json.decodeFromString(EstadoPartida.serializer(), input.readBytes().decodeToString())
    } catch (excepcion: CancellationException) {
        throw excepcion
    } catch (excepcion: Exception) {
        null
    }

    override suspend fun writeTo(t: EstadoPartida?, output: OutputStream) {
        if (t != null) {
            output.write(Json.encodeToString(EstadoPartida.serializer(), t).encodeToByteArray())
        }
    }
}

private val Context.dataStoreDePartida: DataStore<EstadoPartida?> by dataStore(
    fileName = "partida.json",
    serializer = SerializadorEstadoPartida
)

// Implementación con DataStore (decisión 12): escritura atómica, así que un proceso
// muerto a mitad de guardar nunca deja un archivo corrupto a medio escribir.
class RepositorioPartidaDataStore(context: Context) : RepositorioPartida {
    private val dataStore = context.applicationContext.dataStoreDePartida

    override suspend fun guardar(estado: EstadoPartida) {
        dataStore.updateData { estado }
    }

    override suspend fun cargar(): EstadoPartida? = dataStore.data.first()

    override suspend fun borrar() {
        dataStore.updateData { null }
    }
}
