package com.suecagame.model;

import java.util.ArrayList;
import java.util.List;

public class Game {

    //ATTRIBUTES
    private Deck deck;
    private List<Player> players;
    private Team team1;
    private Team team2;
    private Suit trump;
    private int currentPlayerIndex;

    //CONSTRUCTOR
    public Game() {
        this.deck = new Deck();
        this.players = new ArrayList<>();
        this.trump = null;
        this.currentPlayerIndex = 0;
        this.team1 = new Team("Equipa 1");
        this.team2 = new Team("Equipa 2");

    }


    //GETTERS
    public Deck getDeck() {
        return deck;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Team getTeam1() {
        return team1;
    }

    public Team getTeam2() {
        return team2;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public Suit getTrump() {
        return trump;
    }

}
