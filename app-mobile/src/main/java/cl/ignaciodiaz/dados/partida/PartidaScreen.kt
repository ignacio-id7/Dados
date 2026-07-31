package cl.ignaciodiaz.dados.partida

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.Dado as DadoModelo
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.Tirada
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.Puntaje
import cl.ignaciodiaz.dados.persistencia.RepositorioPartidaDataStore

@Composable
fun PartidaScreen(
    viewModel: PartidaViewModel = viewModel(factory = fabricaPartidaViewModel(LocalContext.current)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // Mientras se espera la respuesta del repositorio no se dibuja el tablero: el
    // jugador vería una partida nueva que luego cambia por la partida guardada
    // (decisión 40, requisito de la pantalla).
    if (uiState.cargando) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // El panel de fin de partida se puede cerrar para revisar la tabla y reabrir
    // (decisión 42). "panelCerrado" es presentación pura, no una regla del juego: el
    // motor sigue siendo el único que decide partidaTerminada. Se reabre solo cuando
    // termina una partida nueva (la transición false -> true), no en cada recomposición.
    var panelCerrado by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.partidaTerminada) {
        if (uiState.partidaTerminada) panelCerrado = false
    }

    val jugador = uiState.estado.jugadorEnTurno
    val dadosAMostrar: List<DadoModelo?> = uiState.estado.tiradaActual?.dados
        ?: List(Tirada.CANTIDAD_DE_DADOS) { null }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lanzamientos: ${uiState.estado.tiradaActual?.lanzamientos ?: 0} / ${Tirada.MAXIMO_DE_LANZAMIENTOS}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            FilaDados(
                dados = dadosAMostrar,
                accionesLegales = uiState.accionesLegales,
                onTocarDado = { indice -> viewModel.despachar(Accion.AlternarRetencion(indice)) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            BotonLanzar(
                habilitado = Accion.Lanzar in uiState.accionesLegales,
                onClick = { viewModel.despachar(Accion.Lanzar) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            TablaCategorias(
                categorias = viewModel.categorias,
                jugador = jugador,
                accionesLegales = uiState.accionesLegales,
                previsualizaciones = uiState.previsualizaciones,
                onAnotar = { categoriaId -> viewModel.despachar(Accion.Anotar(categoriaId)) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            ResumenPuntaje(puntaje = uiState.puntaje)

            if (uiState.partidaTerminada && panelCerrado) {
                TextButton(onClick = { panelCerrado = false }) {
                    Text("Ver resultado final")
                }
            }
        }

        if (uiState.partidaTerminada && !panelCerrado) {
            PanelFinDePartida(
                puntaje = uiState.puntaje,
                onCerrar = { panelCerrado = true },
                onPartidaNueva = { viewModel.iniciarPartidaNueva() }
            )
        }
    }
}

@Composable
private fun FilaDados(
    dados: List<DadoModelo?>,
    accionesLegales: List<Accion>,
    onTocarDado: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dados.forEachIndexed { indice, dado ->
            val tocable = Accion.AlternarRetencion(indice) in accionesLegales
            Dado(
                dado = dado,
                indice = indice,
                tocable = tocable,
                onTocar = { onTocarDado(indice) }
            )
        }
    }
}

// dado es null cuando el turno todavía no lanzó: se dibuja un dado vacío (sin
// puntos, en tono apagado) en vez de inventar un valor, para que nunca se
// confunda con una tirada real. Un dado vacío tampoco es tocable, aunque
// tocable venga en true.
@Composable
private fun Dado(
    dado: DadoModelo?,
    indice: Int,
    tocable: Boolean,
    onTocar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorFondo = when {
        dado == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        dado.retenido -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val colorPunto = if (dado?.retenido == true) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val descripcion = if (dado == null) "Dado vacío $indice" else "Dado con valor ${dado.valor}"

    Canvas(
        modifier = modifier
            .size(56.dp)
            .semantics { contentDescription = descripcion }
            .let { base -> if (tocable && dado != null) base.clickable(onClick = onTocar) else base }
    ) {
        drawRoundRect(
            color = colorFondo,
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        if (dado == null) return@Canvas
        val centro = size.width / 2f
        val desplazamiento = size.width / 4f
        val radio = size.width / 12f
        val posiciones = when (dado.valor) {
            1 -> listOf(Offset(centro, centro))
            2 -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
            3 -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro, centro),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
            4 -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro + desplazamiento, centro - desplazamiento),
                Offset(centro - desplazamiento, centro + desplazamiento),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
            5 -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro + desplazamiento, centro - desplazamiento),
                Offset(centro, centro),
                Offset(centro - desplazamiento, centro + desplazamiento),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
            else -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro + desplazamiento, centro - desplazamiento),
                Offset(centro - desplazamiento, centro),
                Offset(centro + desplazamiento, centro),
                Offset(centro - desplazamiento, centro + desplazamiento),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
        }
        posiciones.forEach { posicion -> drawCircle(color = colorPunto, radius = radio, center = posicion) }
    }
}

@Composable
private fun BotonLanzar(habilitado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = habilitado, modifier = modifier) {
        Text("Lanzar")
    }
}

