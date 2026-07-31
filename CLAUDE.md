# CLAUDE.md

Reglas del proyecto Dados (juego de dados original, inspirado en la mecánica de dominio público de Yacht; Android). No es documentación: para contexto y decisiones ver `docs/`.

## Restricciones de arquitectura no negociables

- **`:core` es Kotlin puro.** Prohibido importar `android.*`, Compose o cualquier librería de UI. Prohibido referenciar tamaños de pantalla, colores, recursos o strings de interfaz. Prohibido acceso directo a disco, red o preferencias.
- **Ninguna regla del juego vive en un composable ni en un ViewModel.** Toda lógica de juego (dados, retención/relanzamiento, evaluación de categorías, puntajes, bonus, turnos, jugadas legales) va en `:core`. Antes de escribir código nuevo, preguntar: ¿es lógica de juego o es presentación? Lógica → `:core`, sirve para celular y reloj. Presentación → módulo de la app, se implementa una vez por dispositivo.
- **El motor se parametriza con un `RuleSet` inmutable.** El motor recorre `ruleSet.categorias`, nunca un enum fijo, y no hardcodea puntajes ni el conjunto de categorías. No agregar atajos que asuman el preset "Clásico" a nivel de motor.
- **Todo el código se comenta en español.**
- **Generación aleatoria inyectable** en `:core` (necesaria para tests con semilla fija y para el futuro modo desafío diario).
- **`EstadoPartida` modela una lista de jugadores** e índice de turno desde el primer commit, aunque el MVP use un solo jugador. No colapsar el modelo a un solo jugador "para simplificar".

Violar estas reglas "para ir más rápido" obliga a reescribir en la Fase B (Wear OS), que reutiliza `:core` sin reescribir lógica.

## Estado actual de los módulos

- **`app-mobile`** — único módulo existente hoy, plantilla Empty Activity generada por Android Studio (Compose, package `cl.ignaciodiaz.dados`, `minSdk 26`, `targetSdk 36`). Ya renombrado desde `app`.
- **`:core`** — no existe todavía. Próximo paso: crearlo como módulo Kotlin puro (Kotlin JVM library, sin plugin de Android) y declarar la dependencia `:app-mobile → :core`.
- **`:app-wear`** — no existe. Fase B, posterior a que la Fase A esté completa y jugable.

Sin código propio del juego escrito aún. Detalle y siguiente paso exacto en `docs/00-estado-actual.md`.
