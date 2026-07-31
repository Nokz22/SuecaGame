package com.suecagame.network;

import com.suecagame.events.GameEvent;
import com.suecagame.events.GameEventListener;
import com.suecagame.model.Card;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Cliente LAN: liga-se ao servidor, traduz as linhas do protocolo de volta
 * em GameEvents e notifica os listeners — a UI observa um cliente remoto
 * exatamente da mesma forma que observa um jogo local.
 */
public class SuecaClient {

    private final List<GameEventListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<Integer, String> players = new ConcurrentHashMap<>();
    private Socket socket;
    private PrintWriter out;
    private volatile int mySeat = -1;
    private volatile boolean connected;
    private final CountDownLatch welcomeLatch = new CountDownLatch(1);

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Liga-se e faz JOIN. Bloqueia até o servidor atribuir um lugar
     * (ou lança IOException se a mesa estiver cheia / o servidor não responder).
     */
    public void connect(String host, int port, String name) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        connected = true;

        Thread reader = new Thread(() -> readLoop(in), "sueca-client-reader");
        reader.setDaemon(true);
        reader.start();

        out.println(Protocol.JOIN + "|" + Protocol.sanitizeName(name));
        try {
            if (!welcomeLatch.await(10, TimeUnit.SECONDS)) {
                close();
                throw new IOException("O servidor não respondeu");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ligação interrompida", e);
        }
        if (mySeat < 0) {
            close();
            throw new IOException("A mesa já está completa");
        }
    }

    private void readLoop(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                handleLine(line);
            }
        } catch (IOException e) {
            // ligação terminada
        } finally {
            connected = false;
            welcomeLatch.countDown();
        }
    }

    private void handleLine(String line) {
        String[] parts = line.split("\\|");
        if (Protocol.WELCOME.equals(parts[0]) && parts.length == 2) {
            mySeat = Integer.parseInt(parts[1]);
            welcomeLatch.countDown();
            return;
        }
        if ("FULL".equals(parts[0])) {
            mySeat = -1;
            welcomeLatch.countDown();
            return;
        }
        GameEvent event = Protocol.decodeEvent(line);
        if (event != null) {
            if (event instanceof GameEvent.PlayerJoined joined) {
                players.put(joined.seat(), joined.name());
            }
            for (GameEventListener listener : listeners) {
                listener.onGameEvent(event);
            }
        }
    }

    /** Snapshot dos jogadores conhecidos (seat → nome), para a UI montar o lobby. */
    public Map<Integer, String> getPlayers() {
        return Map.copyOf(players);
    }

    public void sendPlay(Card card) {
        send("PLAY|" + card.id());
    }

    public void sendStart() {
        send("START");
    }

    private void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public int getMySeat() {
        return mySeat;
    }

    public boolean isConnected() {
        return connected;
    }

    public void close() {
        connected = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // já fechado
        }
    }
}
