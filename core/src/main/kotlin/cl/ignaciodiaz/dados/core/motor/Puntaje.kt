package cl.ignaciodiaz.dados.core.motor

// El desglose del puntaje de un jugador: subtotal de sección superior, el bonus si
// se alcanzó el umbral (0 si no), subtotal de sección inferior y el total. Lo arma
// MotorPartida porque es quien conoce el RuleSet y por lo tanto la sección de cada
// categoría.
data class Puntaje(
    val subtotalSuperior: Int,
    val bonus: Int,
    val subtotalInferior: Int,
    val total: Int
)
