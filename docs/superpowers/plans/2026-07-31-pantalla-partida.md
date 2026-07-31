# Pantalla de partida en :app-mobile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Conectar `:app-mobile` a `MotorPartida` con una primera pantalla Compose jugable de punta a punta (lanzar, retener dados, anotar, ver puntaje con bonus) usando el preset "Clásico" y un jugador.

**Architecture:** `PartidaViewModel` sostiene `EstadoPartida` en memoria, recibe un `MotorPartida` inyectado (por defecto uno real con `presetClasico()` y `Random.Default`), y traduce cada toque de la UI en un `Accion` despachado a `motor.aplicar()`. Ninguna regla de juego vive en el ViewModel ni en los composables: legalidad, puntajes, conteo de lanzamientos y fin de partida los resuelve `MotorPartida`. Tres consultas nuevas en `:core` (`puntaje`, `puntajeSiSeAnotara`, `categorias`) le dan a la UI todo lo que necesita sin que decida nada.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose (Material 3), `androidx.lifecycle:lifecycle-viewmodel-compose`, JUnit4, `kotlin.random.Random`.

## Global Constraints

- `:core` es Kotlin puro: prohibido `android.*` o Compose (CLAUDE.md).
- Ninguna regla del juego en el ViewModel ni en composables — todo pedido a `MotorPartida`. Si se borra el ViewModel, no se pierde ninguna regla del juego (restricción explícita de esta tarea).
- Comentarios en español en todo el código nuevo (CLAUDE.md).
- Nombres de jugador neutros en datos de prueba — nunca el nombre real del autor (CLAUDE.md). Usar `"Jugador 1"`.
- Sin constantes que dupliquen lo que ya deriva el `RuleSet` o `Tirada` — usar `Tirada.MAXIMO_DE_LANZAMIENTOS` / `Tirada.CANTIDAD_DE_DADOS`, no hardcodear `3` o `5`.
- Aleatoriedad inyectable; tests deterministas con semilla fija, calculando valores esperados con un `Random` espejo de la misma semilla en vez de asumir el algoritmo interno (patrón ya usado en `MotorPartidaTest`).
- Los rechazos se reportan como valores (`MotivoRechazo`), nunca con excepciones ni texto en `:core`.
- **Entorno de esta máquina Windows:** la carpeta de usuario tiene espacio y tilde (`C:\Users\Ignacio Díaz`), lo que rompe el classpath del worker de test de Gradle. **Todo comando `gradlew` de este plan debe ejecutarse así:**
  ```
  JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew <tarea> --console=plain
  ```
- Hay un emulador Android ya corriendo (`emulator-5554`, AVD `Pixel_8`); no hace falta crear ni levantar uno nuevo.
- `Categoria` (en `:core`) NO es `data class`; su igualdad es por `id`. `CategoriaId` es un `value class` sobre `String` — un `when` sobre un `CategoriaId` no puede ser exhaustivo para el compilador, así que cualquier mapeo por id se hace con `when (idOrValor.valor) { ... else -> ... }`, nunca `when (categoriaId) { ... }`.

---

### Task 1: `:core` — `Puntaje` y `MotorPartida.puntaje(jugador)`

**Files:**
- Create: `core/src/main/kotlin/cl/ignaciodiaz/dados/core/motor/Puntaje.kt`
- Modify: `core/src/main/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartida.kt:46-62` (función `puntajeTotal`)
- Modify: `core/src/test/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartidaTest.kt:288-310` (test `el total aplica el bonus...`)

**Interfaces:**
- Consumes: `RuleSet`, `Jugador`, `Seccion` (ya existen en `cl.ignaciodiaz.dados.core.modelo`).
- Produces: `data class Puntaje(val subtotalSuperior: Int, val bonus: Int, val subtotalInferior: Int, val total: Int)` y `MotorPartida.puntaje(jugador: Jugador): Puntaje`, que Task 3 usa para `PartidaUiState.puntaje`.

- [ ] **Step 1: Modificar el test existente para que use `puntaje()` en vez de `puntajeTotal()`**

En `core/src/test/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartidaTest.kt`, reemplazar el cuerpo del test `el total aplica el bonus con subtotal superior 63 y no lo aplica con 62` (líneas 289-310) completo por:

