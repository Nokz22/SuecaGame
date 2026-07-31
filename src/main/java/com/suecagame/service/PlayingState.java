package com.suecagame.service;

import com.suecagame.controller.GameRules;
import com.suecagame.events.GameEvent;
import com.suecagame.model.Card;
import com.suecagame.model.Game;
import com.suecagame.model.Player;
import com.suecagame.model.Trick;

/**
 * Fase de jogada: valida cada carta (obrigação de assistir ao naipe),
 * fecha vazas, atribui pontos e termina a ronda ao fim de 10 vazas.
 */
class PlayingState extends GameState {

    @Override
    GamePhase phase() {
        return GamePhase.PLAYING;
    }

    @Override
    void onEnter(GameService service) {
        service.emit(new GameEvent.TurnStarted(service.getGame().getCurrentPlayerIndex()));
    }

    @Override
    boolean playCard(GameService service, int seat, Card card) {
        Game game = service.getGame();

        if (seat != game.getCurrentPlayerIndex()) {
            service.emit(new GameEvent.InvalidPlay(seat, "Não é a tua vez de jogar"));
            return false;
        }

        Player player = game.playerAt(seat);
        if (!player.hasCard(card)) {
            service.emit(new GameEvent.InvalidPlay(seat, "Não tens essa carta na mão"));
            return false;
        }

        Trick trick = game.getCurrentTrick();
        if (!GameRules.isValidPlay(card, player.getHand(), trick.firstCard())) {
            service.emit(new GameEvent.InvalidPlay(seat,
                    "Tens de assistir ao naipe de " + trick.leadSuit().getDisplayName()));
            return false;
        }

        player.playCard(card);
        trick.add(seat, card);
        service.emit(new GameEvent.CardPlayed(seat, card));

        if (trick.isComplete()) {
            finishTrick(service, game, trick);
        } else {
            game.advanceTurn();
            service.emit(new GameEvent.TurnStarted(game.getCurrentPlayerIndex()));
        }
        return true;
    }

    private void finishTrick(GameService service, Game game, Trick trick) {
        int winnerIndex = GameRules.determineWinner(trick.cards(), game.getTrump());
        int winnerSeat = trick.playAt(winnerIndex).seat();
        int points = trick.points();

        game.teamOfSeat(winnerSeat).addRoundPoints(points);
        game.trickFinished();
        service.emit(new GameEvent.TrickCompleted(winnerSeat, points));

        if (game.isRoundOver()) {
            service.setState(new ScoringState());
        } else {
            game.startNewTrick(winnerSeat);
            service.emit(new GameEvent.TurnStarted(winnerSeat));
        }
    }
}
