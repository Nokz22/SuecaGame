package com.suecagame.service;

import com.suecagame.controller.GameRules;
import com.suecagame.events.GameEvent;
import com.suecagame.model.Game;
import com.suecagame.model.Team;

/**
 * Contagem: converte os pontos da ronda em jogos
 * (61–90 → 1 jogo, 91–119 → 2 jogos, 120 → 4 jogos de capote)
 * e decide se a partida acabou (primeira equipa a 4 jogos) ou se há nova ronda.
 */
class ScoringState extends GameState {

    @Override
    GamePhase phase() {
        return GamePhase.SCORING;
    }

    @Override
    void onEnter(GameService service) {
        Game game = service.getGame();
        Team team1 = game.getTeam1();
        Team team2 = game.getTeam2();

        team1.addGamePoints(GameRules.calculateGamePoints(team1.getRoundPoints()));
        team2.addGamePoints(GameRules.calculateGamePoints(team2.getRoundPoints()));

        service.emit(new GameEvent.RoundEnded(
                team1.getRoundPoints(), team2.getRoundPoints(),
                team1.getGamePoints(), team2.getGamePoints()));

        if (team1.hasWin() || team2.hasWin()) {
            service.setState(new GameOverState());
        } else {
            game.rotateDealer();
            service.setState(new DealingState());
        }
    }
}
