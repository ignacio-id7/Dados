package cl.ignaciodiaz.dados.menu

// Lo que la pantalla de menú necesita para dibujarse.
//
// "cargando" es true mientras se consulta el repositorio (decisión 45): saber si hay
// partida guardada es asíncrono, y no se dibujan los botones definitivos antes de
// saberlo, para que "Partida nueva" no salte de lugar cuando aparece "Continuar".
//
// "hayPartidaGuardada" decide si se muestra "Continuar". "partidaGuardadaSinTerminar"
// (guardada y no terminada) decide si "Partida nueva" pide confirmación antes de
// borrar: una partida terminada, o la ausencia de partida, no tiene nada que perder.
data class MenuUiState(
    val cargando: Boolean,
    val hayPartidaGuardada: Boolean,
    val partidaGuardadaSinTerminar: Boolean
)
