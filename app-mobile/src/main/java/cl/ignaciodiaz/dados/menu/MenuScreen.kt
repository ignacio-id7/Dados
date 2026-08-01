package cl.ignaciodiaz.dados.menu

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.ignaciodiaz.dados.persistencia.RepositorioPartidaDataStore
import kotlinx.coroutines.launch

// Punto de entrada de la app (decisión 45). Sin librería de navegación (decisión 44):
// esta pantalla solo avisa por callback, quien decide a dónde ir es DadosApp.
@Composable
fun MenuScreen(
    viewModel: MenuViewModel = viewModel(factory = fabricaMenuViewModel(LocalContext.current)),
    onContinuar: () -> Unit,
    onPartidaNueva: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val alcance = rememberCoroutineScope()

    // El ViewModel sobrevive mientras la Activity vive, aunque esta pantalla salga y
    // vuelva a entrar en composición (por ejemplo, al volver con atrás desde la
    // partida). Sin este refresco en cada entrada, "Continuar" seguiría mostrando la
    // respuesta de la primera vez que se armó el menú.
    LaunchedEffect(Unit) { viewModel.refrescar() }

    // Saber si hay partida guardada es asíncrono (decisión 45): no se dibujan los
    // botones definitivos antes de saberlo, para que "Partida nueva" no salte de
    // lugar cuando aparece "Continuar" un instante después.
    if (uiState.cargando) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var mostrarConfirmacion by remember { mutableStateOf(false) }

    fun empezarPartidaNueva() {
        alcance.launch {
            viewModel.partidaNueva()
            onPartidaNueva()
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Dados", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.hayPartidaGuardada) {
            Button(onClick = onContinuar) { Text("Continuar") }
            Spacer(modifier = Modifier.height(24.dp))
        }

        OutlinedButton(
            onClick = {
                if (uiState.partidaGuardadaSinTerminar) mostrarConfirmacion = true else empezarPartidaNueva()
            }
        ) { Text("Partida nueva") }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("¿Empezar partida nueva?") },
            text = { Text("Se perderá el progreso de la partida en curso.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    empezarPartidaNueva()
                }) { Text("Empezar de nuevo") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) { Text("Cancelar") }
            }
        )
    }
}

// Instancia MenuViewModel con el repositorio de verdad, el mismo que usa la partida
// (decisión 12): ambas pantallas leen y escriben el mismo archivo.
private fun fabricaMenuViewModel(context: Context): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { MenuViewModel(RepositorioPartidaDataStore(context.applicationContext)) }
    }
