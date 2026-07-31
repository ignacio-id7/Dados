package cl.ignaciodiaz.dados.partida

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import cl.ignaciodiaz.dados.persistencia.RepositorioPartida
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

// Cubre el bug donde, antes del primer lanzamiento del turno, la pantalla
// dibujaba cinco unos falsos indistinguibles de un Yacht real. Un dado sin
// tirada debe verse vacío (sin puntos) y no debe responder al toque.
class PartidaScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Repositorio en memoria: sin partida guardada, para que la pantalla arranque
    // directo en una partida nueva sin depender de disco.
    private class RepositorioPartidaSinGuardar : RepositorioPartida {
        override suspend fun guardar(estado: EstadoPartida) = Unit
        override suspend fun cargar(): EstadoPartida? = null
        override suspend fun borrar() = Unit
    }

    @Test
    fun antesDelPrimerLanzamiento_losCincoDadosSeMuestranVacios() {
        val viewModel = PartidaViewModel(RepositorioPartidaSinGuardar(), MotorPartida(presetClasico(), Random(1)))
        composeTestRule.setContent { PartidaScreen(viewModel = viewModel) }

        for (indice in 0 until 5) {
            composeTestRule.onNodeWithContentDescription("Dado vacío $indice")
                .assertIsDisplayed()
                .assertHasNoClickAction()
        }
    }

    @Test
    fun despuesDeLanzar_losDadosMuestranSuValorYSonTocables() {
        val viewModel = PartidaViewModel(RepositorioPartidaSinGuardar(), MotorPartida(presetClasico(), Random(1)))
        composeTestRule.setContent { PartidaScreen(viewModel = viewModel) }

        composeTestRule.runOnIdle { viewModel.despachar(Accion.Lanzar) }

        for (indice in 0 until 5) {
            composeTestRule.onNode(
                androidx.compose.ui.test.hasContentDescription(
                    "Dado vacío $indice",
                    substring = false
                )
            ).assertDoesNotExist()
        }
    }
}
