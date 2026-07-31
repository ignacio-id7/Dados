# Diseño: primera pantalla de partida en `:app-mobile`

Fecha: 2026-07-31. Conecta la UI de celular a `MotorPartida` (`:core`) por primera vez.

## Objetivo

Una sola pantalla vertical, jugable de punta a punta con el preset "Clásico" y un jugador: lanzar, retener dados, anotar, ver el puntaje total con bonus. Diseño visual sobrio (Material 3 por defecto), sin identidad visual definida. Nada de 3D.

## Regla no negociable

El ViewModel sostiene el `EstadoPartida`, traduce toques en `Accion` y guarda el `EstadoPartida` que devuelve `motor.aplicar()`. No decide legalidad, no calcula puntajes, no cuenta lanzamientos, no determina fin de partida. Si se borra el ViewModel, no se pierde ninguna regla del juego — toda regla vive en `:core`.

## 1. Cambios en `:core` (paquete `motor/`)

### `MotorPartida.puntajeTotal(jugador): Int` → `puntaje(jugador): Puntaje`

```kotlin
data class Puntaje(
    val subtotalSuperior: Int,
    val bonus: Int,
    val subtotalInferior: Int,
    val total: Int
)
```

Un solo llamado le da a la UI el subtotal de sección superior, si el bonus se aplicó (y cuánto), el subtotal inferior y el total. Reemplaza al método existente; se actualiza el test `el total aplica el bonus con subtotal superior 63 y no lo aplica con 62` para usar `puntaje(jugador).total` (y puede sumar aserciones sobre `subtotalSuperior`/`bonus` directamente).

### Nuevo: `MotorPartida.puntajeSiSeAnotara(estado, categoriaId): Int?`

Aplica `esValida`/`puntaje` de la categoría sobre `estado.tiradaActual` sin mutar nada — es una consulta, no una acción.

- `estado.tiradaActual == null` → `null` (no hay dados sobre los que calcular).
- `categoriaId` no existe en el `RuleSet` → `null`.
- Tirada no califica → `0` (el sacrificio es un valor real, no una ausencia).
- Tirada califica → `categoria.puntaje(tirada)`.

`anotar()` internamente se reescribe para usar esta misma función (una sola fuente de verdad para "cuánto vale anotar aquí ahora").

Ambos cambios llevan tests en `:core` escritos antes que la implementación, seads con `Random` fija donde corresponda — mismo patrón que el resto del módulo.

## 2. `PartidaViewModel` (`:app-mobile`)

```kotlin
class PartidaViewModel : ViewModel() {
    private val ruleSet = presetClasico()
    private val motor = MotorPartida(ruleSet, Random())
    // StateFlow<PartidaUiState>, estado inicial: 1 jugador, indiceTurno=0, tiradaActual=null
    fun despachar(accion: Accion) { /* motor.aplicar(estadoActual, accion) */ }
}
```

- Sin factory: constructor sin argumentos, `viewModel()` de Compose lo instancia solo.
- `despachar` es el único punto de entrada de eventos. `Exito` reemplaza el estado guardado; `Rechazada` no hace nada. En operación normal `Rechazada` nunca debería ocurrir porque la UI solo ofrece acciones que ya están en `accionesLegales` — es una red de seguridad, no una decisión.
- Cada vez que el estado cambia, se recalculan `accionesLegales` y `puntaje` pidiéndoselos al motor.

```kotlin
data class PartidaUiState(
    val estado: EstadoPartida,
    val accionesLegales: List<Accion>,
    val puntaje: Puntaje
)
```

## 3. Nombres de categoría (`:app-mobile`, no en `:core`)

```kotlin
fun nombreDeCategoria(id: CategoriaId): String
```

`when` exhaustivo sobre los 12 ids exactos de `PresetClasico` (`ones`, `twos`, ..., `yacht`). El orden de las filas de la tabla sale de iterar `ruleSet.categorias` (vía el estado/puntaje que ya tiene el ViewModel), nunca de una lista propia en la UI — si el `RuleSet` cambiara de orden o cantidad, la tabla lo sigue automáticamente.

