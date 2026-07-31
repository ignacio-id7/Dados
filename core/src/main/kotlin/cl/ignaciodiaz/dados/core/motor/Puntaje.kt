package cl.ignaciodiaz.dados.core.motor

// El desglose del puntaje de un jugador: subtotal de sección superior, el bonus si
// se alcanzó el umbral (0 si no), subtotal de sección inferior y el total. Lo arma
// MotorPartida porque es quien conoce el RuleSet y por lo tanto la sección de cada
// categoría.
//
// umbralBonus es el umbral que hay que alcanzar para ganar el bonus (63 en el preset
// Clásico), o null si el RuleSet no tiene bonus de sección superior. Viaja acá para que
// la interfaz pueda mostrar el progreso ("60/63") sin conocer ese número (decisión 43).
data class Puntaje(
    val subtotalSuperior: Int,
    val bonus: Int,
    val subtotalInferior: Int,
    val total: Int,
    val umbralBonus: Int?
)
