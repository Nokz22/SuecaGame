package com.suecagame.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Player {

    //ENUM
    public enum Type {
        HUMAN,
        BOT
    }

    //ATTRIBUTES
    private String name;
    private final List<Card> hand;
    private Type type;

    //CONSTRUCTOR
    public Player(String name, Type type) {
        this.name = name;
        this.type = type;
        this.hand = new ArrayList<>();
    }

    //GETTERS + SETTERS
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isBot() {
        return type == Type.BOT;
    }

    //METHODS
    public void receiveCard(Card card) {
        hand.add(card);
    }

    public void receiveCards(List<Card> cards) {
        hand.addAll(cards);
    }

    public Card playCard(Card card) {
        if (!hand.remove(card)) {
            throw new IllegalArgumentException(name + " não tem a carta " + card);
        }
        return card;
    }

    public boolean hasCard(Card card) {
        return hand.contains(card);
    }

    public boolean hasSuit(Suit suit) {
        return hand.stream().anyMatch(c -> c.getSuit() == suit);
    }

    public void clearHand() {
        hand.clear();
    }

    public void sortHand() {
        hand.sort(Comparator
                .comparing((Card c) -> c.getSuit().ordinal())
                .thenComparing(c -> -c.getValue().getValue()));
    }
}
