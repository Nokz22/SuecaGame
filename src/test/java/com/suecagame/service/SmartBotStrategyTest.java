package com.suecagame.service;

import com.suecagame.controller.GameRules;
import com.suecagame.model.Card;
import com.suecagame.model.Suit;
import com.suecagame.model.Trick;
import com.suecagame.model.Value;
import com.suecagame.service.ai.SmartBotStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartBotStrategyTest {

    private final SmartBotStrategy strategy = new SmartBotStrategy();

    @Test
    void alwaysFollowsSuitWhenPossible() {
        Trick trick = new Trick();
        trick.add(0, new Card(Suit.COPAS, Value.DAMA));

        List<Card> hand = List.of(
                new Card(Suit.COPAS, Value.DOIS),
                new Card(Suit.OUROS, Value.ÀS),
                new Card(Suit.PAUS, Value.SETE));

        Card chosen = strategy.chooseCard(hand, trick, Suit.ESPADAS, 1);
        assertEquals(Suit.COPAS, chosen.getSuit());
        assertTrue(GameRules.isValidPlay(chosen, hand, trick.firstCard()));
    }

    @Test
    void beatsOpponentWithCheapestWinningCard() {
        Trick trick = new Trick();
        trick.add(0, new Card(Suit.COPAS, Value.DAMA));

        List<Card> hand = List.of(
                new Card(Suit.COPAS, Value.ÀS),
                new Card(Suit.COPAS, Value.REI),
                new Card(Suit.COPAS, Value.DOIS));

        // ganha com o Rei (mais barato que o Ás) em vez de gastar o Ás
        Card chosen = strategy.chooseCard(hand, trick, Suit.ESPADAS, 1);
        assertEquals(Value.REI, chosen.getValue());
    }

    @Test
    void loadsTrickWithPointsWhenPartnerIsWinning() {
        Trick trick = new Trick();
        trick.add(0, new Card(Suit.COPAS, Value.ÀS));   // parceiro do lugar 2 vai a ganhar
        trick.add(1, new Card(Suit.COPAS, Value.DOIS));

        List<Card> hand = List.of(
                new Card(Suit.COPAS, Value.SETE),
                new Card(Suit.COPAS, Value.TRES));

        // carrega a vaza com o 7 (10 pontos) porque o parceiro segura a vaza
        Card chosen = strategy.chooseCard(hand, trick, Suit.ESPADAS, 2);
        assertEquals(Value.SETE, chosen.getValue());
    }

    @Test
    void discardsLowestPointsWhenCannotWin() {
        Trick trick = new Trick();
        trick.add(0, new Card(Suit.COPAS, Value.ÀS));

        List<Card> hand = List.of(
                new Card(Suit.OUROS, Value.SETE),
                new Card(Suit.OUROS, Value.QUATRO));

        // sem copas nem trunfo: desfaz-se da carta sem pontos
        Card chosen = strategy.chooseCard(hand, trick, Suit.ESPADAS, 1);
        assertEquals(Value.QUATRO, chosen.getValue());
    }
}
