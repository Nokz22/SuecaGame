package com.suecagame.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {

    private final List<Card> cards;

    public Deck(List<Card> cards) {
        this.cards = new ArrayList<>(cards);
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public void shuffle(Random random) {
        Collections.shuffle(cards, random);
    }

    public List<Card> distribute(int quantity) {
        List<Card> result = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            result.add(cards.remove(0));
        }
        return result;
    }

    public Card getLastCard() {
        return cards.get(cards.size() - 1);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
