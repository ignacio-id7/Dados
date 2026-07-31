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

---

### 2026-07-31 — Estructura multi-módulo y migración a Claude Code

- **Qué se hizo:**
  - Renombrado el módulo `app` a `app-mobile` (`git mv` + `include(":app-mobile")` en `settings.gradle.kts`). Gradle Sync exitoso.
  - Creado el módulo `:core` como biblioteca Kotlin JVM pura: plugin `org.jetbrains.kotlin.jvm` agregado al catálogo de versiones, `jvmToolchain(11)` alineado con `app-mobile`, JUnit 4 como dependencia de test y un test de humo que verifica que el módulo compila y los tests corren. `.\gradlew :core:test` en verde.
  - Declarada la dependencia `:app-mobile → :core`, en un solo sentido.
  - Instalado y autenticado Claude Code 2.1.220 (requirió instalar Node.js 24 LTS vía winget). Escrito `CLAUDE.md` en la raíz con las restricciones de arquitectura no negociables.

- **Decisiones tomadas:**
  - La ejecución de código pasa a Claude Code; el proyecto de Claude Desktop queda para diseño, arquitectura y planificación.
  - `:core` es un módulo Kotlin JVM, no una biblioteca Android. Sin el SDK en el classpath, un `import android.*` no compila: la regla deja de depender de la disciplina y pasa a depender del compilador. Además los tests corren en la JVM del PC en segundos, sin emulador.

- **Problemas encontrados:**
  - `00-estado-actual.md` se actualizó antes de ejecutar el rename, así que el `CLAUDE.md` generado a partir de él heredó el dato viejo. Corregido en ambos archivos. Recordatorio: actualizar los documentos después de ejecutar, no antes.
  - `.\gradlew` desde PowerShell falló con `JAVA_HOME is not set`. Causa: el único JDK del equipo es el que Android Studio trae embebido (`C:\Program Files\Android\Android Studio\jbr`), que el IDE usa internamente pero no está registrado a nivel de sistema. Resuelto definiendo `JAVA_HOME` a esa ruta en el perfil de usuario.

- **Siguiente paso:** diseñar el modelo de datos de `:core` (dados, tirada, `RuleSet`, `EstadoPartida`) antes de escribir código.

---

### 2026-07-31 — Modelo de datos de `:core`

- **Qué se hizo:**
  - Cerradas las decisiones 22 a 26 (ver `02-decisiones.md`) y documentado el modelo en `04-arquitectura.md`.
  - Implementados en `core/.../modelo/` los tipos: `Dado`, `Tirada`, `Seccion`, `CategoriaId`, `Categoria`, `BonusSeccionSuperior`, `RuleSet`, `Jugador` y `EstadoPartida`. Todo inmutable, con validación en los constructores y comentarios en español. 27 tests en verde, incluidos los casos que deben fallar.
  - Sin motor ni preset "Clásico" todavía: las funciones de validez y puntaje de las categorías concretas son el paso siguiente.

- **Decisiones tomadas:** `RuleSet` es código y `EstadoPartida` es datos (22); `CategoriaId` basado en texto en vez de `enum` (23); las sub-reglas de validez viven solo en las funciones de cada categoría, sin banderas duplicadas (24), lo que anula lo que insinuaba `01-especificacion-juego.md`; se inyecta `kotlin.random.Random` sin interfaz propia (25); modelo inmutable con `copy()` (26).

- **Problemas encontrados:**
  - `Jugador.puntajeTotal` sumaba solo las anotaciones sin el bonus de sección superior, que depende del `RuleSet`. Un nombre que promete el total y devuelve un número plausible es un bug silencioso esperando a la pantalla de fin de partida. Renombrado a `sumaDeAnotaciones`; el total con bonus lo calculará el motor.
  - `Categoria` se implementó como `data class` conteniendo lambdas: `equals` comparaba funciones por identidad. Pasada a clase normal con igualdad por `id`.
  - Aceptado conscientemente: `Tirada` fija 5 dados y 3 lanzamientos como constantes, pese a que se prohibió la constante `12`. El backlog contempla presets con distinto número de categorías, ninguno con distinto número de dados o lanzamientos.
  - `core/build/` se coló en un commit porque el `.gitignore` raíz decía `/build` (solo la raíz) y `:core` se creó sin `.gitignore` propio. Corregido con `build/` en el `.gitignore` raíz, que cubre cualquier módulo futuro incluido `:app-wear`.

