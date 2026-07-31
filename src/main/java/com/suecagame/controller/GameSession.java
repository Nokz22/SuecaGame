package com.suecagame.controller;

import com.suecagame.events.GameEventListener;
import com.suecagame.model.Card;

import java.util.Map;

/**
 * Abstração de uma sessão de jogo para a UI: a interface gráfica
 * fala sempre com uma GameSession e não sabe se o jogo é local
 * (contra bots) ou remoto (cliente LAN).
 */
public interface GameSession {

    int mySeat();

    /** Só tem efeito para o anfitrião/jogo local. */
    void start();

    void playCard(Card card);

    void addListener(GameEventListener listener);

    /** Snapshot dos jogadores conhecidos (seat → nome). */
    Map<Integer, String> playerNames();

    boolean isHost();

    void close();
}