```kotlin
    @Test
    fun `el total aplica el bonus con subtotal superior 63 y no lo aplica con 62`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))

        val jugadorConBonus = Jugador(
            nombre = "Jugador 1",
            anotaciones = mapOf(
                CategoriaId("ones") to 3,
                CategoriaId("twos") to 6,
                CategoriaId("threes") to 9,
                CategoriaId("fours") to 12,
                CategoriaId("fives") to 15,
                CategoriaId("sixes") to 18
            )
        )
        val jugadorSinBonus = jugadorConBonus.copy(
            anotaciones = jugadorConBonus.anotaciones + (CategoriaId("ones") to 2)
        )

        val puntajeConBonus = motor.puntaje(jugadorConBonus)
        assertEquals(63, puntajeConBonus.subtotalSuperior)
        assertEquals(35, puntajeConBonus.bonus)
        assertEquals(0, puntajeConBonus.subtotalInferior)
        assertEquals(98, puntajeConBonus.total)

        val puntajeSinBonus = motor.puntaje(jugadorSinBonus)
        assertEquals(62, puntajeSinBonus.subtotalSuperior)
        assertEquals(0, puntajeSinBonus.bonus)
        assertEquals(62, puntajeSinBonus.total)
    }
```

- [ ] **Step 2: Ejecutar los tests de `:core` y verificar que este falla en compilación**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :core:test --console=plain`
Expected: `FAILURE` — `compileTestKotlin` con `Unresolved reference 'puntaje'`.

- [ ] **Step 3: Crear `Puntaje.kt`**

```kotlin
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
```

- [ ] **Step 4: Reemplazar `puntajeTotal` por `puntaje` en `MotorPartida.kt`**

Reemplazar el bloque completo (líneas 46-62 del archivo actual):

```kotlin
    // Suma de sección superior (más el bonus si alcanza el umbral) más la suma de
    // sección inferior. Vive acá porque el motor es quien conoce el RuleSet y por
    // lo tanto la sección de cada categoría.
    fun puntajeTotal(jugador: Jugador): Int {
        val categoriaPorId = ruleSet.categorias.associateBy { it.id }
        val subtotalSuperior = jugador.anotaciones
            .filterKeys { categoriaPorId.getValue(it).seccion == Seccion.SUPERIOR }
            .values.sum()
        val subtotalInferior = jugador.anotaciones
            .filterKeys { categoriaPorId.getValue(it).seccion == Seccion.INFERIOR }
            .values.sum()
        val bonus = ruleSet.bonusSeccionSuperior
            ?.takeIf { subtotalSuperior >= it.umbral }
            ?.valor
            ?: 0
        return subtotalSuperior + bonus + subtotalInferior
    }
```

por:

```kotlin
    // Desglose del puntaje de un jugador: subtotal de sección superior, bonus si
    // alcanza el umbral, subtotal de sección inferior y total. Vive acá porque el
    // motor es quien conoce el RuleSet y por lo tanto la sección de cada categoría.
    fun puntaje(jugador: Jugador): Puntaje {
        val categoriaPorId = ruleSet.categorias.associateBy { it.id }
        val subtotalSuperior = jugador.anotaciones
            .filterKeys { categoriaPorId.getValue(it).seccion == Seccion.SUPERIOR }
            .values.sum()
        val subtotalInferior = jugador.anotaciones
            .filterKeys { categoriaPorId.getValue(it).seccion == Seccion.INFERIOR }
            .values.sum()
        val bonus = ruleSet.bonusSeccionSuperior
            ?.takeIf { subtotalSuperior >= it.umbral }
            ?.valor
            ?: 0
        return Puntaje(
            subtotalSuperior = subtotalSuperior,
            bonus = bonus,
            subtotalInferior = subtotalInferior,
            total = subtotalSuperior + bonus + subtotalInferior
        )
    }
```

- [ ] **Step 5: Ejecutar los tests de `:core` y verificar que pasan**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :core:test --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add core/src/main/kotlin/cl/ignaciodiaz/dados/core/motor/Puntaje.kt core/src/main/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartida.kt core/src/test/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartidaTest.kt
git commit -m "Reemplaza MotorPartida.puntajeTotal por puntaje(jugador): Puntaje

Un solo llamado le da a la UI subtotal superior, bonus, subtotal inferior
y total, en vez de que arme el resumen con multiples numeros sueltos."
```

---

### Task 2: `:core` — `MotorPartida.categorias` y `puntajeSiSeAnotara`

**Files:**
- Modify: `core/src/main/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartida.kt`
- Modify: `core/src/test/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartidaTest.kt`

**Interfaces:**
- Consumes: `Puntaje`/`puntaje()` de Task 1; `CategoriaId`, `EstadoPartida`, `Tirada`, `Dado` de `cl.ignaciodiaz.dados.core.modelo`.
- Produces: `MotorPartida.categorias: List<CategoriaId>` y `MotorPartida.puntajeSiSeAnotara(estado: EstadoPartida, categoriaId: CategoriaId): Int?`, que Task 3 usa para armar `PartidaUiState` (orden de la tabla y previsualización de puntaje).

- [ ] **Step 1: Escribir los tests que fallan**

