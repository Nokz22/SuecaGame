package com.suecagame.service.ai;

import com.suecagame.controller.GameRules;
import com.suecagame.model.Card;
import com.suecagame.model.Suit;
import com.suecagame.model.Trick;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Bot com heurísticas simples de Sueca:
 * - a abrir a vaza, sai com a carta mais fraca (evitando gastar trunfos);
 * - se o parceiro vai a ganhar, carrega a vaza com pontos sem o cortar;
 * - se o adversário vai a ganhar, tenta ganhar com a carta mais barata
 *   (ou com a mais valiosa, se for o último a jogar);
 * - se não pode ganhar, desfaz-se da carta com menos pontos.
 */
public class SmartBotStrategy implements BotStrategy {

    @Override
    public Card chooseCard(List<Card> hand, Trick trick, Suit trump, int seat) {
        List<Card> legal = legalCards(hand, trick);

        if (trick.isEmpty()) {
            return lead(legal, trump);
        }

        int currentWinnerSeat = currentWinnerSeat(trick, trump);
        boolean partnerWinning = currentWinnerSeat == (seat + 2) % Trick.SIZE;

        List<Card> winning = legal.stream()
                .filter(c -> wouldWin(c, trick, trump))
                .toList();

        if (partnerWinning) {
            return supportPartner(legal, winning, trick, trump);
        }
        if (!winning.isEmpty()) {
            boolean lastToPlay = trick.size() == Trick.SIZE - 1;
            return lastToPlay
                    ? best(winning, byPointsThenWeakness(trick, trump))
                    : best(winning, byWeakness(trick, trump));
        }
        return discard(legal);
    }

    private List<Card> legalCards(List<Card> hand, Trick trick) {
        List<Card> legal = new ArrayList<>();
        for (Card card : hand) {
            if (GameRules.isValidPlay(card, hand, trick.firstCard())) {
                legal.add(card);
            }
        }
        return legal;
    }

    private Card lead(List<Card> legal, Suit trump) {
        List<Card> nonTrump = legal.stream().filter(c -> c.getSuit() != trump).toList();
        List<Card> pool = nonTrump.isEmpty() ? legal : nonTrump;
        return best(pool, Comparator
                .comparingInt((Card c) -> c.getValue().getPoints())
                .thenComparingInt(c -> c.getValue().getValue()));
    }

    private Card supportPartner(List<Card> legal, List<Card> winning, Trick trick, Suit trump) {
        List<Card> nonOvertaking = legal.stream().filter(c -> !winning.contains(c)).toList();
        if (!nonOvertaking.isEmpty()) {
            // carregar a vaza: máximo de pontos, e entre iguais a carta mais fraca
            return best(nonOvertaking, Comparator
                    .comparingInt((Card c) -> -c.getValue().getPoints())
                    .thenComparingInt(c -> c.getValue().getValue()));
        }
        return best(winning, byWeakness(trick, trump));
    }

    private Card discard(List<Card> legal) {
        return best(legal, Comparator
                .comparingInt((Card c) -> c.getValue().getPoints())
                .thenComparingInt(c -> c.getValue().getValue()));
    }

    private int currentWinnerSeat(Trick trick, Suit trump) {
        int winnerIndex = GameRules.determineWinner(trick.cards(), trump);
        return trick.playAt(winnerIndex).seat();
    }

    /** Simula a jogada: a carta ganharia a vaza tal como está? */
    private boolean wouldWin(Card candidate, Trick trick, Suit trump) {
        List<Card> cards = new ArrayList<>(trick.cards());
        cards.add(candidate);
        return GameRules.determineWinner(cards, trump) == cards.size() - 1;
    }

    /** Força da carta no contexto da vaza: trunfos acima do naipe de saída, resto abaixo. */
    private int strength(Card card, Trick trick, Suit trump) {
        int base = card.getValue().getValue();
        if (card.getSuit() == trump) {
            return 200 + base;
        }
        if (trick.leadSuit() != null && card.getSuit() == trick.leadSuit()) {
            return 100 + base;
        }
        return base;
    }

    private Comparator<Card> byWeakness(Trick trick, Suit trump) {
        return Comparator.comparingInt(c -> strength(c, trick, trump));
    }

    private Comparator<Card> byPointsThenWeakness(Trick trick, Suit trump) {
        return Comparator
                .comparingInt((Card c) -> -c.getValue().getPoints())
                .thenComparingInt(c -> strength(c, trick, trump));
    }

    private Card best(List<Card> cards, Comparator<Card> order) {
        return cards.stream().min(order).orElseThrow();
    }
}
