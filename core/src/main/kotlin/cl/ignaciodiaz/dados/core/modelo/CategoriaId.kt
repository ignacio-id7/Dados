package cl.ignaciodiaz.dados.core.modelo

import kotlinx.serialization.Serializable

// Identificador de una categoría basado en texto, envuelto en un tipo propio para no
// confundirlo con un String cualquiera (decisión 23). Agregar una categoría nueva es
// construir otro RuleSet, no editar un enum.
@Serializable
@JvmInline
value class CategoriaId(val valor: String)
