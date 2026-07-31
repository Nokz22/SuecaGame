package com.suecagame.service;

import com.suecagame.events.GameEvent;
import com.suecagame.model.Card;
import com.suecagame.model.Player;

/**
 * Padrão State: cada fase do jogo define o que é permitido fazer.
 * Ações fora da fase certa são rejeitadas com um evento InvalidPlay,
 * em vez de espalhar ifs de fase pelo serviço.
 */
abstract class GameState {

    abstract GamePhase phase();

    void onEnter(GameService service) {
        // por omissão, nada a fazer ao entrar na fase
    }

    int addPlayer(GameService service, String name, Player.Type type) {
        throw new IllegalStateException("Não é possível juntar jogadores na fase " + phase());
    }

    void start(GameService service) {
        throw new IllegalStateException("Não é possível começar o jogo na fase " + phase());
    }

    boolean playCard(GameService service, int seat, Card card) {
        service.emit(new GameEvent.InvalidPlay(seat, "Não podes jogar agora (fase: " + phase() + ")"));
        return false;
    }
}