En `core/src/test/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartidaTest.kt`, agregar estos tests después del test `el total aplica el bonus...` (antes de la sección `// --- accionesLegales ---`):

```kotlin
    // --- categorias ---

    @Test
    fun `categorias expone los ids del RuleSet en su orden`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))

        val idsEsperados = listOf(
            "ones", "twos", "threes", "fours", "fives", "sixes",
            "choice", "four_dice", "full_house", "small_straight", "big_straight", "yacht"
        ).map { CategoriaId(it) }

        assertEquals(idsEsperados, motor.categorias)
    }

    // --- puntajeSiSeAnotara ---

    @Test
    fun `puntaje si se anotara es nulo sin tirada en curso`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))

        assertNull(motor.puntajeSiSeAnotara(estadoInicial(1), CategoriaId("yacht")))
    }

    @Test
    fun `puntaje si se anotara es nulo para una categoria desconocida`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaDePrueba()
        )

        assertNull(motor.puntajeSiSeAnotara(estado, CategoriaId("no_existe")))
    }

    @Test
    fun `puntaje si se anotara es 0 cuando la tirada no califica (sacrificio)`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val tiradaQueNoEsYacht = Tirada(listOf(Dado(1), Dado(2), Dado(3), Dado(4), Dado(6)), lanzamientos = 1)
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaQueNoEsYacht
        )

        assertEquals(0, motor.puntajeSiSeAnotara(estado, CategoriaId("yacht")))
    }

    @Test
    fun `puntaje si se anotara es el puntaje real cuando la tirada califica`() {
        val ruleSet = presetClasico()
        val motor = MotorPartida(ruleSet, Random(SEMILLA))
        val tiradaYacht = Tirada(List(5) { Dado(4) }, lanzamientos = 1)
        val estado = EstadoPartida(
            jugadores = listOf(Jugador("Jugador 1")),
            indiceTurno = 0,
            tiradaActual = tiradaYacht
        )

        assertEquals(50, motor.puntajeSiSeAnotara(estado, CategoriaId("yacht")))
    }
```

