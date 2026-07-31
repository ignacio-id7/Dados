# CLAUDE.md

Reglas del proyecto Dados (juego de dados original, inspirado en la mecánica de dominio público de Yacht; Android). No es documentación: para contexto y decisiones ver `docs/`.

## Restricciones de arquitectura no negociables

- **`:core` es Kotlin puro.** Prohibido importar `android.*`, Compose o cualquier librería de UI. Prohibido referenciar tamaños de pantalla, colores, recursos o strings de interfaz. Prohibido acceso directo a disco, red o preferencias.
- **Ninguna regla del juego vive en un composable ni en un ViewModel.** Toda lógica de juego (dados, retención/relanzamiento, evaluación de categorías, puntajes, bonus, turnos, jugadas legales) va en `:core`. Antes de escribir código nuevo, preguntar: ¿es lógica de juego o es presentación? Lógica → `:core`, sirve para celular y reloj. Presentación → módulo de la app, se implementa una vez por dispositivo.
- **El motor se parametriza con un `RuleSet` inmutable.** El motor recorre `ruleSet.categorias`, nunca un enum fijo, y no hardcodea puntajes ni el conjunto de categorías. No agregar atajos que asuman el preset "Clásico" a nivel de motor.
- **Todo el código se comenta en español.**
- **Datos personales solo donde signifiquen autoría.** El identificador `cl.ignaciodiaz.dados` es autoría deliberada. Fuera de eso, prohibido usar el nombre real del autor en datos de prueba, nombres de jugador por defecto, valores de ejemplo, comentarios o cadenas de la interfaz: usar nombres neutros (`Jugador 1`, `Jugadora`, `test`). Tampoco incluir rutas locales con el nombre de usuario de Windows en código o recursos.
- **Generación aleatoria inyectable** en `:core` (necesaria para tests con semilla fija y para el futuro modo desafío diario).
- **`EstadoPartida` modela una lista de jugadores** e índice de turno desde el primer commit, aunque el MVP use un solo jugador. No colapsar el modelo a un solo jugador "para simplificar".

Violar estas reglas "para ir más rápido" obliga a reescribir en la Fase B (Wear OS), que reutiliza `:core` sin reescribir lógica.

## Entorno de compilación — leer antes de ejecutar Gradle

- **El comando de verificación del proyecto es `./gradlew test`, sin prefijos.** Corre los tests de todos los módulos.
- **`JAVA_HOME` y `GRADLE_USER_HOME` ya están definidos en el perfil de usuario de Windows** (`C:\Program Files\Android\Android Studio\jbr` y `C:\Gradle`). **Prohibido anteponerlas al comando.** Hacerlo apunta Gradle a una caché distinta y rompe el classpath del proceso de tests con un `ClassNotFoundException: GradleWorkerMain`.
- Para forzar que los tests se ejecuten de verdad y no se salten por estar «up-to-date»: `./gradlew test --rerun-tasks`.
- **Si `./gradlew test` falla, está prohibido rodearlo** ejecutando las clases de test directamente con `java`, con rutas cortas tipo `IGNACI~1` o con cualquier otro atajo. Un rodeo devuelve un verde que no significa nada y deja el proyecto sin señal de verificación. Reportar el fallo y detenerse.

### Síntoma conocido: `ClassNotFoundException: GradleWorkerMain`

Ocurrió el 2026-08-01. Causa: una ejecución con `GRADLE_USER_HOME` incorrecto **guardó un caché de configuración con rutas equivocadas**, y todas las ejecuciones siguientes lo reutilizaban, incluidas las que ya tenían el entorno bien. El error sobrevive a corregir las variables, lo que lo hace parecer un problema de codificación de Windows. No lo es.

Receta de recuperación:

```powershell
.\gradlew --stop
Remove-Item -Recurse -Force .gradle\configuration-cache -ErrorAction SilentlyContinue
.\gradlew test --rerun-tasks
```

## Estado actual de los módulos

- **`app-mobile`** — único módulo existente hoy, plantilla Empty Activity generada por Android Studio (Compose, package `cl.ignaciodiaz.dados`, `minSdk 26`, `targetSdk 36`). Ya renombrado desde `app`.
- **`:core`** — no existe todavía. Próximo paso: crearlo como módulo Kotlin puro (Kotlin JVM library, sin plugin de Android) y declarar la dependencia `:app-mobile → :core`.
- **`:app-wear`** — no existe. Fase B, posterior a que la Fase A esté completa y jugable.

Sin código propio del juego escrito aún. Detalle y siguiente paso exacto en `docs/00-estado-actual.md`.
