package com.adaptionsoft.games.trivia.runner;

import java.util.Random;

import com.adaptionsoft.games.uglytrivia.Game;

public class GameRunner {

    public static void main(String[] args) {
        Game game = new Game();
        game.add("Chet");
        game.add("Pat");
        game.add("Sue");

        Random rand = new Random();

        boolean gameContinues = true;
        while (gameContinues) {
            game.roll(rand.nextInt(5) + 1);

            boolean answeredWrong = (rand.nextInt(9) == 7);
            gameContinues = answeredWrong ? game.wrongAnswer() : game.wasCorrectlyAnswered();
        }
    }
}