- [ ] **Step 2: Ejecutar los tests y verificar que fallan en compilación**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :core:test --console=plain`
Expected: `FAILURE` — `Unresolved reference 'categorias'` y `'puntajeSiSeAnotara'`.

- [ ] **Step 3: Implementar `categorias` y `puntajeSiSeAnotara`, y refactorizar `anotar` para reusarlo**

En `MotorPartida.kt`, agregar esta propiedad justo después de la línea `) {` que abre la clase (antes de `fun aplicar`):

```kotlin
    // Los ids de las categorías del RuleSet, en orden. Le permite a la UI dibujar la
    // tabla de categorías sin mantener su propia lista ni conocer el RuleSet directamente.
    val categorias: List<CategoriaId> = ruleSet.categorias.map { it.id }

```

Agregar esta función pública inmediatamente después de `puntaje(jugador: Jugador): Puntaje` (la de Task 1):

```kotlin

    // Cuánto valdría anotar en esa categoría ahora mismo, sin aplicar nada: null si no
    // hay tirada en curso o la categoría no existe, 0 si la tirada no califica (el
    // sacrificio es un valor real, no una ausencia), o el puntaje si la tirada califica.
    fun puntajeSiSeAnotara(estado: EstadoPartida, categoriaId: CategoriaId): Int? {
        val tiradaActual = estado.tiradaActual ?: return null
        val categoria = ruleSet.categorias.find { it.id == categoriaId } ?: return null
        return if (categoria.esValida(tiradaActual)) categoria.puntaje(tiradaActual) else 0
    }
```

Reemplazar la función privada `anotar` (al final de la clase) completa:

```kotlin
    private fun anotar(estado: EstadoPartida, categoriaId: CategoriaId): EstadoPartida {
        val tiradaActual = estado.tiradaActual!!
        val categoria = ruleSet.categorias.first { it.id == categoriaId }
        val puntaje = if (categoria.esValida(tiradaActual)) categoria.puntaje(tiradaActual) else 0

        val jugadorActual = estado.jugadorEnTurno
        val jugadorAnotado = jugadorActual.copy(
            anotaciones = jugadorActual.anotaciones + (categoriaId to puntaje)
        )
        val jugadores = estado.jugadores.mapIndexed { i, jugador ->
            if (i == estado.indiceTurno) jugadorAnotado else jugador
        }

        return estado.copy(
            jugadores = jugadores,
            indiceTurno = (estado.indiceTurno + 1) % jugadores.size,
            tiradaActual = null
        )
    }
```

por:

```kotlin
    private fun anotar(estado: EstadoPartida, categoriaId: CategoriaId): EstadoPartida {
        // motivoRechazo() ya garantizó, antes de llegar acá, que hay tirada en curso
        // y que la categoría existe: el resultado nunca es null en este punto.
        val puntaje = puntajeSiSeAnotara(estado, categoriaId)!!

        val jugadorActual = estado.jugadorEnTurno
        val jugadorAnotado = jugadorActual.copy(
            anotaciones = jugadorActual.anotaciones + (categoriaId to puntaje)
        )
        val jugadores = estado.jugadores.mapIndexed { i, jugador ->
            if (i == estado.indiceTurno) jugadorAnotado else jugador
        }

        return estado.copy(
            jugadores = jugadores,
            indiceTurno = (estado.indiceTurno + 1) % jugadores.size,
            tiradaActual = null
        )
    }
```

- [ ] **Step 4: Ejecutar todos los tests de `:core` y verificar que pasan**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :core:test --console=plain`
Expected: `BUILD SUCCESSFUL`. Confirmar en particular que `anotar en una categoria cuya tirada no califica anota 0 y es legal (sacrificio)` (test ya existente) sigue pasando: verifica que el refactor no rompió el comportamiento real de `Anotar`.

- [ ] **Step 5: Commit**

```bash
git add core/src/main/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartida.kt core/src/test/kotlin/cl/ignaciodiaz/dados/core/motor/MotorPartidaTest.kt
git commit -m "Agrega MotorPartida.categorias y puntajeSiSeAnotara para la previsualizacion de la UI

anotar() se reescribe para reusar puntajeSiSeAnotara: una sola fuente de
verdad para 'cuanto vale anotar aqui ahora', en vez de duplicar la logica
de esValida/puntaje en dos lugares."
```

---

### Task 3: `:app-mobile` — `PartidaUiState` y `PartidaViewModel`

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app-mobile/build.gradle.kts`
- Create: `app-mobile/src/main/java/cl/ignaciodiaz/dados/partida/PartidaUiState.kt`
- Create: `app-mobile/src/main/java/cl/ignaciodiaz/dados/partida/PartidaViewModel.kt`
- Test: `app-mobile/src/test/java/cl/ignaciodiaz/dados/partida/PartidaViewModelTest.kt`

**Interfaces:**
- Consumes: `MotorPartida`, `Puntaje`, `Accion`, `ResultadoAccion` (`cl.ignaciodiaz.dados.core.motor`); `EstadoPartida`, `Jugador`, `CategoriaId` (`cl.ignaciodiaz.dados.core.modelo`); `presetClasico()` (`cl.ignaciodiaz.dados.core.reglas`).
- Produces: `data class PartidaUiState(estado: EstadoPartida, accionesLegales: List<Accion>, puntaje: Puntaje, previsualizaciones: Map<CategoriaId, Int?>)`; `class PartidaViewModel(motor: MotorPartida = ...) : ViewModel()` con `val categorias: List<CategoriaId>`, `val uiState: StateFlow<PartidaUiState>`, `fun despachar(accion: Accion)`. Task 5 los usa para armar la pantalla.

- [ ] **Step 1: Agregar la dependencia `lifecycle-viewmodel-compose`**

En `gradle/libs.versions.toml`, en la sección `[libraries]`, agregar esta línea justo después de `androidx-lifecycle-runtime-ktx`:

```toml
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
```

En `app-mobile/build.gradle.kts`, en el bloque `dependencies`, agregar esta línea justo después de `implementation(libs.androidx.lifecycle.runtime.ktx)`:

```kotlin
    implementation(libs.androidx.lifecycle.viewmodel.compose)
```

- [ ] **Step 2: Escribir el test del ViewModel que falla**

Crear `app-mobile/src/test/java/cl/ignaciodiaz/dados/partida/PartidaViewModelTest.kt`:

```kotlin
package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.motor.ResultadoAccion
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

private const val SEMILLA = 7

class PartidaViewModelTest {

    @Test
    fun `el estado inicial trae un jugador, sin tirada, y la unica accion legal es lanzar`() {
        val viewModel = PartidaViewModel(MotorPartida(presetClasico(), Random(SEMILLA)))

        val uiState = viewModel.uiState.value

        assertEquals(1, uiState.estado.jugadores.size)
        assertNull(uiState.estado.tiradaActual)
        assertEquals(listOf(Accion.Lanzar), uiState.accionesLegales)
    }

    @Test
    fun `despachar Lanzar guarda exactamente el estado y las acciones legales que devuelve el motor`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val motorEspejo = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(motor)
        val estadoInicial = viewModel.uiState.value.estado

        viewModel.despachar(Accion.Lanzar)

        val esperado = motorEspejo.aplicar(estadoInicial, Accion.Lanzar)
        check(esperado is ResultadoAccion.Exito)
        assertEquals(esperado.estado, viewModel.uiState.value.estado)
        assertEquals(motorEspejo.accionesLegales(esperado.estado), viewModel.uiState.value.accionesLegales)
    }

    @Test
    fun `despachar una accion rechazada no modifica el estado`() {
        val viewModel = PartidaViewModel(MotorPartida(presetClasico(), Random(SEMILLA)))
        val estadoAntes = viewModel.uiState.value

        // AlternarRetencion es ilegal sin tirada en curso: el turno recien empieza.
        viewModel.despachar(Accion.AlternarRetencion(0))

        assertEquals(estadoAntes, viewModel.uiState.value)
    }

    @Test
    fun `el puntaje expuesto es el que devuelve motor puntaje para el jugador en turno`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(motor)

        val esperado = motor.puntaje(viewModel.uiState.value.estado.jugadorEnTurno)

        assertEquals(esperado, viewModel.uiState.value.puntaje)
    }

    @Test
    fun `las previsualizaciones vienen de motor puntajeSiSeAnotara para cada categoria`() {
        val motor = MotorPartida(presetClasico(), Random(SEMILLA))
        val viewModel = PartidaViewModel(motor)
        val estado = viewModel.uiState.value.estado

        val esperadas = motor.categorias.associateWith { id -> motor.puntajeSiSeAnotara(estado, id) }

        assertEquals(esperadas, viewModel.uiState.value.previsualizaciones)
    }
}
```

- [ ] **Step 3: Ejecutar los tests de `:app-mobile` y verificar que fallan en compilación**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :app-mobile:testDebugUnitTest --console=plain`
Expected: `FAILURE` — `Unresolved reference 'PartidaViewModel'`.

