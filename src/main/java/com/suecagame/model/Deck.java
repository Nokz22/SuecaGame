package com.suecagame.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<Card> card;
    public Deck() {
        this.card = new ArrayList<>();

        for (Suit suit : Suit.values()){
            for (Value value : Value.values()) {
                this.card.add(new Card(suit, value));

            }
        }
    }

    public void shuffle() {
        Collections.shuffle(card);
    }

    public List<Card> distribute(int quantity) {
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            result.add(card.remove(0));
        }
        return result;
    }

    public Card getLastCard() {
        return card.get(card.size() - 1);
    }

}
