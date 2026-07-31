package com.suecagame.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Uma vaza: as cartas jogadas na mesa e quem as jogou, pela ordem de jogada.
 */
public class Trick {

    public record Play(int seat, Card card) {
    }

    public static final int SIZE = 4;

    private final List<Play> plays = new ArrayList<>(SIZE);

    public void add(int seat, Card card) {
        if (isComplete()) {
            throw new IllegalStateException("A vaza já está completa");
        }
        plays.add(new Play(seat, card));
    }

    public boolean isEmpty() {
        return plays.isEmpty();
    }

    public boolean isComplete() {
        return plays.size() == SIZE;
    }

    public int size() {
        return plays.size();
    }

    public Card firstCard() {
        return plays.isEmpty() ? null : plays.get(0).card();
    }

    public Suit leadSuit() {
        Card first = firstCard();
        return first == null ? null : first.getSuit();
    }

    public Play playAt(int index) {
        return plays.get(index);
    }

    public List<Play> plays() {
        return List.copyOf(plays);
    }

    public List<Card> cards() {
        return plays.stream().map(Play::card).toList();
    }

    public int points() {
        return plays.stream().mapToInt(p -> p.card().getValue().getPoints()).sum();
    }
}
