package com.suecagame.controller;

import com.suecagame.model.Card;
import com.suecagame.model.Suit;
import com.suecagame.model.Value;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameRulesTest {

    private final Card asCopas = new Card(Suit.COPAS, Value.ÀS);
    private final Card seteCopas = new Card(Suit.COPAS, Value.SETE);
    private final Card doisCopas = new Card(Suit.COPAS, Value.DOIS);
    private final Card reiOuros = new Card(Suit.OUROS, Value.REI);
    private final Card doisPaus = new Card(Suit.PAUS, Value.DOIS);

    @Test
    void anyCardIsValidWhenLeading() {
        assertTrue(GameRules.isValidPlay(reiOuros, List.of(reiOuros, asCopas), null));
    }

    @Test
    void mustFollowSuitWhenPossible() {
        List<Card> hand = List.of(doisCopas, reiOuros);
        assertTrue(GameRules.isValidPlay(doisCopas, hand, asCopas));
        assertFalse(GameRules.isValidPlay(reiOuros, hand, asCopas));
    }

    @Test
    void anyCardIsValidWithoutLeadSuit() {
        List<Card> hand = List.of(reiOuros, doisPaus);
        assertTrue(GameRules.isValidPlay(reiOuros, hand, asCopas));
        assertTrue(GameRules.isValidPlay(doisPaus, hand, asCopas));
    }

    @Test
    void highestOfLeadSuitWinsWithoutTrump() {
        List<Card> played = List.of(seteCopas, asCopas, doisCopas, reiOuros);
        assertEquals(1, GameRules.determineWinner(played, Suit.ESPADAS));
    }

    @Test
    void trumpBeatsLeadSuit() {
        List<Card> played = List.of(asCopas, seteCopas, doisPaus, reiOuros);
        assertEquals(2, GameRules.determineWinner(played, Suit.PAUS));
    }

    @Test
    void offSuitNonTrumpNeverWins() {
        List<Card> played = List.of(doisCopas, reiOuros);
        assertEquals(0, GameRules.determineWinner(played, Suit.ESPADAS));
    }

    @Test
    void roundScoreSumsCardPoints() {
        assertEquals(25, GameRules.calculateRoundScore(List.of(asCopas, seteCopas, reiOuros, doisPaus)));
    }

    @Test
    void gamePointsBoundaries() {
        assertEquals(0, GameRules.calculateGamePoints(60));
        assertEquals(1, GameRules.calculateGamePoints(61));
        assertEquals(1, GameRules.calculateGamePoints(90));
        assertEquals(2, GameRules.calculateGamePoints(91));
        assertEquals(2, GameRules.calculateGamePoints(119));
        assertEquals(4, GameRules.calculateGamePoints(120));
    }
}
