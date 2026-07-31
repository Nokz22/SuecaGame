package com.suecagame.events;

/**
 * Observador de eventos do jogo. Implementado pela UI (consola e JavaFX)
 * e pelo servidor de rede, que reencaminha os eventos para os clientes.
 */
@FunctionalInterface
public interface GameEventListener {

    void onGameEvent(GameEvent event);
}
