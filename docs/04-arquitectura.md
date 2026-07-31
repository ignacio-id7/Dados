# Arquitectura del proyecto

## Estructura de módulos

    yacht-dice/
    ├── core/            Kotlin puro — motor del juego
    ├── app-mobile/      Aplicación Android para celular (Fase A)
    └── app-wear/        Aplicación Wear OS (Fase B, no existe todavía)

## Contrato del módulo :core

Responsabilidades:

- Representación de los dados y del estado de la tirada.
- Lógica de retención y relanzamiento (máximo 3 lanzamientos por turno).
- Definición del `RuleSet`: objeto inmutable con las categorías de la partida, sus fórmulas de puntaje, las reglas de validez configurables y el bonus de sección superior.
- Evaluación de categorías y cálculo de puntajes **contra el `RuleSet` recibido**. El motor recorre `ruleSet.categorias`, nunca un enum fijo, y no hardcodea ningún valor de puntaje.
- Cálculo del bonus de la sección superior (umbral y valor vienen del `RuleSet`).
- Estado de la partida: **lista de jugadores** e índice de turno (decisión 19), turno actual, categorías ya usadas por jugador, puntaje acumulado. El MVP la usa con un solo jugador, pero el modelo no asume que haya uno solo.
- Validación de jugadas legales.
- Generación aleatoria inyectable (para poder testear con semilla fija y para el modo desafío diario).

Prohibido en `:core`:

- Cualquier import de `android.*`.
- Cualquier import de Compose o de librerías de UI.
- Referencias a tamaños de pantalla, colores, recursos o strings de interfaz.
- Acceso directo a disco, red o preferencias.

Salida esperada: una API que reciba acciones ("lanzar", "retener dado n", "anotar en categoría X") y devuelva el nuevo estado de la partida.

## Modelo de datos de `:core`

Cerrado el 2026-07-31 (decisiones 22 a 26).

**Principio rector: el `RuleSet` es código, el `EstadoPartida` es datos.** El `RuleSet` contiene funciones y se construye en memoria al arrancar; no se serializa nunca. El `EstadoPartida` contiene solo valores planos y es lo único que se persiste.

Tipos:

- **`Dado`** — valor de 1 a 6 y si está retenido. Los cinco dados conservan su posición en la lista entre lanzamientos: el índice tiene que seguir significando el mismo dado cuando el jugador lo toca en la interfaz.
- **`Tirada`** — los 5 dados y cuántos lanzamientos se han usado (0 a 3). Sabe si todavía se puede relanzar.
- **`CategoriaId`** — identificador estable basado en texto, envuelto en un tipo propio para no confundirlo con un `String` cualquiera.
- **`Categoria`** — `CategoriaId`, sección, función de validez (`¿esta tirada califica?`) y función de puntaje (`¿cuánto vale?`). Choice, Four Dice y Full House comparten función de puntaje y se diferencian solo en la de validez.
- **`RuleSet`** — lista de categorías y bonus de sección superior (umbral y valor, o ninguno). El número de turnos se deriva de contar las categorías; no existe una constante `12`.
- **`EstadoPartida`** — lista de jugadores, índice de turno, tirada actual y anotaciones por jugador.
- **`MotorPartida`** — recibe acciones (`Lanzar`, `AlternarRetencion(indice)`, `Anotar(categoriaId)`) y devuelve el estado nuevo o un error si la jugada es ilegal.

Todo el modelo es inmutable: cada acción devuelve un estado nuevo con `copy()`.

La aleatoriedad se inyecta como `kotlin.random.Random`, sin interfaz propia intermedia.

## Regla de portabilidad

Cualquier funcionalidad nueva se evalúa con esta pregunta antes de implementarla: ¿es lógica de juego o es presentación? Si es lógica, va en `:core` y sirve para ambos dispositivos. Si es presentación, va en el módulo de la aplicación correspondiente y se implementará dos veces, una por dispositivo.

## Deuda técnica aceptada

Se acepta implementar la interfaz de la Fase A sin optimizarla para pantallas pequeñas. No se acepta implementar lógica de juego dentro de la interfaz "para ir más rápido": eso obliga a reescribir todo en la Fase B.
