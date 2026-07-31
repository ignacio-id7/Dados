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

## Regla de portabilidad

Cualquier funcionalidad nueva se evalúa con esta pregunta antes de implementarla: ¿es lógica de juego o es presentación? Si es lógica, va en `:core` y sirve para ambos dispositivos. Si es presentación, va en el módulo de la aplicación correspondiente y se implementará dos veces, una por dispositivo.

## Deuda técnica aceptada

Se acepta implementar la interfaz de la Fase A sin optimizarla para pantallas pequeñas. No se acepta implementar lógica de juego dentro de la interfaz "para ir más rápido": eso obliga a reescribir todo en la Fase B.
