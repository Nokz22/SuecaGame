package com.suecagame.service;

import com.suecagame.events.GameEvent;
import com.suecagame.model.Game;
import com.suecagame.model.Team;

/**
 * Fase terminal: anuncia a equipa vencedora e não aceita mais jogadas.
 */
class GameOverState extends GameState {

    @Override
    GamePhase phase() {
        return GamePhase.GAME_OVER;
    }

    @Override
    void onEnter(GameService service) {
        Game game = service.getGame();
        Team winner = game.getTeam1().hasWin() ? game.getTeam1() : game.getTeam2();
        service.emit(new GameEvent.GameOver(winner.getName()));
    }
}