- [ ] **Step 4: Implementar `PartidaUiState.kt`**

```kotlin
package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.Puntaje

// Lo que la pantalla necesita para dibujarse: el estado de la partida, qué acciones
// el motor aceptaría ahora mismo, el desglose de puntaje del jugador en turno y la
// previsualización de puntaje de cada categoría libre. Todo pedido al motor:
// PartidaUiState no decide nada, solo empaqueta lo que devuelve.
data class PartidaUiState(
    val estado: EstadoPartida,
    val accionesLegales: List<Accion>,
    val puntaje: Puntaje,
    val previsualizaciones: Map<CategoriaId, Int?>
)
```

- [ ] **Step 5: Implementar `PartidaViewModel.kt`**

```kotlin
package cl.ignaciodiaz.dados.partida

import androidx.lifecycle.ViewModel
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.EstadoPartida
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.MotorPartida
import cl.ignaciodiaz.dados.core.motor.ResultadoAccion
import cl.ignaciodiaz.dados.core.reglas.presetClasico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

// Sostiene el EstadoPartida de una partida de un jugador y traduce toques en Accion.
// No decide legalidad, no calcula puntajes, no cuenta lanzamientos, no determina fin
// de partida: todo eso lo responde MotorPartida. Si se borra esta clase, no se pierde
// ninguna regla del juego.
//
// El motor se recibe por constructor con valor por defecto: Kotlin genera un
// constructor sin argumentos cuando todos los parámetros lo tienen, así que
// viewModel() lo sigue instanciando solo, y los tests pueden inyectar un motor con
// Random(semilla) para ser deterministas.
class PartidaViewModel(
    private val motor: MotorPartida = MotorPartida(presetClasico(), Random.Default)
) : ViewModel() {

    // Ids de categoría en el orden del RuleSet. No cambia durante la vida del
    // ViewModel: no hace falta que viaje dentro del StateFlow.
    val categorias: List<CategoriaId> = motor.categorias

    private val estadoInicial = EstadoPartida(
        jugadores = listOf(Jugador(nombre = "Jugador 1")),
        indiceTurno = 0,
        tiradaActual = null
    )

    private val _uiState = MutableStateFlow(construirUiState(estadoInicial))
    val uiState: StateFlow<PartidaUiState> = _uiState.asStateFlow()

    fun despachar(accion: Accion) {
        when (val resultado = motor.aplicar(_uiState.value.estado, accion)) {
            is ResultadoAccion.Exito -> _uiState.value = construirUiState(resultado.estado)
            is ResultadoAccion.Rechazada -> Unit
        }
    }

    private fun construirUiState(estado: EstadoPartida) = PartidaUiState(
        estado = estado,
        accionesLegales = motor.accionesLegales(estado),
        puntaje = motor.puntaje(estado.jugadorEnTurno),
        previsualizaciones = categorias.associateWith { id -> motor.puntajeSiSeAnotara(estado, id) }
    )
}
```

