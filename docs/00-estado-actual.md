# Estado actual del proyecto — punto de traspaso

> **Léeme primero.** Este archivo es el resumen vivo del proyecto. Se actualiza al cerrar cada sesión.
> Última actualización: 31 de julio de 2026 (tras crear el repositorio, antes de renombrar el módulo).

## Qué es este proyecto

Juego de dados original basado en la mecánica de dominio público de Yacht (5 dados, 3 lanzamientos por turno, 12 categorías). Nombre, arte, textos e identidad visual propios: no se replican assets ni nombres de aplicaciones existentes.

Dos fases, en orden estricto:

- **Fase A — App Android para celular.** Producto principal. Debe estar completa y jugable antes de tocar nada de reloj.
- **Fase B — App Wear OS.** Extensión que reutiliza el módulo `:core` de la Fase A sin reescribir lógica.

## Dónde quedamos

Proyecto base creado en Android Studio y versionado en GitHub. Todas las decisiones que bloqueaban la implementación están cerradas. **Todavía no se ha escrito código propio del juego:** el único código en el repositorio es la plantilla Empty Activity que generó Android Studio. El siguiente movimiento es estructural: renombrar `app` a `app-mobile` y agregar `:core`.

## Qué está funcionando

- **Android Studio Quail 2 | 2026.1.2** (build 2026.1.2.10) + Patch 1, sobre Windows con 24 GB de RAM y ~87 GB libres.
- **SDK en `C:\Android\Sdk`** — fuera del perfil de usuario a propósito: la ruta por defecto contiene un espacio y una tilde (`C:\Users\Ignacio Díaz\...`), lo que rompe herramientas de la cadena de compilación.
- **SDK Platform Android 16 "Baklava" (API 36)** instalado.
- **Proyecto en `C:\Proyectos\Dados`** (ruta sin espacios ni tildes). Plantilla Empty Activity con Compose, package `cl.ignaciodiaz.dados`, `minSdk 26`, Kotlin DSL y catálogo de versiones. Gradle Sync exitoso.
- **Repositorio Git público:** https://github.com/ignacio-id7/Dados. Dos commits: el proyecto inicial y los documentos de diseño en `docs/`.
- **Documentos de diseño en `C:\Proyectos\Dados\docs`**, versionados junto al código. Esta es la carpeta de trabajo conectada en Claude Desktop.
- **Emulador funcionando.** AVD Pixel 8 (1080×2400, 420 dpi) con la imagen `Google APIs Intel x86_64 Atom` de API 36.1. Nunca hubo problema de virtualización: al instalador solo le faltaban el componente Android Emulator y la imagen de sistema, ambos instalados desde el SDK Manager. Se eligió Google APIs en vez de Google Play porque las imágenes con Play Store vienen bloqueadas y estorban al depurar. Decisión de Ignacio (2026-07-31): se prueba en el emulador, no en el celular físico.
- **`JAVA_HOME` = `C:\Program Files\Android\Android Studio\jbr`**, definido en el perfil de usuario. Sin esto, `.\gradlew` desde PowerShell falla: el único JDK del equipo es el embebido en Android Studio y no está registrado a nivel de sistema.
- **`GRADLE_USER_HOME` = `C:\Gradle`**, fuera del perfil de usuario. Sin esto, `.\gradlew test` falla al armar el classpath del proceso de tests, porque el nombre de usuario con tilde se corrompe. Tercera vez que la tilde causa un problema, tras el SDK y la ruta del proyecto: **ante cualquier fallo raro de herramientas en este equipo, sospechar primero de una ruta dentro de `C:\Users\Ignacio Díaz\`.**
- **`.\gradlew test` es el comando de verificación del proyecto.** Corre los tests de todos los módulos. Si algo lo obliga a rodearse con trucos, arreglar el rodeo, no aceptarlo.
- **Claude Code 2.1.220** instalado y autenticado, con Node.js 24 LTS. La ejecución de código pasa a Claude Code; este chat queda para diseño, arquitectura y planificación. `CLAUDE.md` en la raíz le entrega las reglas en cada sesión.

## Estructura actual del repositorio

```
C:\Proyectos\Dados
├── app-mobile\           ← app Android (Compose), depende de :core
├── core\                 ← Kotlin JVM puro, motor del juego (vacío por ahora)
├── CLAUDE.md             ← reglas de arquitectura que lee Claude Code
├── docs\                 ← documentos de diseño (este archivo incluido)
├── gradle\               ← wrapper + catálogo de versiones (libs.versions.toml)
├── build.gradle.kts      ← build raíz
├── settings.gradle.kts   ← declara rootProject.name = "Dados" e include(":app")
├── gradle.properties
└── local.properties      ← NO versionado (contiene la ruta local del SDK)
```

## Qué está pendiente en el entorno

- (Resuelto el 2026-07-31: el emulador ya funciona. Ver "Qué está funcionando").
- **No actualizar a Android Studio Quail 3** mientras siga en Release Candidate. El aviso de update aparece de forma recurrente; ignorarlo.

## Decisiones cerradas

| # | Decisión | Elegido |
|---|---|---|
| 1 | Plataforma | Android: celular (Fase A) → Wear OS (Fase B) |
| 2 | Lenguaje y framework | Kotlin + Jetpack Compose |
| 3 | Estructura de módulos | `:core` (Kotlin puro) + `:app-mobile` + `:app-wear` |
| 4 | Celular de pruebas | Xiaomi Poco F5 Pro, Android 15 |
| 5 | Niveles de API | `minSdk = 26`, `targetSdk = 36` |
| 6 | Reloj de pruebas (Fase B) | OnePlus Watch 4 — redondo, 1.5", 466×466 px, Wear OS 6 |
| 7 | Reglas de puntuación | Preset "Clásico", 12 categorías: bonus 63→+35, Full House y Four Dice = suma de los 5 dados, escaleras 15 / 30, Yacht 50. Máximo 325 |
| 8 | Alcance del MVP (Fase A) | Partida en solitario completa + persistencia + háptica. Nada más |
| 17 | Motor parametrizado | `:core` recibe un `RuleSet` inmutable; el MVP expone un solo preset |
| 19 | Modelo multijugador | `EstadoPartida` con lista de jugadores desde el primer commit; el MVP usa uno solo |
| 20 | Control de versiones | Git + GitHub público (`ignacio-id7/Dados`) |
| 21 | Ubicación de los documentos de diseño | Carpeta `docs/` dentro del repositorio |

Detalle y motivos en `02-decisiones.md`. Reglamento completo y alcance del MVP en `01-especificacion-juego.md`.

## Siguiente paso

## MVP de la Fase A: completo (2026-08-01)

Todo el alcance de la decisión 8 está implementado, testeado y commiteado: menú de inicio, partida de 12 turnos, retención y relanzamiento, anotación obligatoria con sacrificio, tabla con previsualizaciones, bonus, puntaje final, persistencia que sobrevive a que el sistema mate el proceso, y háptica al lanzar.

**Paso 17: cerrar las decisiones que le dan carácter al juego.** No son código:

1. **Decisión 10 — nombre del juego.** Arrastra la decisión 28, el `applicationId` definitivo, que tras publicar en Google Play no se puede cambiar nunca.
2. **Decisión 9 — diferenciador.** El previsto es el gesto de sacudir, pero no está decidido formalmente ni implementado.
3. **Decisión 11 — identidad visual.** Hoy la app es Material 3 por defecto: funcional y anónima.

**Deudas técnicas anotadas:**

- La háptica está testeada en cuanto a que se invoca, pero el emulador no vibra: nadie ha comprobado que efectivamente vibre.
- El gesto de sacudir exige el acelerómetro, que el emulador no simula de forma útil.

Ambas requieren instalar en el Poco F5 Pro, con el obstáculo conocido de HyperOS ("Instalar vía USB", que pide cuenta Mi).

**La Fase B (Wear OS) no ha empezado y no debe empezar hasta cerrar lo anterior.** `:core` está completo y es exactamente lo que la app de reloj reutilizará sin reescribir lógica.

Método de trabajo: el diseño se discute en el proyecto de Claude Desktop; la implementación la ejecuta Claude Code; el código se revisa antes de commitear.

Pasos ya completados: 1) entorno instalado, 2) decisiones cerradas, 3) proyecto creado y repositorio en GitHub, 4) módulo `app` renombrado a `app-mobile`, 5) módulo `:core` creado como biblioteca Kotlin JVM pura, 6) modelo de datos, 7) preset "Clásico", 8) `MotorPartida`, 9) emulador funcionando, 10) primera pantalla jugable conectada al motor.

## Cómo retomar una sesión

1. Abrir Android Studio con el proyecto y esperar el Gradle Sync.
2. Arrancar el AVD Pixel 8 desde **Tools → Device Manager**.
3. En PowerShell: `cd C:\Proyectos\Dados` y luego `claude`, para la ejecución de código.
4. En Claude Desktop, abrir el proyecto "Watch Dice!" para diseño y decisiones. Lee este archivo y `03-bitacora.md` al empezar.
5. Verificar que el repositorio esté limpio: `git status`.

Decisiones abiertas que vienen después: diferenciador, nombre del juego, identidad visual, librería de persistencia, publicación y monetización.

> **Bloqueante antes de publicar (decisión 28):** el `applicationId` es hoy `cl.ignaciodiaz.dados` y **no se puede cambiar una vez publicada la app en Google Play**. Se decide junto con el nombre del juego (decisión 10). Mientras nada esté publicado, renombrarlo cuesta minutos; después, es imposible. No publicar sin haber cerrado esta decisión.

Backlog de reglas (fuera del MVP, ya anotado en `01-especificacion-juego.md`): preset "Moderno" de 13 categorías, pantalla de ajustes pre-partida, bonificación por Yacht múltiple.

## Riesgos anotados

- **Xiaomi / HyperOS:** para instalar la app desde Android Studio por USB no basta con "Depuración USB"; hay que activar además **"Instalar vía USB"**, que exige cuenta Mi y a veces SIM insertada. Aparecerá al conectar el celular por primera vez.
- **Descargas del navegador bloqueadas** en este equipo (quedan en 0 B/s). Alternativa que funciona: `curl.exe` desde PowerShell, con `-C -` para reanudar.
- **`.idea/` versionada.** Se subió al repositorio en el commit inicial. Android Studio la modifica sola: hay cambios sin commitear en `.idea/*`, `gradle/wrapper/gradle-wrapper.properties` y `gradlew.bat`. Si el ruido molesta, se saca del control de versiones más adelante.
- **Google Play:** desde el 31-08-2026 las apps nuevas deben apuntar a API 36. Registro: US$25 pago único + test cerrado con 12 testers por 14 días en cuenta personal. Alternativa gratuita para portafolio: cuentas de distribución limitada (hasta 20 dispositivos), disponibles globalmente desde agosto de 2026. Decisión actual: no registrarse todavía.

## Mapa de archivos

| Archivo | Contenido |
|---|---|
| `00-estado-actual.md` | Este archivo. Resumen vivo y punto de entrada de cada sesión. |
| `01-especificacion-juego.md` | Reglas del juego. Independiente del dispositivo. |
| `02-decisiones.md` | Registro de decisiones con motivos. Nunca se borra una decisión. |
| `03-bitacora.md` | Registro cronológico de sesiones. |
| `04-arquitectura.md` | Contrato del módulo `:core` y reglas de portabilidad. |
| `proyecto-yacht-v2-android-primero.md` | Documento de alcance vigente (v2). |
| `proyecto-yacht-smartwatch-claude-desktop.md` | Documento de alcance original (v1). Superado, se conserva como historial. |
