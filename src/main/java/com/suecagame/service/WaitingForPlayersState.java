package com.suecagame.service;

import com.suecagame.events.GameEvent;
import com.suecagame.model.Game;
import com.suecagame.model.Player;

/**
 * Fase de espera: aceita jogadores até a mesa encher.
 * Ao começar, os lugares vazios são preenchidos com bots.
 */
class WaitingForPlayersState extends GameState {

    @Override
    GamePhase phase() {
        return GamePhase.WAITING_FOR_PLAYERS;
    }

    @Override
    int addPlayer(GameService service, String name, Player.Type type) {
        Game game = service.getGame();
        if (game.isFull()) {
            throw new IllegalStateException("A mesa já está completa");
        }
        int seat = game.addPlayer(new Player(name, type));
        service.emit(new GameEvent.PlayerJoined(seat, name));
        return seat;
    }

    @Override
    void start(GameService service) {
        Game game = service.getGame();
        if (game.getPlayers().isEmpty()) {
            throw new IllegalStateException("É preciso pelo menos um jogador para começar");
        }
        int botNumber = 1;
        while (!game.isFull()) {
            addPlayer(service, "Bot " + botNumber++, Player.Type.BOT);
        }
        service.emit(new GameEvent.GameStarted(
                game.getPlayers().stream().map(Player::getName).toList()));
        service.setState(new DealingState());
    }
}
