package cl.ignaciodiaz.dados.haptica

// Abstrae la retroalimentación háptica para que PartidaViewModel (que no es un
// composable y no puede leer LocalHapticFeedback) la dispare sin conocer Compose, y
// para que los tests inyecten una falsa en vez de tocar un Vibrator de verdad.
// :core no se entera de que esto existe: es presentación, vive en :app-mobile.
fun interface RetroalimentacionHaptica {
    fun alLanzar()
}
