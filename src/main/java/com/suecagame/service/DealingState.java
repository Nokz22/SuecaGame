package com.suecagame.service;

import com.suecagame.events.GameEvent;
import com.suecagame.model.Card;
import com.suecagame.model.Deck;
import com.suecagame.model.DeckFactory;
import com.suecagame.model.Game;
import com.suecagame.model.Player;

import java.util.List;

/**
 * Distribuição: baralha, dá 10 cartas a cada jogador começando à esquerda
 * do carteador, e o trunfo é a última carta dada (fica com o carteador).
 */
class DealingState extends GameState {

    @Override
    GamePhase phase() {
        return GamePhase.DEALING;
    }

    @Override
    void onEnter(GameService service) {
        Game game = service.getGame();
        game.resetForNewRound();

        Deck deck = DeckFactory.createSuecaDeck();
        deck.shuffle(service.getRandom());
        game.setDeck(deck);

        int dealer = game.getDealerIndex();
        for (int i = 1; i <= Game.NUM_PLAYERS; i++) {
            int seat = (dealer + i) % Game.NUM_PLAYERS;
            List<Card> cards = deck.distribute(Game.TRICKS_PER_ROUND);
            if (seat == dealer) {
                game.setTrumpCard(cards.get(cards.size() - 1));
            }
            Player player = game.playerAt(seat);
            player.receiveCards(cards);
            player.sortHand();
        }

        for (int seat = 0; seat < Game.NUM_PLAYERS; seat++) {
            service.emit(new GameEvent.HandDealt(seat, List.copyOf(game.playerAt(seat).getHand())));
        }
        service.emit(new GameEvent.TrumpRevealed(game.getTrumpCard(), dealer));

        game.startNewTrick((dealer + 1) % Game.NUM_PLAYERS);
        service.setState(new PlayingState());
    }
}