- **Siguiente paso:** implementar el preset "Clásico" —las 12 categorías con sus funciones de validez y puntaje— con tests que cubran explícitamente las aclaraciones de validez de `01-especificacion-juego.md`.

---

### 2026-07-31 — Preset "Clásico" y regla sobre datos personales

- **Qué se hizo:**
  - Implementado `reglas/PresetClasico.kt`: función que construye el `RuleSet` de las 12 categorías con sus funciones de validez y puntaje, más el bonus 63 → +35. Identificadores fijados en inglés, calcados de las tablas de la especificación: `ones`, `twos`, `threes`, `fours`, `fives`, `sixes`, `choice`, `four_dice`, `full_house`, `small_straight`, `big_straight`, `yacht`.
  - 52 tests en verde en `:core`, con cobertura explícita de cada aclaración de validez y de los casos negativos.
  - Cerrada la decisión 27 y registrada en `CLAUDE.md`: el nombre real del autor se mantiene en el identificador de la aplicación como señal de autoría, y queda prohibido en datos de prueba, nombres por defecto, ejemplos y cadenas de interfaz.
  - Abierta la decisión 28 y marcada como bloqueante antes de publicar: el `applicationId` definitivo se decide junto con el nombre del juego.

- **Decisiones tomadas:** identificadores de categoría en inglés y definitivos, porque se persisten y mapean 1:1 con la especificación (renombrarlos rompería las partidas guardadas). Uso del nombre real del autor acotado al `applicationId` (27). El `applicationId` definitivo queda pendiente del nombre del juego (28).

- **Problemas encontrados:**
  - El test del bonus contenía aserciones tautológicas (`assertTrue(63 >= bonus.umbral)`) que comparaban un literal contra el valor recién leído: verdaderas por construcción, incapaces de fallar. Eliminadas. El comportamiento real del bonus solo se puede testear cuando exista el motor.
  - Faltaba el caso de Small Straight con un dado repetido, `(1,2,3,4,4)`, que sí debe ser válido. Agregado; pasó sin tocar la implementación, porque la validez ya se evalúa sobre el conjunto de valores distintos.
  - Los tests usaban el nombre real del autor como dato de prueba. Reemplazado por `Jugador 1`, y la regla registrada en `CLAUDE.md` para que no vuelva a ocurrir.

- **Siguiente paso:** implementar `MotorPartida`: acciones `Lanzar`, `AlternarRetencion(indice)` y `Anotar(categoriaId)`, avance de turno, fin de partida y cálculo del puntaje total con bonus.

---

### 2026-07-31 — `MotorPartida` completo

- **Qué se hizo:**
  - Implementado el paquete `motor/` en `:core`: `Accion` (sealed interface con `Lanzar`, `AlternarRetencion`, `Anotar`), `ResultadoAccion`, `MotivoRechazo` y `MotorPartida`.
  - `MotorPartida` expone `aplicar(estado, accion)`, `accionesLegales(estado)`, `partidaTerminada(estado)` y `puntajeTotal(jugador)`. No guarda estado propio.
  - 68 tests en verde en `:core`. Con esto la lógica completa del MVP está implementada y testeada: falta solo la interfaz y la persistencia.
  - Cerradas las decisiones 29, 30 y 31.

- **Decisiones tomadas:** motor sin estado propio, construido con `RuleSet` y `Random` (29); jugadas ilegales reportadas con `ResultadoAccion.Rechazada`, nunca con excepciones (30); el motor expone `accionesLegales` para que la interfaz pregunte en vez de deducir (31).

- **Problemas encontrados:**
  - `Rechazada` llevaba un `String` en español. Eso es texto de interfaz dentro de `:core`, prohibido por `CLAUDE.md`: habría obligado a reescribir los mensajes en la Fase B y bloqueado cualquier traducción. Reemplazado por el enum `MotivoRechazo`, que cada app traduce a su propio texto.
  - El `String` genérico tapaba que `Anotar` se rechazaba por tres causas distintas con el mismo mensaje. Con el enum quedaron separadas y la interfaz puede reaccionar distinto a cada una.
  - Mejora colateral: `esLegal` y `motivoRechazo` eran dos bloques `when` paralelos que podían desincronizarse. Ahora `esLegal` es `motivoRechazo(...) == null`: una sola fuente de verdad.
  - Nota de calidad: los tests de lanzamiento replican la semilla en un `Random` independiente para calcular los valores esperados, en vez de hardcodearlos. Verifican la regla, no una secuencia concreta del generador.

