package com.suecagame.service.ai;

import com.suecagame.model.Card;
import com.suecagame.model.Suit;
import com.suecagame.model.Trick;

import java.util.List;

/**
 * Estratégia de jogada de um bot (padrão Strategy).
 * Deve devolver sempre uma carta legal da mão recebida.
 */
public interface BotStrategy {

    Card chooseCard(List<Card> hand, Trick trick, Suit trump, int seat);
}
