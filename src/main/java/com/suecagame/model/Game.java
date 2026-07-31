package com.suecagame.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado de domínio de uma partida de Sueca: 4 jogadores, 2 equipas
 * (lugares 0 e 2 contra lugares 1 e 3), trunfo e vaza corrente.
 */
public class Game {

    public static final int NUM_PLAYERS = 4;
    public static final int TRICKS_PER_ROUND = 10;

    //ATTRIBUTES
    private final List<Player> players;
    private final Team team1;
    private final Team team2;
    private Deck deck;
    private Card trumpCard;
    private int dealerIndex;
    private int currentPlayerIndex;
    private Trick currentTrick;
    private int tricksPlayed;

    //CONSTRUCTOR
    public Game() {
        this.players = new ArrayList<>(NUM_PLAYERS);
        this.team1 = new Team("Equipa 1");
        this.team2 = new Team("Equipa 2");
        this.deck = null;
        this.trumpCard = null;
        this.dealerIndex = 0;
        this.currentPlayerIndex = 0;
        this.currentTrick = new Trick();
        this.tricksPlayed = 0;
    }

    //GETTERS
    public List<Player> getPlayers() {
        return players;
    }

    public Player playerAt(int seat) {
        return players.get(seat);
    }

    public Team getTeam1() {
        return team1;
    }

    public Team getTeam2() {
        return team2;
    }

    /** Lugares 0 e 2 pertencem à Equipa 1; lugares 1 e 3 à Equipa 2. */
    public Team teamOfSeat(int seat) {
        return seat % 2 == 0 ? team1 : team2;
    }

    public Deck getDeck() {
        return deck;
    }

    public Card getTrumpCard() {
        return trumpCard;
    }

    public Suit getTrump() {
        return trumpCard == null ? null : trumpCard.getSuit();
    }

    public int getDealerIndex() {
        return dealerIndex;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public Trick getCurrentTrick() {
        return currentTrick;
    }

    public int getTricksPlayed() {
        return tricksPlayed;
    }

    //SETTERS / MUTATORS
    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public void setTrumpCard(Card trumpCard) {
        this.trumpCard = trumpCard;
    }

    public void setDealerIndex(int dealerIndex) {
        this.dealerIndex = dealerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    //METHODS
    public int addPlayer(Player player) {
        if (players.size() >= NUM_PLAYERS) {
            throw new IllegalStateException("A mesa já está completa");
        }
        players.add(player);
        int seat = players.size() - 1;
        teamOfSeat(seat).addPlayer(player);
        return seat;
    }

    public boolean isFull() {
        return players.size() == NUM_PLAYERS;
    }

    public void advanceTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % NUM_PLAYERS;
    }

    public void rotateDealer() {
        dealerIndex = (dealerIndex + 1) % NUM_PLAYERS;
    }

    public void startNewTrick(int leaderSeat) {
        currentTrick = new Trick();
        currentPlayerIndex = leaderSeat;
    }

    public void trickFinished() {
        tricksPlayed++;
    }

    public boolean isRoundOver() {
        return tricksPlayed >= TRICKS_PER_ROUND;
    }

    public void resetForNewRound() {
        tricksPlayed = 0;
        currentTrick = new Trick();
        trumpCard = null;
        team1.setRoundPoints(0);
        team2.setRoundPoints(0);
        players.forEach(Player::clearHand);
    }
}
