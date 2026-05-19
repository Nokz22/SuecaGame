package com.suecagame.model;

public enum Value {

    ÀS(10, 11),
    SETE(9,10),
    REI(8,4),
    VALETE(7,3),
    DAMA(6,2),
    SEIS(5,0),
    CINCO(4,0),
    QUATRO(3,0),
    TRES(2,0),
    DOIS(1,0);

    private final int value;
    private final int points;
    Value(int value, int points) {
        this.value = value;
        this.points = points;
    }

    public int getValue() {
        return value;
    }
    public int getPoints() {
        return points;
    }
}
