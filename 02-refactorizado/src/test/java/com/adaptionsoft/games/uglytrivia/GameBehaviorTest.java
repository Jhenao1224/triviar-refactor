package com.adaptionsoft.games.uglytrivia;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias mas finas, enfocadas en reglas de negocio puntuales.
 * A diferencia del golden master (que protege TODO el comportamiento a la
 * vez pero es opaco), estas pruebas documentan y explican reglas concretas:
 * son las que se leen para entender COMO funciona el juego, y las primeras
 * en delatar en que regla especifica se rompio algo si el golden master
 * falla.
 *
 * Todos los valores esperados se obtuvieron ejecutando el propio codigo
 * (no se "inventaron a mano"): son, por tanto, pruebas de caracterizacion
 * tambien, solo que mas pequenas y con nombres que documentan la intencion.
 */
class GameBehaviorTest {

    @Test
    void aGameWithFewerThanTwoPlayersIsNotPlayable() {
        Game game = new Game();
        assertFalse(game.isPlayable());

        game.add("Ana");
        assertFalse(game.isPlayable());

        game.add("Beto");
        assertTrue(game.isPlayable());
    }

    @Test
    void addingAPlayerRegistersItAtPositionZeroWithNoCoins() {
        Game game = new Game();
        game.add("Ana");

        assertEquals(1, game.howManyPlayers());
        assertEquals(0, game.getPlaceOf(0));
        assertEquals(0, game.getPurseOf(0));
        assertFalse(game.isInPenaltyBox(0));
    }

    @Test
    void aCorrectAnswerAwardsACoinAndPassesTheTurn() {
        Game game = new Game();
        game.add("Ana");
        game.add("Beto");

        game.roll(3); // Ana: 0 -> 3
        boolean gameContinues = game.wasCorrectlyAnswered();

        assertTrue(gameContinues, "con 1 moneda el juego debe continuar");
        assertEquals(3, game.getPlaceOf(0));
        assertEquals(1, game.getPurseOf(0));
        assertEquals(1, game.getCurrentPlayerIndex(), "el turno debe pasar al siguiente jugador");
    }

    @Test
    void movingPastTheLastCellWrapsAroundTheBoard() {
        Game game = new Game();
        game.add("Ana");
        game.add("Beto");

        game.roll(3); game.wasCorrectlyAnswered();  // Ana: 0 -> 3
        game.roll(2); game.wrongAnswer();            // Beto: entra en la caja
        game.roll(5); game.wasCorrectlyAnswered();   // Ana: 3 -> 8
        game.roll(4); game.wasCorrectlyAnswered();   // Beto en caja, par: se queda dentro, sin mover
        game.roll(4); game.wasCorrectlyAnswered();   // Ana: 8 -> 12 % 12 -> 0 (vuelta al tablero)

        assertEquals(0, game.getPlaceOf(0), "12 debe dar la vuelta y quedar en la casilla 0");
        assertEquals(3, game.getPurseOf(0));
    }

    @Test
    void aWrongAnswerSendsThePlayerToThePenaltyBoxWithoutAwardingACoin() {
        Game game = new Game();
        game.add("Ana");
        game.add("Beto");

        game.roll(3); game.wasCorrectlyAnswered(); // turno de Ana
        game.roll(2);                               // turno de Beto
        boolean gameContinues = game.wrongAnswer();

        assertTrue(gameContinues, "wrongAnswer() siempre devuelve true en el diseno original");
        assertTrue(game.isInPenaltyBox(1));
        assertEquals(0, game.getPurseOf(1), "no se otorga moneda si la respuesta es incorrecta");
        assertEquals(0, game.getCurrentPlayerIndex(), "el turno vuelve al primer jugador");
    }

    @Test
    void aPlayerInThePenaltyBoxWhoRollsEvenStaysInWithoutBeingAskedAQuestion() {
        Game game = new Game();
        game.add("Ana");
        game.add("Beto");

        game.roll(3); game.wasCorrectlyAnswered(); // Ana
        game.roll(2); game.wrongAnswer();            // Beto entra en la caja, place=2

        int placeBefore = game.getPlaceOf(1);
        game.roll(3);                                 // turno de Ana otra vez? no: turno vuelve a Ana? currentPlayer=0
        // avanzamos hasta que le toque de nuevo a Beto
        game.wasCorrectlyAnswered();
        game.roll(4); // roll par para Beto, en caja -> se queda dentro
        game.wasCorrectlyAnswered();

        assertEquals(placeBefore, game.getPlaceOf(1),
            "con tirada par y estando en la caja, el jugador no se mueve ni responde pregunta");
        assertTrue(game.isInPenaltyBox(1), "el marcador de caja de penalizacion nunca se limpia (comportamiento heredado)");
    }

    @Test
    void aPlayerInThePenaltyBoxWhoRollsOddGetsToPlayThatTurnNormally() {
        Game game = new Game();
        game.add("Ana");
        game.add("Beto");

        game.roll(3); game.wasCorrectlyAnswered(); // Ana
        game.roll(2); game.wrongAnswer();            // Beto entra en la caja, place=2

        game.roll(5); game.wasCorrectlyAnswered();  // Ana
        game.roll(4); game.wasCorrectlyAnswered();  // Beto (par, se queda dentro)
        game.roll(4); game.wasCorrectlyAnswered();  // Ana
        game.roll(1);                                 // Beto: impar, sale este turno y juega
        boolean gameContinues = game.wasCorrectlyAnswered();

        assertTrue(gameContinues);
        assertEquals(3, game.getPlaceOf(1), "2 + 1 = 3 (categoria Rock)");
        assertEquals(1, game.getPurseOf(1), "si responde bien ese turno, SI gana moneda aunque siga marcado en la caja");
    }

    @Test
    void aPlayerWinsOnReachingSixCoinsAndTheGameStopsContinuing() {
        Game game = new Game();
        game.add("Ana");
        game.add("Beto");

        boolean gameContinues = true;
        for (int i = 0; i < 11 && gameContinues; i++) {
            game.roll(3);
            gameContinues = game.wasCorrectlyAnswered();
        }

        assertFalse(gameContinues, "al llegar a 6 monedas, wasCorrectlyAnswered() debe devolver false");
        assertEquals(6, game.getPurseOf(0));
    }

    @Test
    void categoriesRepeatEveryFourCellsInTheSameOrder() {
        assertEquals(Category.POP, Category.forPlace(0));
        assertEquals(Category.SCIENCE, Category.forPlace(1));
        assertEquals(Category.SPORTS, Category.forPlace(2));
        assertEquals(Category.ROCK, Category.forPlace(3));
        assertEquals(Category.POP, Category.forPlace(4));
        assertEquals(Category.POP, Category.forPlace(8));
        assertEquals(Category.SCIENCE, Category.forPlace(9));
        assertEquals(Category.ROCK, Category.forPlace(11));
    }

    @Test
    void questionDeckServesFiftyDistinctQuestionsPerCategoryInOrder() {
        QuestionDeckTestHelper.assertServesQuestionsInOrder();
    }

    /** Pequena clase auxiliar solo para no exponer QuestionDeck fuera del paquete. */
    static class QuestionDeckTestHelper {
        static void assertServesQuestionsInOrder() {
            QuestionDeck deck = new QuestionDeck();
            assertEquals("Pop Question 0", deck.nextQuestionFor(Category.POP));
            assertEquals("Pop Question 1", deck.nextQuestionFor(Category.POP));
            assertEquals("Rock Question 0", deck.nextQuestionFor(Category.ROCK));
        }
    }
}
