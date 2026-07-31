package com.suecagame.controller;

import com.suecagame.events.GameEvent;
import com.suecagame.events.GameEventListener;
import com.suecagame.model.Card;
import com.suecagame.model.Player;
import com.suecagame.service.GameService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sessão local (single player): 1 humano no lugar 0 contra 3 bots.
 * Esconde as mãos dos bots e joga as vezes deles com um pequeno atraso,
 * para a partida ser legível na UI.
 */
public class LocalGameSession implements GameSession, GameEventListener {

    private static final long BOT_DELAY_MS = 800;
    private static final int HUMAN_SEAT = 0;

    private final GameService service;
    private final List<GameEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService botScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sueca-local-bot");
        t.setDaemon(true);
        return t;
    });

    public LocalGameSession(String playerName) {
        this.service = new GameService();
        this.service.addListener(this);
        this.service.addPlayer(playerName, Player.Type.HUMAN);
    }

    @Override
    public int mySeat() {
        return HUMAN_SEAT;
    }

    @Override
    public void start() {
        service.start();
    }

    @Override
    public void playCard(Card card) {
        service.playCard(HUMAN_SEAT, card);
    }

    @Override
    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public Map<Integer, String> playerNames() {
        Map<Integer, String> names = new HashMap<>();
        List<Player> players = service.getGame().getPlayers();
        for (int seat = 0; seat < players.size(); seat++) {
            names.put(seat, players.get(seat).getName());
        }
        return names;
    }

    @Override
    public boolean isHost() {
        return true;
    }

    @Override
    public void onGameEvent(GameEvent event) {
        // mesma privacidade que em rede: só passa a mão e os erros do humano
        boolean forward = switch (event) {
            case GameEvent.HandDealt e -> e.seat() == HUMAN_SEAT;
            case GameEvent.InvalidPlay e -> e.seat() == HUMAN_SEAT;
            default -> true;
        };
        if (forward) {
            for (GameEventListener listener : listeners) {
                listener.onGameEvent(event);
            }
        }
        if (event instanceof GameEvent.TurnStarted && service.isBotTurn()) {
            botScheduler.schedule(service::playBotTurn, BOT_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void close() {
        botScheduler.shutdownNow();
    }
}
