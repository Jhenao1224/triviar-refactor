package com.adaptionsoft.games.uglytrivia;

/**
 * Extract Class + Replace Data Value with Object: antes el estado de cada
 * jugador vivia repartido en tres arrays paralelos (places[], purses[],
 * inPenaltyBox[]) indexados por currentPlayer. Aqui cada jugador es un solo
 * objeto, lo que elimina el riesgo de desincronizar los indices y el limite
 * artificial de 6 jugadores (tamano fijo de los arrays).
 */
public class Player {

    private final String name;
    private int place = 0;
    private int purse = 0;
    private boolean inPenaltyBox = false;

    public Player(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getPlace() {
        return place;
    }

    public int getPurse() {
        return purse;
    }

    public boolean isInPenaltyBox() {
        return inPenaltyBox;
    }

    public void sendToPenaltyBox() {
        inPenaltyBox = true;
    }

    /** Avanza `roll` casillas en un tablero circular de `boardSize` casillas. */
    public void moveBy(int roll, int boardSize) {
        place = (place + roll) % boardSize;
    }

    public void earnCoin() {
        purse++;
    }

    public boolean hasReached(int coins) {
        return purse == coins;
    }
}
