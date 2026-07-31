package com.suecagame.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrickTest {

    @Test
    void emptyTrickHasNoLeadSuit() {
        Trick trick = new Trick();
        assertTrue(trick.isEmpty());
        assertNull(trick.firstCard());
        assertNull(trick.leadSuit());
    }

    @Test
    void leadSuitIsFirstCardSuit() {
        Trick trick = new Trick();
        trick.add(2, new Card(Suit.PAUS, Value.REI));
        trick.add(3, new Card(Suit.COPAS, Value.ÀS));
        assertEquals(Suit.PAUS, trick.leadSuit());
        assertFalse(trick.isComplete());
    }

    @Test
    void completeAfterFourPlaysAndSumsPoints() {
        Trick trick = new Trick();
        trick.add(0, new Card(Suit.OUROS, Value.ÀS));      // 11
        trick.add(1, new Card(Suit.OUROS, Value.SETE));    // 10
        trick.add(2, new Card(Suit.OUROS, Value.DOIS));    // 0
        trick.add(3, new Card(Suit.OUROS, Value.DAMA));    // 2
        assertTrue(trick.isComplete());
        assertEquals(23, trick.points());
        assertThrows(IllegalStateException.class,
                () -> trick.add(0, new Card(Suit.COPAS, Value.TRES)));
    }
}
