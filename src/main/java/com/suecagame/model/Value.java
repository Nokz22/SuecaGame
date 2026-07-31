package com.suecagame.model;

public enum Value {

    ÀS(10, 11, "Ás", "A"),
    SETE(9, 10, "7", "7"),
    REI(8, 4, "Rei", "R"),
    VALETE(7, 3, "Valete", "V"),
    DAMA(6, 2, "Dama", "D"),
    SEIS(5, 0, "6", "6"),
    CINCO(4, 0, "5", "5"),
    QUATRO(3, 0, "4", "4"),
    TRES(2, 0, "3", "3"),
    DOIS(1, 0, "2", "2");

    private final int value;
    private final int points;
    private final String displayName;
    private final String shortName;

    Value(int value, int points, String displayName, String shortName) {
        this.value = value;
        this.points = points;
        this.displayName = displayName;
        this.shortName = shortName;
    }

    public int getValue() {
        return value;
    }

    public int getPoints() {
        return points;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Nome curto para desenhar na carta (A, 7, R, V, D, ...). */
    public String getShortName() {
        return shortName;
    }
}