@Composable
private fun TablaCategorias(
    categorias: List<CategoriaId>,
    jugador: Jugador,
    accionesLegales: List<Accion>,
    previsualizaciones: Map<CategoriaId, Int?>,
    onAnotar: (CategoriaId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        categorias.forEach { categoriaId ->
            val anotado = jugador.anotaciones[categoriaId]
            val interactiva = Accion.Anotar(categoriaId) in accionesLegales
            val textoPuntaje = when {
                anotado != null -> anotado.toString()
                else -> previsualizaciones[categoriaId]?.toString() ?: ""
            }
            val colorTexto = if (anotado != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val pesoTexto = if (anotado != null) FontWeight.Bold else FontWeight.Normal

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { base -> if (interactiva) base.clickable { onAnotar(categoriaId) } else base }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = nombreDeCategoria(categoriaId))
                Text(text = textoPuntaje, color = colorTexto, fontWeight = pesoTexto)
            }
        }
    }
}

@Composable
private fun ResumenPuntaje(puntaje: Puntaje, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        // El umbral viaja en Puntaje (decisión 43): la pantalla nunca conoce el 63,
        // solo lo muestra si el motor lo entrega.
        val textoSubtotal = if (puntaje.umbralBonus != null) {
            "Subtotal superior: ${puntaje.subtotalSuperior}/${puntaje.umbralBonus}"
        } else {
            "Subtotal superior: ${puntaje.subtotalSuperior}"
        }
        Text(textoSubtotal)
        Text(if (puntaje.bonus > 0) "Bonus: +${puntaje.bonus}" else "Bonus: no alcanzado")
        Text("Total: ${puntaje.total}", fontWeight = FontWeight.Bold)
    }
}

// Panel sobre el tablero, no una pantalla aparte (decisión 42): el jugador acaba de
// pasar doce turnos construyendo la tabla y quiere seguir viéndola. Quién decide que
// la partida terminó es el motor (partidaTerminada en PartidaUiState); este panel solo
// reacciona.
@Composable
private fun PanelFinDePartida(
    puntaje: Puntaje,
    onCerrar: () -> Unit,
    onPartidaNueva: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(modifier = Modifier.padding(24.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Partida terminada", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Subtotal superior: ${puntaje.subtotalSuperior}")
                Text(if (puntaje.bonus > 0) "Bonus: +${puntaje.bonus}" else "Bonus: no alcanzado")
                Text("Subtotal inferior: ${puntaje.subtotalInferior}")
                Text(
                    "Total: ${puntaje.total}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    TextButton(onClick = onCerrar) { Text("Cerrar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onPartidaNueva) { Text("Partida nueva") }
                }
            }
        }
    }
}

// Instancia PartidaViewModel con el repositorio de verdad (decisión 41). El contexto de
// aplicación evita que el DataStore se recree en cada recomposición.
private fun fabricaPartidaViewModel(context: Context): ViewModelProvider.Factory =
    viewModelFactory {
        initializer { PartidaViewModel(RepositorioPartidaDataStore(context.applicationContext)) }
    }
