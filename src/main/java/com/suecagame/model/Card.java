package com.suecagame.model;

import java.util.Objects;

public class Card {
    private final Suit suit;
    private final Value value;

    public Card(Suit suit, Value value) {
        this.suit = suit;
        this.value = value;
    }

    public Suit getSuit() {
        return suit;
    }

    public Value getValue() {
        return value;
    }

    /**
     * Identificador estável para serialização (protocolo de rede).
     * Ex.: "COPAS:ÀS"
     */
    public String id() {
        return suit.name() + ":" + value.name();
    }

    public static Card fromId(String id) {
        String[] parts = id.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Carta inválida: " + id);
        }
        return new Card(Suit.valueOf(parts[0]), Value.valueOf(parts[1]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Card card)) {
            return false;
        }
        return suit == card.suit && value == card.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, value);
    }

    @Override
    public String toString() {
        return value.getDisplayName() + " de " + suit.getDisplayName();
    }
}