- **Siguiente paso:** instalar el emulador (AVD) desde el SDK Manager. Es bloqueante: sin él no se puede ejecutar la app, y toda la lógica del MVP ya está lista para tener interfaz encima.

---

### 2026-07-31 — Emulador funcionando y primera pantalla jugable

- **Qué se hizo:**
  - Resuelto el emulador. Nunca hubo problema de virtualización: al instalador solo le faltaban el componente Android Emulator y una imagen de sistema. Instalados desde el SDK Manager (`Google APIs Intel x86_64 Atom`, API 36.1) y creado el AVD Pixel 8.
  - Validada la cadena completa compilando e instalando la plantilla en el emulador antes de escribir interfaz.
  - Implementada la primera pantalla jugable en `:app-mobile`: `PartidaViewModel`, `PartidaUiState`, `PartidaScreen` y `CategoriaTexto`. Dados dibujados con `Canvas`, botón de lanzar, tabla de 12 filas con previsualizaciones y resumen de puntaje con bonus.
  - Agregadas dos consultas a `MotorPartida`: `puntaje(jugador): Puntaje` (desglose con subtotales y bonus, reemplaza a `puntajeTotal`) y `puntajeSiSeAnotara(estado, categoriaId)`, que `anotar()` reutiliza internamente para que "cuánto vale anotar aquí" tenga una sola implementación.
  - Cerradas las decisiones 32 a 38.

- **Decisiones tomadas:** Compose nativo, Unity descartado porque no soporta Wear OS (32); skins como presentación pura fuera de `:core` (33); cosméticos con moneda del juego en vez de dinero real, recomendado y pendiente (34); dados 3D alcanzables en nativo mediante animaciones pre-generadas sobre modelo 3D, con el valor decidido siempre por `:core` y nunca por la física (35); el ViewModel despacha y guarda, no decide (36); dados y tabla en una sola pantalla (37); menú de inicio diferido al final del MVP (38).

- **Problemas encontrados:**
  - La especificación que generó Superpowers se contradecía: el ViewModel construía su propio `Random`, pero los tests decían usar semilla fija. Corregido inyectando el motor por constructor con valor por defecto, aprovechando que Kotlin genera un constructor sin argumentos cuando todos los parámetros lo tienen — así `viewModel()` sigue funcionando sin factory.
  - Superpowers no escribe código hasta que se aprueba una especificación y luego un plan. Se confundió dos veces "presentó el plan" con "terminó". Conviene mirar qué está esperando antes de asumir que acabó.
  - Falsa alarma: se sospechó un error de puntaje leyendo una captura del emulador. La aritmética confirmó que era correcto — un valor ya anotado se confundió con una previsualización. Verificar en el código antes de reportar un bug a partir de una captura.

- **Pendiente inmediato (no hecho):** cuando `tiradaActual` es null, `PartidaScreen` fabrica cinco dados con valor 1. El jugador ve cinco unos inexistentes, indistinguibles de un Yacht real. Hay que dibujar dados vacíos en ese estado y verificar que ninguno sea tocable.

- **Siguiente paso:** corregir los dados fantasma, y después la persistencia de la partida en curso.

---

### 2026-08-01 — Persistencia y entorno de tests reparado

- **Qué se hizo:**
  - Corregidos los dados fantasma: cuando `tiradaActual` es null la pantalla dibuja cinco dados vacíos y no tocables, en vez de fabricar `Dado(valor = 1)`. De paso se agregó `contentDescription` a cada dado para lectores de pantalla.
  - Implementada la persistencia de la partida en curso: `@Serializable` en las clases de datos de `:core`, `RepositorioPartida` con implementación sobre DataStore en `:app-mobile`, carga al iniciar el ViewModel y guardado tras cada acción exitosa. `PartidaUiState.cargando` evita dibujar un tablero falso mientras carga.
  - JSON corrupto o inexistente devuelve `null` y arranca partida nueva; nunca hace caer la app.
  - Verificado matando el proceso con `am force-stop` y reabriendo: la partida vuelve idéntica, con los mismos dados, la misma retención y el mismo contador de lanzamientos.
  - Cerradas las decisiones 12, 39, 40 y 41.

