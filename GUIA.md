# Refactoring del juego de Trivia (jbrains/trivia, versión Java)

Repositorio base: https://github.com/jbrains/trivia — carpeta `java/`
(paquete `com.adaptionsoft.games.uglytrivia`).

Esta guía cubre los tres entregables que pide el enunciado y sirve como
guion para el video:

1. Identificación de code smells
2. Definición de las pruebas
3. Paso a paso del refactoring, con evidencia verificada

Todo el código de este paquete fue **compilado y ejecutado de verdad**
(no es pseudocódigo): la carpeta `03-evidencia-verificacion/` contiene las
salidas reales capturadas y los `diff` entre el original y cada versión
refactorizada, generados corriendo el código.

---

## 1. Identificación de code smells

Código analizado: `Game.java` (169 líneas) y `GameRunner.java` (36 líneas).

| # | Code smell | Dónde | Por qué es un problema |
|---|---|---|---|
| 1 | **God Class / Large Class** | `Game` | Una sola clase gestiona jugadores, tablero, categorías, preguntas, puntuación, caja de penalización y salida por consola. Ninguna responsabilidad está aislada. |
| 2 | **Primitive Obsession** | `places[]`, `purses[]`, `inPenaltyBox[]` | El estado de "un jugador" está repartido en 3 arrays paralelos indexados por `currentPlayer`, en vez de existir un objeto `Player`. Fácil desincronizar los índices. |
| 3 | **Duplicated Code** | `roll()` y `wasCorrectlyAnswered()` | Ambos métodos repiten casi el mismo bloque en el `if` y en el `else`. La duplicación ya causó un bug real (ver #10). |
| 4 | **Long Method** | `roll()`, `wasCorrectlyAnswered()` | Mezclan validación, movimiento, impresión y lógica de turno en un solo método. |
| 5 | **Shotgun Surgery / cadena de `if` repetida** | `currentCategory()` (9 ifs) y `askQuestion()` (4 ifs) | Añadir una 5ª categoría obliga a tocar dos métodos distintos y a repetir el patrón. |
| 6 | **Magic Numbers** | `6` (límite de jugadores y monedas para ganar), `11`/`12` (tamaño del tablero), `0,4,8 / 1,5,9 / 2,6,10` (mapeo de categorías) | Ningún número tiene nombre; hay que leer y "descifrar" la intención. |
| 7 | **Comparación de Strings con `==`** | `if (currentCategory() == "Pop")` | Funciona "por casualidad" gracias al *string interning* de literales en Java; es frágil y es un olor clásico. |
| 8 | **Acoplamiento a la salida estándar (I/O mezclado con lógica de dominio)** | `System.out.println` disperso en casi todos los métodos | Es imposible probar la lógica de negocio sin capturar la consola; `Game` no se puede reusar con otra UI. |
| 9 | **Nombres que mienten (Mysterious Naming)** | `didPlayerWin()` devuelve `true` cuando el jugador **NO** ha ganado (`!(purses[...] == 6)`); `GameRunner` guarda ese valor en una variable llamada `notAWinner` | El nombre del método dice lo contrario de lo que retorna. Entendible solo si se lee el cuerpo. |
| 10 | **Bug real causado por la duplicación** | `"Answer was correct!!!!"` vs `"Answer was corrent!!!!"` (typo) en las dos copias casi idénticas de `wasCorrectlyAnswered()` | Las dos copias divergieron con el tiempo: exactamente el riesgo que advierte "no te repitas" (DRY). Se detecta y se corrige en el refactor (paso documentado más abajo). |
| 11 | **Estado mutable global con acoplamiento temporal** | `currentPlayer`, `isGettingOutOfPenaltyBox` | `wasCorrectlyAnswered()`/`wrongAnswer()` solo tienen sentido si se llamaron *después* de `roll()`. Nada en el código impone ese orden; es un contrato implícito. |
| 12 | **Colecciones sin genéricos / encapsulación débil** | `ArrayList players = new ArrayList();` (raw type), campos con visibilidad de paquete (no `private`) | Estilo pre-Java-5; cualquier clase del mismo paquete puede mutar el estado interno de `Game` directamente. |
| 13 | **God Method / estado estático mutable en `GameRunner`** | `private static boolean notAWinner;` | Campo estático mutable en la clase que contiene `main`; acopla el runner a `Random` sin poder inyectar semillas para pruebas deterministas. |
| 14 | **Bug latente (capacidad fija de 6 jugadores)** | `int[] places = new int[6];` | Nada valida el límite en `add()`; agregar un 7º jugador lanza `ArrayIndexOutOfBoundsException` en producción, no en un `if` controlado. |
| 15 | **"Falsa salida" de la caja de penalización** | `inPenaltyBox[currentPlayer]` nunca se pone en `false` en ningún método | El nombre sugiere que el jugador "sale" de la caja, pero el marcador queda `true` para siempre; solo `isGettingOutOfPenaltyBox` decide turno a turno si se ignora. Es un bug/diseño confuso que se **preserva a propósito** en el refactor (ver sección 4, nota de fidelidad) porque cambiarlo sin autorización sería alterar el comportamiento sin pruebas de aceptación para el nuevo comportamiento. |

> Sugerencia para el video: muestra el código fuente con estos 15 puntos
> señalados uno a uno (captura de pantalla + resaltado), no hace falta
> leerlos todos en voz alta con el mismo detalle; los más vistosos para
> explicar son el #3/#10 (duplicación → bug real) y el #9 (nombre que miente).

---

## 2. Definición de las pruebas

**Problema de partida:** no hay ninguna prueba, y no hay ninguna
especificación aparte del propio código. No se puede "adivinar" el
comportamiento correcto; solo se puede **congelar el comportamiento actual**
y refactorizar sin romperlo. Esa es exactamente la técnica de **Golden
Master / Characterization Testing** (Michael Feathers, *Working Effectively
with Legacy Code*).

### 2.1 Estrategia en dos capas

**Capa 1 — Golden Master (`GameCharacterizationTest`)**
- Se define un guion **fijo y determinista** de 21 turnos (nada de `Random`):
  cada turno es un par `(tirada, respuesta correcta/incorrecta)`.
- El guion se diseñó para tocar todas las ramas relevantes: movimiento
  normal, vuelta al tablero (posición > 11), las 4 categorías, entrada a la
  caja de penalización, permanencia dentro (tirada par) y salida (tirada
  impar), y varias rondas de acumulación de monedas.
- Se captura **toda la salida de consola** (`System.out`) ejecutando ese
  guion contra el código **sin tocar nada**, y esa captura se guarda como
  el archivo "aprobado" (`golden-master.approved.txt`).
- A partir de ahí, cualquier refactor que cambie una sola letra de esa
  salida hace fallar la prueba. Eso obliga a decidir conscientemente: ¿es
  un efecto secundario no deseado (revertir el cambio) o es una corrección
  intencional (actualizar el archivo aprobado y documentarlo)?

**Capa 2 — Pruebas de comportamiento específicas (`GameBehaviorTest`)**
El golden master protege *todo* a la vez pero es opaco (si falla, hay que
leer un diff de texto). Por eso se añaden 10 pruebas más pequeñas, con
nombres que documentan una regla de negocio cada una:

- `aGameWithFewerThanTwoPlayersIsNotPlayable`
- `addingAPlayerRegistersItAtPositionZeroWithNoCoins`
- `aCorrectAnswerAwardsACoinAndPassesTheTurn`
- `movingPastTheLastCellWrapsAroundTheBoard` (posición 12 → 0)
- `aWrongAnswerSendsThePlayerToThePenaltyBoxWithoutAwardingACoin`
- `aPlayerInThePenaltyBoxWhoRollsEvenStaysInWithoutBeingAskedAQuestion`
- `aPlayerInThePenaltyBoxWhoRollsOddGetsToPlayThatTurnNormally`
- `aPlayerWinsOnReachingSixCoinsAndTheGameStopsContinuing`
- `categoriesRepeatEveryFourCellsInTheSameOrder`
- `questionDeckServesFiftyDistinctQuestionsPerCategoryInOrder`

Todos los valores esperados de estas pruebas **no se inventaron a mano**:
se obtuvieron ejecutando el propio código con un pequeño programa de
"descubrimiento" y leyendo el resultado real (evita el error clásico de
escribir un `assertEquals` con el número equivocado).

### 2.2 Cómo se hizo posible probar código sin tests previos

1. Los campos de `Game` en el original tienen visibilidad de **paquete**
   (no `private`), así que una clase de prueba en el mismo paquete
   (`com.adaptionsoft.games.uglytrivia`) puede leerlos directamente sin
   reflexión, para inspeccionar estado interno mientras aún no existían
   getters.
2. Para capturar la salida por consola sin modificar el código de
   producción, se redirige `System.out` con `System.setOut(...)` a un
   `ByteArrayOutputStream` antes de ejecutar el guion, y se restaura al
   terminar (patrón estándar para probar código con I/O acoplado).
3. Solo **después** de tener esta red de seguridad se empezó a
   refactorizar.

### 2.3 Resultado real (verificado en este entorno, no solo "en teoría")

```
11 pruebas ejecutadas -> 11 aprobadas (10 de comportamiento + 1 golden master)
```

---

## 3. Paso a paso del refactoring

Cada paso se hizo de forma **pequeña y reversible**, y se comprobó contra
el golden master antes de seguir al siguiente. Aquí el resumen para narrar
en el video (código completo en `02-refactorizado/`).

### Paso 0 — Congelar el comportamiento (ver sección 2)
Antes de tocar una sola línea de producción: escribir `GameCharacterizationTest`
+ `GameBehaviorTest` contra el código original y comprobar que pasan.

### Paso 1 — Replace Magic Number with Symbolic Constant
`6`, `11`/`12` pasan a ser `COINS_TO_WIN`, `BOARD_SIZE`. Riesgo mínimo,
cero cambio de comportamiento, primer paso "de calentamiento".

### Paso 2 — Replace Conditional with Algorithm (`currentCategory`)
Los 9 `if` de `currentCategory()` se reemplazan por aritmética modular:
las categorías se repiten cada 4 casillas en el mismo orden
(Pop, Science, Sports, Rock), así que `place % 4` basta.

```java
// Antes: 9 comparaciones == encadenadas
if (places[currentPlayer] == 0) return "Pop";
if (places[currentPlayer] == 4) return "Pop";
... (7 líneas más)

// Después
public static Category forPlace(int place) {
    return VALUES[place % VALUES.length];
}
```

### Paso 3 — Replace Type Code with Enum (`Category`)
Los Strings "Pop"/"Science"/"Sports"/"Rock" (comparados con `==`, un olor
en sí mismo) se convierten en el enum `Category`. Elimina de raíz el smell
#7 de la tabla.

### Paso 4 — Extract Class `QuestionDeck`
Los 4 campos `LinkedList` casi idénticos (`popQuestions`, `scienceQuestions`,
`sportsQuestions`, `rockQuestions`) y el `askQuestion()` con 4 `if`
idénticos salvo el nombre del campo se colapsan en un solo
`Map<Category, Queue<String>>` dentro de una clase nueva. Un solo camino
de código en vez de cuatro copiados.

### Paso 5 — Extract Class `Player` (elimina Primitive Obsession)
Los tres arrays paralelos `places[]`, `purses[]`, `inPenaltyBox[]`
(indexados a mano por `currentPlayer`) se convierten en una
`List<Player>`, donde cada `Player` encapsula su propio `place`, `purse`
e `inPenaltyBox`. Efecto colateral positivo y verificado: desaparece el
límite artificial de 6 jugadores (antes, un 7° jugador lanzaba
`ArrayIndexOutOfBoundsException`).

### Paso 6 — Extract Method / eliminar duplicación en `roll()` y `wasCorrectlyAnswered()`
Los bloques idénticos "mover ficha + imprimir + preguntar" y "dar moneda +
comprobar victoria + pasar turno" se extraen a métodos privados
(`moveAndAskQuestion`, y la unificación de las dos ramas de
`wasCorrectlyAnswered`).

**Punto clave del video — la duplicación escondía un bug:**
Al mirar de cerca las dos copias de `wasCorrectlyAnswered()` para
fusionarlas, aparece la divergencia: una decía `"Answer was correct!!!!"`
y la otra `"Answer was corrent!!!!"` (typo). Esto se hizo en **dos
sub-pasos**, cada uno verificado por separado contra el golden master
(evidencia real en `03-evidencia-verificacion/`):

- **Paso 6a (solo estructural):** se fusiona el código duplicado pero se
  **preserva el texto exacto de cada rama** con un parámetro, a propósito,
  para no cambiar comportamiento todavía.
  Resultado real: `diff-1-refactor-estructural-vs-original.txt` está
  **vacío** → 0 diferencias de salida. Refactor 100% neutro, confirmado.

- **Paso 6b (arreglo de bug, deliberado y documentado aparte):** se
  unifica el texto a `"Answer was correct!!!!"` (la ortografía correcta).
  Resultado real: `diff-2-refactor-final-vs-original.txt` muestra
  **exactamente 15 líneas** cambiadas, todas `"corrent" -> "correct"`, y
  nada más. Ese diff, tan chico y tan explicable, es la prueba de que el
  golden master está haciendo su trabajo: cualquier cambio de
  comportamiento se ve inmediatamente y se puede auditar línea por línea.

> Este es el momento más demostrativo para el video: mostrar el `diff`
> vacío del paso estructural, y luego el `diff` de 15 líneas del arreglo
> de bug, explicando por qué ambos se consideran "seguros" (uno porque no
> cambia nada; el otro porque el cambio es mínimo, entendido y aprobado a
> propósito).

### Paso 7 — Rename Method (`didPlayerWin` → lógica inline con nombre honesto)
`didPlayerWin()` (que en realidad devolvía "no ganó") se elimina; la
condición se vuelve `boolean gameContinues = !player.hasReached(COINS_TO_WIN);`.
Incluido dentro del paso 6 por simplicidad, pero es conceptualmente un
refactor de nombres, no de comportamiento (`assertEquals` idénticos antes
y después).

### Paso 8 — Introduce Explaining Variable / limpiar `GameRunner`
Se elimina el campo `static boolean notAWinner` (estado mutable a nivel de
clase) y se reemplaza por una variable local `gameContinues` dentro del
bucle `while`. Cero cambio de comportamiento observable (el programa sigue
haciendo exactamente las mismas tiradas para la misma semilla de `Random`).

### Paso 9 — Documentar (no "arreglar") el bug de la caja de penalización
Al encapsular `Player`, era tentador añadir un método
`releaseFromPenaltyBox()` — pero el código original **nunca** libera al
jugador (el campo se queda en `true` para siempre; solo
`isGettingOutOfPenaltyBox` decide turno a turno si eso se ignora). Se optó
por **no** cambiar ese comportamiento (habría sido un cambio de reglas del
juego sin pruebas de aceptación para la nueva regla) y en su lugar se dejó
un comentario explícito en `Game.roll()` señalándolo como candidato a un
cambio futuro, con sus propias pruebas y aprobación aparte. Es un ejemplo
de disciplina de refactoring: *"mejorar la estructura sin cambiar el
comportamiento"* también implica **no colarse** un cambio de comportamiento
que parece inocente.

### Resultado final
- `Game.java` pasó de 169 líneas haciendo de todo, a ~95 líneas que solo
  orquestan turnos, delegando en `Player`, `Category` y `QuestionDeck`.
- 11/11 pruebas en verde.
- El único cambio de comportamiento observable en todo el proceso es el
  arreglo del typo, hecho a propósito y en un paso separado y auditable.

---

## 4. Cómo ejecutar el proyecto tú mismo (para grabar el video)

Requisitos: JDK 11+ y Maven (o usar el `mvnw` incluido).

```bash
# Código original (para mostrar el "antes")
cd 01-original
./mvnw test        # o: mvn test

# Código refactorizado (para mostrar el "después" y correr las pruebas)
cd ../02-refactorizado
./mvnw test        # debe mostrar 11 pruebas OK

# Ejecutar el juego de verdad (partida aleatoria por consola)
./mvnw -q compile exec:java -Dexec.mainClass="com.adaptionsoft.games.trivia.runner.GameRunner"
# (si no tienes el plugin exec configurado, compila y ejecuta con java -cp directamente)
```

> Nota: si al correr `GameCharacterizationTest` contra `01-original`
> quieres verlo fallar a propósito (para el video), copia
> `GameCharacterizationTest.java` y `golden-master.approved.txt` de
> `02-refactorizado` dentro de `01-original`: fallará en 15 líneas, todas
> `"Answer was corrent!!!!"` vs `"Answer was correct!!!!"` — es la
> evidencia en vivo de que el golden master detecta el cambio real.

---

## 5. Guion sugerido para el video (orden recomendado)

1. **Intro (30s):** mostrar el repo, decir que no hay tests ni docs.
2. **Code smells (2-3 min):** abrir `Game.java`, señalar 4-5 smells de la
   tabla (recomendado: God Class, Primitive Obsession, duplicación,
   `==` con Strings, nombre que miente en `didPlayerWin`).
3. **Pruebas (2-3 min):** explicar por qué no se puede probar "lo
   correcto" sino "lo actual" (golden master); mostrar el guion
   determinista y correr `mvn test` sobre el original en verde.
4. **Refactor paso a paso (5-8 min):** ir mostrando los pasos 1 a 9 de la
   sección 3, corriendo `mvn test` después de cada uno o cada dos pasos.
   El clímax narrativo es el paso 6: mostrar el diff vacío del refactor
   estructural y luego el diff de 15 líneas del arreglo de bug.
5. **Cierre (30s):** comparar tamaño/complejidad de `Game.java` antes y
   después, y mencionar el bug de la caja de penalización que se decidió
   documentar en vez de arreglar sin autorización.
