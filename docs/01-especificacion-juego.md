# Especificación del juego

> Reglas cerradas el 2026-07-31 (decisiones 7 y 17 de `02-decisiones.md`).
> Documento independiente del dispositivo: aplica igual a la Fase A (celular) y a la Fase B (reloj).

El juego es original y está inspirado en una mecánica de dominio público. Las reglas de abajo son **nuestras**: no reproducen el reglamento de ninguna aplicación existente.

## Estructura

- 5 dados de 6 caras.
- 12 turnos por jugador. La partida termina cuando las 12 categorías están completas.
- Por turno: hasta 3 lanzamientos. El primero lanza los 5 dados; en el 2.º y el 3.º el jugador elige cuáles retener y cuáles relanzar.
- Al terminar el turno el jugador debe anotar obligatoriamente en una categoría aún libre. Si ninguna combinación aplica, anota 0 en la categoría que elija (sacrificio).

## Reglamento por defecto — preset "Clásico"

Es el único reglamento que expone el MVP de la Fase A.

### Sección superior

| Categoría | Puntaje |
|---|---|
| Ones | Suma de los dados con valor 1 |
| Twos | Suma de los dados con valor 2 |
| Threes | Suma de los dados con valor 3 |
| Fours | Suma de los dados con valor 4 |
| Fives | Suma de los dados con valor 5 |
| Sixes | Suma de los dados con valor 6 |

**Bonus:** si el subtotal de la sección superior alcanza **63 puntos o más**, se suman **35 puntos**. (63 equivale a anotar tres dados de cada valor en cada categoría).

### Sección inferior

| Categoría | Condición | Puntaje |
|---|---|---|
| Choice | Cualquier combinación | Suma de los 5 dados |
| Four Dice | Al menos 4 dados iguales | Suma de los 5 dados |
| Full House | 3 iguales + 2 iguales | Suma de los 5 dados |
| Small Straight | 4 dados consecutivos entre los 5 | 15 fijo |
| Big Straight | Los 5 dados consecutivos | 30 fijo |
| Yacht | Los 5 dados iguales | 50 fijo |

### Aclaraciones de validez

Estas son las reglas que el motor debe hacer cumplir y que los tests deben cubrir explícitamente:

- **Un Yacht (5 iguales) es un Full House válido.** Cinco iguales contienen 3+2.
- **Un Yacht es un Four Dice válido.** Cinco iguales contienen 4 iguales.
- **Small Straight** admite cualquier secuencia de 4 consecutivos contenida en los 5 dados: `1-2-3-4`, `2-3-4-5` o `3-4-5-6`. No exige que los 5 dados formen exactamente la secuencia.
- **Big Straight** exige que los 5 dados sean consecutivos: `1-2-3-4-5` o `2-3-4-5-6`.
- **Un Big Straight es un Small Straight válido** (contiene 4 consecutivos).
- **No hay bonificación por Yacht múltiple.** Un segundo Yacht en la misma partida no otorga puntos extra, y no existen reglas de comodín. Fuera del MVP; queda en el backlog.
- Choice, Four Dice y Full House comparten la misma fórmula (suma de los 5 dados) y se diferencian solo por su condición de validez. El motor debe tratarlas como validación distinta sobre un mismo cálculo.

## Puntaje total

Total = (Sección superior + Bonus si aplica) + Sección inferior.

Puntaje máximo teórico con este preset: sección superior 105 + bonus 35 + Choice 30 + Four Dice 30 + Full House 30 + Small Straight 15 + Big Straight 30 + Yacht 50 = **325**.

### Nota de balance pendiente de playtesting

Con Small Straight en 15, la categoría queda por debajo de la esperanza de una tirada cualquiera anotada en Choice (17,5 puntos). La consecuencia es que perseguir una escalera chica nunca conviene: pasa a ser una casilla de sacrificio en vez de una jugada buscada. Es un valor deliberado, no un error, pero es el primer número a revisar cuando el juego sea jugable. Al estar en el `RuleSet`, cambiarlo es editar una constante y volver a correr los tests.

## Reglamento parametrizado

El motor de `:core` **no puede hardcodear estos valores**. Recibe un objeto `RuleSet` inmutable que define, como mínimo:

- La lista de categorías que componen la partida (y por lo tanto el número de turnos).
- Si existe bonus de sección superior, su umbral y su valor.
- La fórmula de puntaje de cada categoría (suma de los 5 dados, valor fijo, suma de los dados coincidentes).
- Las reglas de validez configurables (si un Yacht cuenta como Full House o como Four Dice; si Small Straight admite subsecuencias).
- Si existe bonificación por Yacht múltiple.

El motor recorre `ruleSet.categorias`, nunca un enum fijo. Motivo: hacerlo desde el primer commit cuesta casi lo mismo que hardcodear; retrofitearlo después es un refactor que rompe motores de puntuación en silencio.

El MVP construye un único `RuleSet` (el preset "Clásico" de arriba) y no expone selección al usuario.

## Backlog de reglas — fuera del MVP

Anotado, no descartado:

- **Preset "Moderno":** 13 categorías (agrega Three of a Kind), Full House 25 fijo. Puntajes más comparables entre partidas, útil si entra el modo desafío diario con semilla por fecha.
- **Pantalla de ajustes pre-partida** para elegir preset o combinar reglas sueltas. Requiere UI, persistencia de preferencias y validación de combinaciones coherentes.
- **Bonificación por Yacht múltiple** y las reglas de comodín asociadas.
- **Variante sin bonus de sección superior.** Evaluada y descartada como preset: sin el bonus, la sección superior se vuelve un vertedero para tiradas malas y el juego pierde su principal tensión de mediano plazo.

## Alcance del MVP de la Fase A

Cerrado el 2026-07-31 (decisión 8). Criterio: la primera versión jugable debe permitir terminar una partida completa y de verdad, y nada más.

**Entra:**

- Partida en solitario, 12 turnos completos, hasta el puntaje final.
- Lanzamiento con retención de dados y los 3 tiros por turno.
- Tabla de 12 filas con subtotal superior, indicador de bonus y total.
- Anotación obligatoria al cerrar el turno, incluido el sacrificio con 0.
- Pantalla de fin de partida con el puntaje.
- Persistencia de la partida en curso (obligatoria: el proceso puede morir en cualquier momento).
- Háptica al lanzar.

**No entra:**

- Multijugador de cualquier tipo. (El modelo de `:core` sí lo soporta desde el primer commit — decisión 19 — pero la interfaz no lo expone).
- Historial y estadísticas.
- Modo desafío diario con semilla por fecha.
- Gesto de sacudir para lanzar. Es el diferenciador previsto y es el primer trabajo *después* del MVP; queda fuera porque exige calibrar umbral y antirrebote contra el acelerómetro real, y un falso positivo arruina la partida.
- Pantalla de ajustes de reglas (decisión 18).
- Sonido y animación 3D de dados.
- Cuenta de usuario, nube, compartir, logros.
- Deshacer la última anotación.
