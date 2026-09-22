package com.adaptionsoft.games.uglytrivia;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Extract Class: antes habia 4 campos LinkedList casi identicos
 * (popQuestions, scienceQuestions, sportsQuestions, rockQuestions) y un
 * metodo askQuestion() con 4 "if" identicos salvo por el nombre del campo.
 * Un Map<Category, Queue<String>> colapsa esa duplicacion en una sola
 * estructura de datos y un solo camino de codigo.
 */
class QuestionDeck {

    private static final int QUESTIONS_PER_CATEGORY = 50;

    private final Map<Category, Queue<String>> questionsByCategory = new EnumMap<>(Category.class);

    QuestionDeck() {
        for (Category category : Category.values()) {
            Queue<String> questions = new LinkedList<>();
            for (int i = 0; i < QUESTIONS_PER_CATEGORY; i++) {
                questions.add(category.displayName() + " Question " + i);
            }
            questionsByCategory.put(category, questions);
        }
    }

    String nextQuestionFor(Category category) {
        return questionsByCategory.get(category).remove();
    }
}
