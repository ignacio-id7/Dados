# Bitácora de desarrollo

Registro cronológico. Se actualiza al final de cada sesión de trabajo.

## Plantilla de entrada

### [FECHA] — [Título de la sesión]

- **Qué se hizo:**
- **Decisiones tomadas:**
- **Problemas encontrados:**
- **Siguiente paso:**

---

### 2026-07-30 — Arranque del proyecto

- **Qué se hizo:** configuración del proyecto en Claude Desktop. Carpeta de trabajo conectada (`Prueba Juego`), instrucciones del proyecto cargadas, creados los tres archivos de conocimiento (01-especificacion-juego, 02-decisiones, 03-bitacora).
- **Decisiones tomadas:** ninguna aún.
- **Problemas encontrados:** —
- **Siguiente paso:** definir plataforma objetivo (depende de qué reloj y computador tiene Ignacio) y alcance del MVP.

---

### 2026-07-30 — Plataforma definida y entorno instalado

- **Qué se hizo:**
  - Cerradas las decisiones 1, 2 y 3 (ver `02-decisiones.md`): plataforma Wear OS 6, stack Kotlin + Compose for Wear OS, dispositivo de referencia OnePlus Watch 4 (redondo, 1.5", 466×466 px).
  - Revisada la estructura de costos: todo el desarrollo es gratuito. Play Store son US$25 pago único (no anual) y exige test cerrado con 12 testers por 14 días en cuenta personal. Alternativa gratuita: cuentas de distribución limitada (hasta 20 dispositivos), disponibles globalmente desde agosto 2026. Decisión: no pagar nada por ahora.
  - Instalado Android Studio Quail 2 | 2026.1.2 (build 2026.1.2.10) en Windows, 24 GB RAM.
  - SDK instalado en `C:\Android\Sdk` en lugar de la ruta por defecto, para evitar el espacio y la tilde de `C:\Users\Ignacio Díaz\...` (rompe herramientas de la cadena de compilación).

- **Decisiones tomadas:** Wear OS + Kotlin/Compose; SDK fuera del perfil de usuario; no registrarse aún en Play Console.

- **Problemas encontrados:**
  - Las descargas del navegador no iniciaban (0 B/s). Solución: descargar con `curl.exe` desde PowerShell. Integridad verificada con SHA-256 contra el hash publicado por Google.
  - La casilla "Android Virtual Device" apareció deshabilitada en el instalador. La virtualización *sí* está habilitada en la BIOS (verificado en Administrador de tareas), así que la causa es un componente de Windows faltante, no hardware. Pendiente de resolver.

- **Siguiente paso:** aplicar el Patch 1 de Quail 2, instalar el emulador de Wear OS desde el SDK Manager y crear el proyecto con la plantilla de Wear OS.

---

### 2026-07-31 — Cambio de alcance: celular primero

- **Qué se hizo:** redefinición del orden del proyecto (documento `proyecto-yacht-v2-android-primero.md`). Aplicado el Patch 1 de Android Studio Quail 2. Reemplazado `02-decisiones.md` y creado `04-arquitectura.md`.
- **Decisiones tomadas:** el producto principal pasa a ser la aplicación Android para celular (Fase A); Wear OS queda como Fase B reutilizando el módulo `:core`. Se fija la arquitectura multi-módulo (`:core` en Kotlin puro + `:app-mobile` + `:app-wear`). Se descarta actualizar a Quail 3 mientras siga en Release Candidate.
- **Problemas encontrados:** —
- **Siguiente paso:** definir modelo de celular de pruebas y su versión de Android (fija el `minSdk`), luego variante de reglas y alcance del MVP de la Fase A.

---

### 2026-07-31 — Dispositivo y niveles de API definidos

- **Qué se hizo:** cerradas las decisiones 4 y 5. Verificados los requisitos vigentes de Google Play para el `targetSdk`.
- **Decisiones tomadas:** celular de pruebas Xiaomi Poco F5 Pro con Android 15. `minSdk = 26` (Android 8.0) y `targetSdk = 36` (Android 16), este último obligatorio para apps nuevas en Google Play desde el 31-08-2026.
- **Problemas encontrados:** ninguno todavía. Anotado como riesgo futuro: los Xiaomi con HyperOS requieren activar "Instalar vía USB" (con cuenta Mi) además de "Depuración USB" para poder instalar desde Android Studio.
- **Siguiente paso:** elegir la variante de reglas de puntuación (decisión 7, ver `01-especificacion-juego.md`) y definir el alcance del MVP de la Fase A. Después de eso, crear el proyecto multi-módulo en Android Studio.

---

### 2026-07-31 — Reglas de puntuación cerradas

- **Qué se hizo:** cerradas las decisiones 7, 7a y 17. Reescrito `01-especificacion-juego.md` con el reglamento definitivo (se elimina la columna comparativa Yacht/Yahtzee) y actualizado el contrato de `:core` en `04-arquitectura.md`.
- **Decisiones tomadas:**
  - Preset por defecto "Clásico", 12 categorías: bonus de sección superior 63→+35; Full House y Four Dice puntúan la suma de los 5 dados; Small Straight 15, Big Straight 30, Yacht 50. Máximo teórico 325. Los valores de las escaleras los fijó Ignacio; queda anotada la objeción de balance sobre Small Straight = 15 para revisar tras el primer playtest.
  - Sub-reglas: un Yacht cuenta como Full House y como Four Dice; Small Straight admite 4 consecutivos cualesquiera entre los 5 dados; Big Straight exige los 5 consecutivos; sin bonificación por Yacht múltiple.
  - El motor de `:core` se parametriza con un `RuleSet` inmutable desde el primer commit y recorre `ruleSet.categorias` en vez de un enum fijo. El MVP construye un único preset y no expone selección al usuario.
  - Al backlog: preset "Moderno" de 13 categorías (con Three of a Kind y Full House 25 fijo), pantalla de ajustes pre-partida, bonificación por Yacht múltiple.
- **Problemas encontrados:** se detectó que la especificación previa era un híbrido inconsistente (el bonus 63/35 y Four Dice = suma vienen de Yahtzee; la lista de 12 categorías, de Yacht). Resuelto fijando un reglamento propio y coherente. Se evaluó y descartó un preset sin bonus de sección superior: elimina la principal tensión de mediano plazo del juego.
- **Siguiente paso:** definir el alcance del MVP de la Fase A (decisión 8): qué entra y qué no en la primera versión jugable.

---

### 2026-07-31 — Alcance del MVP cerrado

- **Qué se hizo:** cerradas las decisiones 8 y 19. Con esto no quedan decisiones que bloqueen la implementación.
- **Decisiones tomadas:**
  - MVP de la Fase A: partida en solitario de 12 turnos hasta el puntaje final, tabla con bonus y total, anotación obligatoria con sacrificio, pantalla de fin de partida, persistencia de la partida en curso y háptica al lanzar. Fuera: multijugador, historial, desafío diario, gesto de sacudir, ajustes de reglas, sonido, animación 3D, cuenta/nube y deshacer.
  - El gesto de sacudir, aunque es el diferenciador previsto, queda como primer trabajo posterior al MVP: exige calibrar umbral y antirrebote, y un falso positivo arruina la partida.
  - `EstadoPartida` en `:core` se modela con lista de jugadores e índice de turno desde el primer commit, aunque el MVP juegue en solitario.
- **Problemas encontrados:** —
- **Siguiente paso:** crear el proyecto base en Android Studio (Empty Activity con Compose, `minSdk 26`, Kotlin DSL), en una ruta sin espacios ni tildes.

---

### 2026-07-31 — Proyecto creado y repositorio en GitHub

- **Qué se hizo:**
  - Creado el proyecto en `C:\Proyectos\Dados` con la plantilla Empty Activity (Compose), package `cl.ignaciodiaz.dados`, `minSdk 26`, Kotlin DSL y catálogo de versiones. Gradle Sync exitoso.
  - Cerradas las decisiones 20 y 21. Repositorio público creado: https://github.com/ignacio-id7/Dados
  - Commit inicial verificado: incluye el Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/`), los `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` y `app/`. **No** incluye `local.properties`.
- **Decisiones tomadas:** control de versiones con Git y GitHub público; los documentos de diseño se mueven a `docs/` dentro del repositorio.
- **Problemas encontrados:**
  - Ruta del proyecto fuera del perfil de usuario por el mismo motivo que el SDK: espacio y tilde en `C:\Users\Ignacio Díaz\`.
  - Aviso de Microsoft Defender en Android Studio: se aplican las exclusiones de carpetas que ofrece el IDE, porque el escaneo en tiempo real ralentiza los builds de Gradle.
  - Git ya estaba instalado en el equipo (`winget` no encontró actualizaciones).
  - Se subió también la carpeta `.idea/`. Es lo habitual; queda anotado por si genera commits ruidosos.
- **Siguiente paso:** mover los documentos de diseño a `C:\Proyectos\Dados\docs`, reconectar esa carpeta como carpeta de trabajo, y después renombrar el módulo `app` a `app-mobile`.
