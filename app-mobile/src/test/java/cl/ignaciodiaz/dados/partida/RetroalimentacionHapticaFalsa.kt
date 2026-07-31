package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.haptica.RetroalimentacionHaptica

// Cuenta invocaciones en vez de vibrar, para que los tests verifiquen cuándo se
// disparó la háptica sin tocar hardware.
class RetroalimentacionHapticaFalsa : RetroalimentacionHaptica {
    var vecesInvocada = 0
        private set

    override fun alLanzar() {
        vecesInvocada++
    }
}
