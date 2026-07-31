package cl.ignaciodiaz.dados.partida

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.Dado as DadoModelo
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.Tirada
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.Puntaje

@Composable
fun PartidaScreen(viewModel: PartidaViewModel = viewModel(), modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val jugador = uiState.estado.jugadorEnTurno
    val dadosAMostrar = uiState.estado.tiradaActual?.dados
        ?: List(Tirada.CANTIDAD_DE_DADOS) { DadoModelo(valor = 1) }

    Column(
        modifier = modifier
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
    }
}

@Composable
private fun FilaDados(
    dados: List<DadoModelo>,
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
                tocable = tocable,
                onTocar = { onTocarDado(indice) }
            )
        }
    }
}

@Composable
private fun Dado(
    dado: DadoModelo,
    tocable: Boolean,
    onTocar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorFondo = if (dado.retenido) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val colorPunto = if (dado.retenido) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Canvas(
        modifier = modifier
            .size(56.dp)
            .let { base -> if (tocable) base.clickable(onClick = onTocar) else base }
    ) {
        drawRoundRect(
            color = colorFondo,
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
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
        Text("Subtotal superior: ${puntaje.subtotalSuperior}")
        Text(if (puntaje.bonus > 0) "Bonus: +${puntaje.bonus}" else "Bonus: no alcanzado")
        Text("Total: ${puntaje.total}", fontWeight = FontWeight.Bold)
    }
}
