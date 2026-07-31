package com.suecagame.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory do baralho de Sueca: 40 cartas (sem 8, 9 e 10), 120 pontos no total.
 */
public final class DeckFactory {

    private DeckFactory() {
    }

    public static Deck createSuecaDeck() {
        List<Card> cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Value value : Value.values()) {
                cards.add(new Card(suit, value));
            }
        }
        return new Deck(cards);
    }
}
