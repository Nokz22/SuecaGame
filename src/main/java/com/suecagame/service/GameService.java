package com.suecagame.service;

import com.suecagame.events.GameEvent;
import com.suecagame.events.GameEventListener;
import com.suecagame.model.Card;
import com.suecagame.model.Game;
import com.suecagame.model.Player;
import com.suecagame.service.ai.BotStrategy;
import com.suecagame.service.ai.SmartBotStrategy;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Motor do jogo: única porta de entrada para mudar o estado da partida.
 * É observável (padrão Observer) — UI e servidor de rede registam-se
 * como listeners e reagem aos eventos emitidos.
 *
 * Os métodos públicos são sincronizados porque em multiplayer chegam
 * jogadas de threads diferentes (uma por cliente ligado).
 */
public class GameService {

    private final Game game;
    private final Random random;
    private final BotStrategy botStrategy;
    private final List<GameEventListener> listeners = new CopyOnWriteArrayList<>();
    private GameState state;

    public GameService() {
        this(new Random());
    }

    public GameService(Random random) {
        this(random, new SmartBotStrategy());
    }

    public GameService(Random random, BotStrategy botStrategy) {
        this.game = new Game();
        this.random = random;
        this.botStrategy = botStrategy;
        this.state = new WaitingForPlayersState();
    }

    //OBSERVER
    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    void emit(GameEvent event) {
        for (GameEventListener listener : listeners) {
            listener.onGameEvent(event);
        }
    }

    //STATE
    void setState(GameState newState) {
        this.state = newState;
        emit(new GameEvent.PhaseChanged(newState.phase().name()));
        newState.onEnter(this);
    }

    public synchronized GamePhase getPhase() {
        return state.phase();
    }

    //ACTIONS
    public synchronized int addPlayer(String name, Player.Type type) {
        return state.addPlayer(this, name, type);
    }

    public synchronized void start() {
        state.start(this);
    }

    public synchronized boolean playCard(int seat, Card card) {
        return state.playCard(this, seat, card);
    }

    /** Joga automaticamente a vez do bot corrente. Devolve false se não for a vez de um bot. */
    public synchronized boolean playBotTurn() {
        if (!isBotTurn()) {
            return false;
        }
        int seat = game.getCurrentPlayerIndex();
        Player bot = game.playerAt(seat);
        Card choice = botStrategy.chooseCard(
                List.copyOf(bot.getHand()), game.getCurrentTrick(), game.getTrump(), seat);
        return state.playCard(this, seat, choice);
    }

    public synchronized boolean isBotTurn() {
        return state.phase() == GamePhase.PLAYING
                && game.playerAt(game.getCurrentPlayerIndex()).isBot();
    }

    //QUERIES
    public Game getGame() {
        return game;
    }

    Random getRandom() {
        return random;
    }

    public synchronized int getCurrentSeat() {
        return game.getCurrentPlayerIndex();
    }

    public synchronized List<Card> handOf(int seat) {
        return List.copyOf(game.playerAt(seat).getHand());
    }
}
