package com.suecagame.controller;

import com.suecagame.model.Card;
import com.suecagame.model.Suit;

import java.util.List;

public class GameRules {

    public static boolean isValidPlay(Card card, List<Card> hand, Card firstCard) {
        if (firstCard == null) {
            return true;
        }

        boolean hasSuit = false;
        for (Card c : hand) {
            if (c.getSuit() == firstCard.getSuit()) {
                hasSuit = true;
                break;
            }
        }

        if (hasSuit){
            return card.getSuit() == firstCard.getSuit() ;
        }
        return true;
    }

    private static boolean beats(Card challenger, Card current, Suit trump) {
        if (challenger.getSuit() == trump && current.getSuit() != trump) return true;
        if (current.getSuit() == trump && challenger.getSuit() != trump) return false;
        if (challenger.getSuit() == current.getSuit()) {
            return challenger.getValue().getValue() > current.getValue().getValue();
        }
        return false;
    }


    public static int determineWinner(List<Card> playedCards, Suit trump) {
        int winnerIndex = 0;
        for (int i = 1; i < playedCards.size(); i++) {
            if (beats(playedCards.get(i), playedCards.get(winnerIndex), trump)) {
                winnerIndex = i;
            }
        }
        return winnerIndex;
    }

    public static int calculateRoundScore(List<Card> playedCards) {
        int totalPoints = 0;
        for (int i = 0; i < playedCards.size(); i++) {
            totalPoints += playedCards.get(i).getValue().getPoints();
        }
    return totalPoints;
    }

    public static int calculateGamePoints(int roundPoints) {

        if  (roundPoints >= 61 && roundPoints <= 90) {
            return  1;
        }
        else if (roundPoints >= 91 && roundPoints <= 119) {
            return  2;
        }
        else if(roundPoints == 120){
            return  4;
        }
        return  0;
    }


}
