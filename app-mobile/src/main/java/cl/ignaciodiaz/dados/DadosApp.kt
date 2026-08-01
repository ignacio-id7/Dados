package cl.ignaciodiaz.dados

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cl.ignaciodiaz.dados.menu.MenuScreen
import cl.ignaciodiaz.dados.partida.PartidaScreen

// Navegación por estado en el composable raíz, sin librería de navegación (decisión
// 44): con dos pantallas alcanza una variable que indica cuál se muestra.
//
// MenuViewModel y PartidaViewModel son una única instancia por clase en el
// ViewModelStore de la Activity (viewModel() sin key): no se crea una instancia nueva
// por navegación, así que no queda ninguna abandonada ahí. Cada pantalla se recarga
// desde el repositorio al entrar (MenuScreen.refrescar() / PartidaViewModel.recargar()
// vía LaunchedEffect), que es lo que evita mostrar datos de la visita anterior.
private enum class Pantalla { Menu, Partida }

@Composable
fun DadosApp(modifier: Modifier = Modifier) {
    var pantalla by remember { mutableStateOf(Pantalla.Menu) }

    // Estando en la partida, atrás vuelve al menú en vez de cerrar la app. Estando en
    // el menú, atrás cierra la app como es habitual: el handler solo se activa en
    // Partida (decisión 44).
    BackHandler(enabled = pantalla == Pantalla.Partida) {
        pantalla = Pantalla.Menu
    }

    when (pantalla) {
        Pantalla.Menu -> MenuScreen(
            onContinuar = { pantalla = Pantalla.Partida },
            onPartidaNueva = { pantalla = Pantalla.Partida },
            modifier = modifier
        )
        Pantalla.Partida -> PartidaScreen(modifier = modifier)
    }
}
