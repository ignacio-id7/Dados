package cl.ignaciodiaz.dados.core.motor

// Por qué una acción fue rechazada. Sin texto: :core no conoce idioma de interfaz
// (CLAUDE.md); cada app traduce el motivo a su propio texto.
enum class MotivoRechazo {
    PARTIDA_TERMINADA,
    SIN_LANZAMIENTOS_DISPONIBLES,
    SIN_TIRADA_EN_CURSO,
    RETENCION_SIN_RELANZAMIENTO_FUTURO,
    INDICE_DE_DADO_FUERA_DE_RANGO,
    CATEGORIA_DESCONOCIDA,
    CATEGORIA_YA_USADA
}
