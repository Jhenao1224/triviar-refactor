package com.adaptionsoft.games.uglytrivia;

/**
 * Reemplaza el "type code" basado en String ("Pop", "Science", ...) por un
 * enum real (Replace Type Code with Class/Enum), y sustituye la larga cadena
 * de if en currentCategory() por aritmética modular (Replace Conditional with
 * Algorithm): las categorias se repiten cada 4 casillas en el mismo orden,
 * asi que place % 4 basta para saber la categoria.
 */
public enum Category {
    POP("Pop"),
    SCIENCE("Science"),
    SPORTS("Sports"),
    ROCK("Rock");

    private static final Category[] VALUES = values();

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static Category forPlace(int place) {
        return VALUES[place % VALUES.length];
    }
}