- [ ] **Step 6: Ejecutar los tests de `:app-mobile` y verificar que pasan**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :app-mobile:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add gradle/libs.versions.toml app-mobile/build.gradle.kts app-mobile/src/main/java/cl/ignaciodiaz/dados/partida/PartidaUiState.kt app-mobile/src/main/java/cl/ignaciodiaz/dados/partida/PartidaViewModel.kt app-mobile/src/test/java/cl/ignaciodiaz/dados/partida/PartidaViewModelTest.kt
git commit -m "Agrega PartidaViewModel: sostiene EstadoPartida y despacha Accion a MotorPartida

El ViewModel no decide ninguna regla; solo pide accionesLegales, puntaje y
puntajeSiSeAnotara al motor y empaqueta el resultado en PartidaUiState."
```

---

### Task 4: `:app-mobile` — nombres de categoría en español

**Files:**
- Create: `app-mobile/src/main/java/cl/ignaciodiaz/dados/partida/CategoriaTexto.kt`
- Test: `app-mobile/src/test/java/cl/ignaciodiaz/dados/partida/CategoriaTextoTest.kt`

**Interfaces:**
- Consumes: `CategoriaId` (`cl.ignaciodiaz.dados.core.modelo`).
- Produces: `fun nombreDeCategoria(id: CategoriaId): String`, que Task 5 usa en cada fila de la tabla.

- [ ] **Step 1: Escribir el test que falla**

Crear `app-mobile/src/test/java/cl/ignaciodiaz/dados/partida/CategoriaTextoTest.kt`:

```kotlin
package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoriaTextoTest {

    @Test
    fun `traduce los 12 ids del preset Clasico a nombres en espanol`() {
        val esperados = mapOf(
            "ones" to "Unos",
            "twos" to "Doses",
            "threes" to "Treses",
            "fours" to "Cuatros",
            "fives" to "Cincos",
            "sixes" to "Seises",
            "choice" to "Choice",
            "four_dice" to "Four Dice",
            "full_house" to "Full House",
            "small_straight" to "Escalera chica",
            "big_straight" to "Escalera grande",
            "yacht" to "Yacht"
        )

        esperados.forEach { (id, nombre) ->
            assertEquals(nombre, nombreDeCategoria(CategoriaId(id)))
        }
    }

    @Test
    fun `un id desconocido devuelve el texto crudo en vez de lanzar`() {
        assertEquals("categoria_experimental", nombreDeCategoria(CategoriaId("categoria_experimental")))
    }
}
```

- [ ] **Step 2: Ejecutar el test y verificar que falla en compilación**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :app-mobile:testDebugUnitTest --console=plain`
Expected: `FAILURE` — `Unresolved reference 'nombreDeCategoria'`.

- [ ] **Step 3: Implementar `CategoriaTexto.kt`**

```kotlin
package cl.ignaciodiaz.dados.partida

import cl.ignaciodiaz.dados.core.modelo.CategoriaId

// Nombres visibles de las 12 categorías del preset "Clásico". :core no conoce texto
// de interfaz (CLAUDE.md); este mapeo vive acá. CategoriaId es un value class sobre
// String, así que el when no puede ser exhaustivo para el compilador: el else
// devuelve el id crudo (sin traducir) en vez de lanzar. Si un preset futuro trae una
// categoría sin nombre mapeado, se ve fea en pantalla en vez de cerrar la app.
fun nombreDeCategoria(id: CategoriaId): String = when (id.valor) {
    "ones" -> "Unos"
    "twos" -> "Doses"
    "threes" -> "Treses"
    "fours" -> "Cuatros"
    "fives" -> "Cincos"
    "sixes" -> "Seises"
    "choice" -> "Choice"
    "four_dice" -> "Four Dice"
    "full_house" -> "Full House"
    "small_straight" -> "Escalera chica"
    "big_straight" -> "Escalera grande"
    "yacht" -> "Yacht"
    else -> id.valor
}
```

