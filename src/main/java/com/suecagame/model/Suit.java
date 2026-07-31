package com.suecagame.model;

public enum Suit {
    OUROS("Ouros", "♦", true),
    ESPADAS("Espadas", "♠", false),
    PAUS("Paus", "♣", false),
    COPAS("Copas", "♥", true);

    private final String displayName;
    private final String symbol;
    private final boolean red;

    Suit(String displayName, String symbol, boolean red) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.red = red;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isRed() {
        return red;
    }
}
