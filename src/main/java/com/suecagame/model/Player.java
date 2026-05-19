package com.suecagame.model;

import java.util.ArrayList;
import java.util.List;

public class Player {

    //ENUM
    public enum Type {
        HUMAN,
        BOT
    }

    //ATTRIBUTES
    private String name;
    private List<Card> hand;
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

    //Methods
    public void receiveCard(Card card) {
        hand.add(card);

    }

    public Card playCard(Card card) {
        this.hand.remove(card);
        return card;
    }


}
