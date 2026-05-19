package com.suecagame.model;

import java.util.ArrayList;
import java.util.List;

public class Team {

//ATTRIBUTES
    private String name;
    private List<Player> players;
    private int roundPoints;
    private int gamePoints;


//CONSTRUCTOR
    public Team(String name) {
        this.name = name;
        this.players = new ArrayList<>();
        this.roundPoints = 0;
        this.gamePoints = 0;
    }

//GETTERS + SETTERS
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public int getRoundPoints() {
        return roundPoints;
    }

    public void setRoundPoints(int roundPoints) {
        this.roundPoints = roundPoints;
    }

    public int getGamePoints() {
        return gamePoints;
    }

    public void setGamePoints(int gamePoints) {
        this.gamePoints = gamePoints;
    }

//METHODS

    public void  addPlayer(Player player) {
        this.players.add(player);

    }

    public void addRoundPoints(int roundPoints) {
         this.roundPoints += roundPoints;
    }

    public void addGamePoints(int  gamePoints) {
        this.gamePoints += gamePoints;
    }

    public boolean hasWin(){
        if(this.gamePoints >= 4){
            return true;
        }
        return false;
    }

}
