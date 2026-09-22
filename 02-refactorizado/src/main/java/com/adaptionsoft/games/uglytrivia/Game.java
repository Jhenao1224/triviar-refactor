package com.adaptionsoft.games.uglytrivia;

import java.util.ArrayList;
import java.util.List;

public class Game {

    private static final int BOARD_SIZE = 12;
    private static final int COINS_TO_WIN = 6;

    private final List<Player> players = new ArrayList<>();
    private final QuestionDeck questionDeck = new QuestionDeck();

    private int currentPlayerIndex = 0;
    private boolean isGettingOutOfPenaltyBox;

    public boolean isPlayable() {
        return howManyPlayers() >= 2;
    }

    public boolean add(String playerName) {
        players.add(new Player(playerName));
        System.out.println(playerName + " was added");
        System.out.println("They are player number " + players.size());
        return true;
    }

    public int howManyPlayers() {
        return players.size();
    }

    public void roll(int roll) {
        Player player = currentPlayer();
        System.out.println(player.getName() + " is the current player");
        System.out.println("They have rolled a " + roll);

        // BUG heredado del original, preservado a proposito (fuera de alcance
        // de este refactor): inPenaltyBox nunca se pone a "false" en ningun
        // lugar de la clase. Un jugador que cae en la caja de penalizacion
        // se queda "marcado" para siempre; isGettingOutOfPenaltyBox solo
        // decide turno a turno si ese marcador se ignora (tirada impar) o
        // no (tirada par). Es decir: no existe una verdadera "salida" de la
        // caja, solo una condicion que se vuelve a evaluar cada turno.
        // Se documenta aqui como candidato a corregir en un futuro cambio
        // de comportamiento, con sus propias pruebas y aprobacion aparte.
        if (player.isInPenaltyBox()) {
            isGettingOutOfPenaltyBox = (roll % 2 != 0);
            if (isGettingOutOfPenaltyBox) {
                System.out.println(player.getName() + " is getting out of the penalty box");
                moveAndAskQuestion(player, roll);
            } else {
                System.out.println(player.getName() + " is not getting out of the penalty box");
            }
        } else {
            moveAndAskQuestion(player, roll);
        }
    }

    private void moveAndAskQuestion(Player player, int roll) {
        player.moveBy(roll, BOARD_SIZE);
        System.out.println(player.getName() + "'s new location is " + player.getPlace());

        Category category = Category.forPlace(player.getPlace());
        System.out.println("The category is " + category.displayName());
        System.out.println(questionDeck.nextQuestionFor(category));
    }

    public boolean wasCorrectlyAnswered() {
        Player player = currentPlayer();

        // Unico caso en el que NO se otorga moneda: el jugador estaba en la
        // caja de penalizacion y la tirada de este turno no lo saco de ella
        // (no se le llego a hacer pregunta alguna en roll()).
        if (player.isInPenaltyBox() && !isGettingOutOfPenaltyBox) {
            advanceTurn();
            return true;
        }

        System.out.println("Answer was correct!!!!");
        player.earnCoin();
        System.out.println(player.getName() + " now has " + player.getPurse() + " Gold Coins.");

        boolean gameContinues = !player.hasReached(COINS_TO_WIN);
        advanceTurn();
        return gameContinues;
    }

    public boolean wrongAnswer() {
        Player player = currentPlayer();
        System.out.println("Question was incorrectly answered");
        System.out.println(player.getName() + " was sent to the penalty box");
        player.sendToPenaltyBox();
        advanceTurn();
        return true;
    }

    private Player currentPlayer() {
        return players.get(currentPlayerIndex);
    }

    // --- Lectura de estado: antes solo se podia "ver" el juego leyendo lo
    // que se imprimia en consola. Extraer Player como objeto de primera
    // clase permite exponer accesores reales, utiles tanto para pruebas
    // como para una futura UI que no dependa de parsear texto. ---

    public int getPurseOf(int playerIndex) {
        return players.get(playerIndex).getPurse();
    }

    public int getPlaceOf(int playerIndex) {
        return players.get(playerIndex).getPlace();
    }

    public boolean isInPenaltyBox(int playerIndex) {
        return players.get(playerIndex).isInPenaltyBox();
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    private void advanceTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
    }
}