- [ ] **Step 4: Ejecutar el test y verificar que pasa**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :app-mobile:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app-mobile/src/main/java/cl/ignaciodiaz/dados/partida/CategoriaTexto.kt app-mobile/src/test/java/cl/ignaciodiaz/dados/partida/CategoriaTextoTest.kt
git commit -m "Agrega el mapeo de nombres de categoria en espanol para la tabla de partida"
```

---

### Task 5: `:app-mobile` — Composables de la pantalla y `MainActivity`

**Files:**
- Create: `app-mobile/src/main/java/cl/ignaciodiaz/dados/partida/PartidaScreen.kt`
- Modify: `app-mobile/src/main/java/cl/ignaciodiaz/dados/MainActivity.kt`

**Interfaces:**
- Consumes: `PartidaViewModel`, `PartidaUiState` (Task 3), `nombreDeCategoria` (Task 4), `Puntaje`, `Accion` (`cl.ignaciodiaz.dados.core.motor`), `Dado`, `Jugador`, `CategoriaId`, `Tirada` (`cl.ignaciodiaz.dados.core.modelo`).
- Produces: `@Composable fun PartidaScreen(viewModel: PartidaViewModel = viewModel(), modifier: Modifier = Modifier)`, usado por `MainActivity`.

Sin tests de Compose UI en esta tarea (decidido en la spec). La verificación es Task 6: compilar, instalar y confirmar visualmente en el emulador.

**Gotcha a tener en cuenta:** el modelo `cl.ignaciodiaz.dados.core.modelo.Dado` y el composable `Dado` de este archivo tienen el mismo nombre simple. Importar el modelo con alias (`import cl.ignaciodiaz.dados.core.modelo.Dado as DadoModelo`) para que no choquen.

- [ ] **Step 1: Crear `PartidaScreen.kt`**

```kotlin
package cl.ignaciodiaz.dados.partida

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.ignaciodiaz.dados.core.modelo.CategoriaId
import cl.ignaciodiaz.dados.core.modelo.Dado as DadoModelo
import cl.ignaciodiaz.dados.core.modelo.Jugador
import cl.ignaciodiaz.dados.core.modelo.Tirada
import cl.ignaciodiaz.dados.core.motor.Accion
import cl.ignaciodiaz.dados.core.motor.Puntaje