- **Decisiones tomadas:** JSON con `kotlinx.serialization` sobre DataStore, guardando tras cada acción (12); `@Serializable` dentro de `:core`, única concesión a la pureza del módulo, justificada porque la biblioteca es Kotlin puro y multiplataforma y la alternativa obligaba a duplicar la traducción en celular y reloj (39); al abrir se entra directo a la partida guardada (40); el ViewModel recibe el repositorio por constructor con factory, lo que anula la parte de la decisión 36 que evitaba la factory (41).

- **Problemas encontrados:**
  - **`gradlew test` estaba roto en este equipo.** Claude Code lo esquivó ejecutando los tests compilados directamente con `java`, lo que dejaba al proyecto sin un comando confiable que respondiera "¿está todo verde?". Causa: la caché de Gradle vivía en `C:\Users\Ignacio Díaz\.gradle` y el nombre de usuario con tilde corrompía el classpath que Gradle pasa al proceso de tests. Es la tercera vez que la tilde causa un problema en este equipo, tras el SDK y la ruta del proyecto.
  - Resuelto igual que las dos anteriores: `GRADLE_USER_HOME = C:\Gradle`, fuera del perfil de usuario. Se descartó la alternativa de activar UTF-8 global en Windows, que sigue en beta y rompe programas antiguos. Tras el cambio, `.\gradlew test` termina en `BUILD SUCCESSFUL` en todos los módulos.

- **Siguiente paso:** pantalla de fin de partida y háptica al lanzar. Con eso queda cerrado el alcance del MVP salvo el menú de inicio, diferido al final por la decisión 38.

---

### 2026-08-01 — Fin de partida, y el caché de configuración envenenado

- **Qué se hizo:**
  - `Puntaje` expone `umbralBonus: Int?`, para que la interfaz muestre el progreso `60/63` sin conocer el número 63. Nulo si el `RuleSet` no tiene bonus.
  - `ResumenPuntaje` muestra el subtotal superior como progreso durante toda la partida, no solo al final.
  - Panel de fin de partida superpuesto al tablero, cerrable para revisar la tabla y reabrible, con el desglose completo y botón de partida nueva.
  - `RepositorioPartida.borrar()` en las dos implementaciones más la falsa: empezar una partida nueva borra la guardada, si no al reabrir la app volvía la partida terminada.
  - `PartidaUiState.partidaTerminada` lo calcula `motor.partidaTerminada(estado)`; la pantalla nunca lo decide.
  - Cerradas las decisiones 42 y 43.

- **Decisiones tomadas:** el fin de partida es un panel sobre el tablero y no una pantalla aparte, para no quitarle al jugador la tabla que acaba de construir (42); el bonus se muestra como progreso `subtotal/umbral` visible durante toda la partida, en vez de "te faltaron N puntos" al final, porque habla mientras el jugador todavía puede hacer algo (43).

- **Problemas encontrados:**
  - **`ClassNotFoundException: GradleWorkerMain`, y el diagnóstico equivocado que lo acompañó.** El síntoma se atribuyó dos veces a la codificación de Windows con el nombre de usuario con tilde, y la solución propuesta era activar UTF-8 global, que es beta y rompe programas antiguos. La causa real: Claude Code anteponía `GRADLE_USER_HOME="C:/Gradle/home"` —carpeta que no es la configurada— y **esa ejecución guardó un caché de configuración con las rutas equivocadas**. A partir de ahí todas las ejecuciones lo reutilizaban, incluidas las correctas, así que el error sobrevivía a arreglar las variables. Resuelto matando los daemons, borrando `.gradle/configuration-cache` y volviendo a ejecutar. Receta anotada en `CLAUDE.md`.
  - **Lección de método:** durante dos sesiones se dio por verificado un código cuyos tests solo pasaban por un rodeo (`java` a mano con rutas cortas). `CLAUDE.md` ahora prohíbe explícitamente rodear un `gradlew test` que falle. Un verde obtenido por otro camino no es una señal de verificación.
  - Un `BUILD SUCCESSFUL` con la mayoría de las tareas «up-to-date» no prueba que los tests se ejecutaran. Para verificar de verdad: `--rerun-tasks`.

- **Siguiente paso:** háptica al lanzar. Después, el menú de inicio, que cierra el MVP.