## 4. Tabla de categorías — especificación exacta de previsualización

Por cada categoría del `RuleSet`, en orden:

| Situación | Qué se muestra | Estilo | Interactiva |
|---|---|---|---|
| Ya anotada por el jugador (`jugador.anotaciones[id]` existe) | El puntaje obtenido | Color sólido, más peso visual | No |
| Libre, con tirada en curso, tirada no califica | `0` | Color atenuado | Sí |
| Libre, con tirada en curso, tirada califica | El puntaje que se obtendría | Color atenuado | Sí |
| Libre, sin tirada en curso (turno recién iniciado) | Nada — celda vacía | — | No |

El `0` de "no califica" se muestra explícitamente (no se deja la celda vacía): comunica "aquí sacrificas" y evita que el jugador piense que hay un error.

"Interactiva" = `Accion.Anotar(id) in accionesLegales` (ya calculado por el motor; la fila no vuelve a decidir esto). Al tocar una fila interactiva se despacha `Accion.Anotar(id)`; el estado nuevo trae la categoría en `anotaciones`, así que en el siguiente render la misma fila pasa automáticamente de atenuada a sólida — no hace falta lógica de transición aparte.

Fuente de cada valor mostrado:

- Ya anotada → `jugador.anotaciones.getValue(id)` (lectura directa de datos, no es una regla).
- Libre → `motor.puntajeSiSeAnotara(estado, id)`: `null` → celda vacía, `0` o valor real → el número, atenuado.

Ningún puntaje de previsualización se calcula en la UI ni en el ViewModel: siempre viene de `puntajeSiSeAnotara`.

## 5. Composables (`app-mobile/.../partida/PartidaScreen.kt`)

- `PartidaScreen(viewModel: PartidaViewModel = viewModel())` — colecta el `StateFlow` con `collectAsState()`, arma la columna vertical.
- `FilaDados(dados, accionesLegales, onTocarDado)` — 5 dados en `Row`. Un dado es tocable si `Accion.AlternarRetencion(indice)` está en `accionesLegales`; al tocarlo despacha esa acción.
- `Dado(valor, retenido)` — `Canvas` cuadrado; fondo distinto si `retenido`; puntos dibujados a mano según el valor (layouts estándar 1–6). Sin imágenes.
- `BotonLanzar(habilitado, onClick)` — habilitado solo si `Accion.Lanzar` está en `accionesLegales`; despacha `Accion.Lanzar`.
- `TablaCategorias(ruleSet, estado, accionesLegales, onAnotar)` — 12 filas según la tabla de la sección 4.
- `ResumenPuntaje(puntaje: Puntaje)` — subtotal superior, indicador de bonus (si `bonus > 0`), total.
- Indicador de lanzamientos: `"${estado.tiradaActual?.lanzamientos ?: 0} / 3"`.
- `MainActivity` muestra `PartidaScreen()` en vez del `Greeting` de la plantilla.

## 6. Dependencias nuevas

`androidx.lifecycle:lifecycle-viewmodel-compose` (trae `ViewModel` y la función `viewModel()`), agregada a `gradle/libs.versions.toml` y `app-mobile/build.gradle.kts`.

## 7. Testing

- `:core`: tests de `Puntaje`/`puntaje()` y de `puntajeSiSeAnotara` (tirada válida, inválida, sin tirada, categoría inexistente), escritos antes que la implementación.
- `:app-mobile`: tests unitarios JVM de `PartidaViewModel` (sin Robolectric) verificando que expone `accionesLegales`/`puntaje` tal como los devuelve un `MotorPartida` real con `Random` sembrado, y que `despachar` se limita a pasar la acción y guardar el resultado — nunca decide nada por su cuenta.
- Sin tests de Compose UI en esta tarea.

## 8. Fuera de alcance (explícito)

- Persistencia de la partida en curso.
- Pantalla de fin de partida dedicada (con `accionesLegales` vacío el tablero queda todo deshabilitado; no hay pantalla nueva).
- Multijugador visible (el modelo ya lo soporta; esta pantalla usa 1 jugador).
- Identidad visual, háptica, sonido, gesto de sacudir.
