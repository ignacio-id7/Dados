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

**Paso 9: instalar el emulador (AVD)** desde el SDK Manager. Es bloqueante: sin él no se puede ejecutar la app. Toda la lógica del MVP ya está implementada y testeada, así que lo único que falta para tener algo jugable es interfaz — y sin emulador no hay dónde mirarla.

Después: primera pantalla en `:app-mobile` (5 dados, botón de lanzar, tabla de 12 filas) conectada al motor mediante un ViewModel, y luego la persistencia de la partida en curso.

**La lógica de `:core` está completa para el MVP.** Modelo, preset "Clásico" y motor, con 68 tests.

Método de trabajo: el diseño se discute en el proyecto de Claude Desktop; la implementación la ejecuta Claude Code; el código se revisa antes de commitear.

Pasos ya completados: 1) entorno instalado, 2) decisiones cerradas, 3) proyecto creado y repositorio en GitHub, 4) módulo `app` renombrado a `app-mobile`, 5) módulo `:core` creado como biblioteca Kotlin JVM pura, 6) modelo de datos, 7) preset "Clásico", 8) `MotorPartida`. 68 tests en verde.

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
