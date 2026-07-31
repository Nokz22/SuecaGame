package com.suecagame.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckFactoryTest {

    @Test
    void deckHas40UniqueCards() {
        Deck deck = DeckFactory.createSuecaDeck();
        assertEquals(40, deck.size());

        Set<Card> unique = new HashSet<>(deck.distribute(40));
        assertEquals(40, unique.size());
    }

    @Test
    void deckTotals120Points() {
        Deck deck = DeckFactory.createSuecaDeck();
        List<Card> all = deck.distribute(40);
        int total = all.stream().mapToInt(c -> c.getValue().getPoints()).sum();
        assertEquals(120, total);
    }

    @Test
    void distributeRemovesCards() {
        Deck deck = DeckFactory.createSuecaDeck();
        List<Card> hand = deck.distribute(10);
        assertEquals(10, hand.size());
        assertEquals(30, deck.size());
    }

    @Test
    void cardIdRoundTrip() {
        Card card = new Card(Suit.COPAS, Value.ÀS);
        assertEquals(card, Card.fromId(card.id()));
        assertTrue(card.id().contains("COPAS"));
    }
}