@Composable
fun PartidaScreen(viewModel: PartidaViewModel = viewModel(), modifier: Modifier = Modifier) {
    val uiState by viewModel.uiState.collectAsState()
    val jugador = uiState.estado.jugadorEnTurno
    val dadosAMostrar = uiState.estado.tiradaActual?.dados
        ?: List(Tirada.CANTIDAD_DE_DADOS) { DadoModelo(valor = 1) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lanzamientos: ${uiState.estado.tiradaActual?.lanzamientos ?: 0} / ${Tirada.MAXIMO_DE_LANZAMIENTOS}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        FilaDados(
            dados = dadosAMostrar,
            accionesLegales = uiState.accionesLegales,
            onTocarDado = { indice -> viewModel.despachar(Accion.AlternarRetencion(indice)) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        BotonLanzar(
            habilitado = Accion.Lanzar in uiState.accionesLegales,
            onClick = { viewModel.despachar(Accion.Lanzar) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        TablaCategorias(
            categorias = viewModel.categorias,
            jugador = jugador,
            accionesLegales = uiState.accionesLegales,
            previsualizaciones = uiState.previsualizaciones,
            onAnotar = { categoriaId -> viewModel.despachar(Accion.Anotar(categoriaId)) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        ResumenPuntaje(puntaje = uiState.puntaje)
    }
}

@Composable
private fun FilaDados(
    dados: List<DadoModelo>,
    accionesLegales: List<Accion>,
    onTocarDado: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        dados.forEachIndexed { indice, dado ->
            val tocable = Accion.AlternarRetencion(indice) in accionesLegales
            Dado(
                dado = dado,
                tocable = tocable,
                onTocar = { onTocarDado(indice) }
            )
        }
    }
}

@Composable
private fun Dado(
    dado: DadoModelo,
    tocable: Boolean,
    onTocar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorFondo = if (dado.retenido) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val colorPunto = if (dado.retenido) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Canvas(
        modifier = modifier
            .size(56.dp)
            .let { base -> if (tocable) base.clickable(onClick = onTocar) else base }
    ) {
        drawRoundRect(
            color = colorFondo,
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        val centro = size.width / 2f
        val desplazamiento = size.width / 4f
        val radio = size.width / 12f
        val posiciones = when (dado.valor) {
            1 -> listOf(Offset(centro, centro))
            2 -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
            3 -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro, centro),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
            4 -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro + desplazamiento, centro - desplazamiento),
                Offset(centro - desplazamiento, centro + desplazamiento),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
            5 -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro + desplazamiento, centro - desplazamiento),
                Offset(centro, centro),
                Offset(centro - desplazamiento, centro + desplazamiento),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
            else -> listOf(
                Offset(centro - desplazamiento, centro - desplazamiento),
                Offset(centro + desplazamiento, centro - desplazamiento),
                Offset(centro - desplazamiento, centro),
                Offset(centro + desplazamiento, centro),
                Offset(centro - desplazamiento, centro + desplazamiento),
                Offset(centro + desplazamiento, centro + desplazamiento)
            )
        }
        posiciones.forEach { posicion -> drawCircle(color = colorPunto, radius = radio, center = posicion) }
    }
}

@Composable
private fun BotonLanzar(habilitado: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, enabled = habilitado, modifier = modifier) {
        Text("Lanzar")
    }
}

@Composable
private fun TablaCategorias(
    categorias: List<CategoriaId>,
    jugador: Jugador,
    accionesLegales: List<Accion>,
    previsualizaciones: Map<CategoriaId, Int?>,
    onAnotar: (CategoriaId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        categorias.forEach { categoriaId ->
            val anotado = jugador.anotaciones[categoriaId]
            val interactiva = Accion.Anotar(categoriaId) in accionesLegales
            val textoPuntaje = when {
                anotado != null -> anotado.toString()
                else -> previsualizaciones[categoriaId]?.toString() ?: ""
            }
            val colorTexto = if (anotado != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            val pesoTexto = if (anotado != null) FontWeight.Bold else FontWeight.Normal

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { base -> if (interactiva) base.clickable { onAnotar(categoriaId) } else base }
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(text = nombreDeCategoria(categoriaId))
                Text(text = textoPuntaje, color = colorTexto, fontWeight = pesoTexto)
            }
        }
    }
}

@Composable
private fun ResumenPuntaje(puntaje: Puntaje, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text("Subtotal superior: ${puntaje.subtotalSuperior}")
        Text(if (puntaje.bonus > 0) "Bonus: +${puntaje.bonus}" else "Bonus: no alcanzado")
        Text("Total: ${puntaje.total}", fontWeight = FontWeight.Bold)
    }
}
```

- [ ] **Step 2: Reescribir `MainActivity.kt` para mostrar `PartidaScreen`**

Reemplazar el archivo completo `app-mobile/src/main/java/cl/ignaciodiaz/dados/MainActivity.kt` (se elimina el `Greeting`/`GreetingPreview` de la plantilla, ya sin uso) por:

```kotlin
package cl.ignaciodiaz.dados

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import cl.ignaciodiaz.dados.partida.PartidaScreen
import cl.ignaciodiaz.dados.ui.theme.DadosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DadosTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PartidaScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
```

- [ ] **Step 3: Compilar `:app-mobile` y verificar que no hay errores**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :app-mobile:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app-mobile/src/main/java/cl/ignaciodiaz/dados/partida/PartidaScreen.kt app-mobile/src/main/java/cl/ignaciodiaz/dados/MainActivity.kt
git commit -m "Agrega la pantalla de partida: dados en Canvas, tabla de categorias y resumen de puntaje

MainActivity muestra PartidaScreen en vez del Greeting de la plantilla de
Android Studio."
```

---

### Task 6: Compilar, instalar en el emulador y verificar

**Files:** ninguno (solo comandos).

**Interfaces:** ninguna nueva — verificación de extremo a extremo de todo lo anterior.

- [ ] **Step 1: Correr toda la batería de tests (`:core` y `:app-mobile`)**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew test --console=plain`
Expected: `BUILD SUCCESSFUL`, todos los módulos.

- [ ] **Step 2: Confirmar que el emulador está disponible**

Run: `"/c/Android/Sdk/platform-tools/adb.exe" devices`
Expected: al menos un dispositivo en estado `device` (ya hay uno corriendo: `emulator-5554`).

- [ ] **Step 3: Instalar la app en el emulador**

Run: `cd "C:/Proyectos/Dados" && JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" GRADLE_USER_HOME="C:/Gradle/home" ./gradlew :app-mobile:installDebug --console=plain`
Expected: `BUILD SUCCESSFUL`, `Installed on 1 device`.

- [ ] **Step 4: Lanzar la app y verificar visualmente**

Run: `"/c/Android/Sdk/platform-tools/adb.exe" shell am start -n cl.ignaciodiaz.dados/.MainActivity`

Confirmar en el emulador (captura de pantalla si hace falta):
- Se ven 5 dados con puntos dibujados, todos mostrando "1", sin retener.
- El botón "Lanzar" está habilitado; el resto de la tabla no responde al toque todavía.
- Tocar "Lanzar" cambia los 5 dados a valores aleatorios y el indicador pasa a "1 / 3"; algunas filas de la tabla muestran previsualización atenuada (o `0` si no califican).
- Tocar un dado lo retiene (cambia de color) y al relanzar ese dado no cambia de valor.
- Tocar una fila de categoría libre la anota: el valor pasa de atenuado a sólido con más peso, la fila deja de responder al toque, y el resumen de puntaje se actualiza.
- Tras 3 lanzamientos, "Lanzar" se deshabilita solo.

- [ ] **Step 5: Reportar el resultado**

Si algo de la verificación visual falla, volver al task correspondiente, corregir con su propio ciclo de test, y repetir este task desde el Step 1.

---
