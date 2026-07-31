package com.suecagame.events;

import com.suecagame.model.Card;

import java.util.List;

/**
 * Eventos emitidos pelo motor de jogo (padrão Observer).
 * Interface selada: o compilador garante que todos os tipos de evento
 * são conhecidos, o que permite switch exaustivo nas camadas de UI e rede.
 */
public sealed interface GameEvent {

    record PlayerJoined(int seat, String name) implements GameEvent {
    }

    record GameStarted(List<String> playerNames) implements GameEvent {
    }

    record HandDealt(int seat, List<Card> hand) implements GameEvent {
    }

    record TrumpRevealed(Card trumpCard, int dealerSeat) implements GameEvent {
    }

    record TurnStarted(int seat) implements GameEvent {
    }

    record CardPlayed(int seat, Card card) implements GameEvent {
    }

    record InvalidPlay(int seat, String reason) implements GameEvent {
    }

    record TrickCompleted(int winnerSeat, int points) implements GameEvent {
    }

    record RoundEnded(int team1RoundPoints, int team2RoundPoints,
                      int team1GamePoints, int team2GamePoints) implements GameEvent {
    }

    record GameOver(String winnerTeamName) implements GameEvent {
    }

    record PhaseChanged(String phase) implements GameEvent {
    }
}
