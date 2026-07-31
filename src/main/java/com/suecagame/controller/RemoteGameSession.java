package com.suecagame.controller;

import com.suecagame.events.GameEventListener;
import com.suecagame.model.Card;
import com.suecagame.network.SuecaClient;
import com.suecagame.network.SuecaServer;

import java.io.IOException;
import java.util.Map;

/**
 * Sessão remota (multiplayer LAN): encaminha as ações para o servidor
 * através de um SuecaClient. Quando esta sessão é o anfitrião, também
 * é dona do servidor embebido e fecha-o no fim.
 */
public class RemoteGameSession implements GameSession {

    private final SuecaClient client;
    private final SuecaServer embeddedServer;

    /** Junta-se a um servidor já existente na rede. */
    public static RemoteGameSession join(String host, int port, String playerName) throws IOException {
        SuecaClient client = new SuecaClient();
        RemoteGameSession session = new RemoteGameSession(client, null);
        client.connect(host, port, playerName);
        return session;
    }

    /** Cria um servidor local e liga-se a ele como anfitrião (seat 0). */
    public static RemoteGameSession host(int port, String playerName) throws IOException {
        SuecaServer server = new SuecaServer(port);
        server.start();
        SuecaClient client = new SuecaClient();
        RemoteGameSession session = new RemoteGameSession(client, server);
        try {
            client.connect("localhost", server.getPort(), playerName);
        } catch (IOException e) {
            server.stop();
            throw e;
        }
        return session;
    }

    private RemoteGameSession(SuecaClient client, SuecaServer embeddedServer) {
        this.client = client;
        this.embeddedServer = embeddedServer;
    }

    @Override
    public int mySeat() {
        return client.getMySeat();
    }

    @Override
    public void start() {
        client.sendStart();
    }

    @Override
    public void playCard(Card card) {
        client.sendPlay(card);
    }

    @Override
    public void addListener(GameEventListener listener) {
        client.addListener(listener);
    }

    @Override
    public Map<Integer, String> playerNames() {
        return client.getPlayers();
    }

    @Override
    public boolean isHost() {
        return embeddedServer != null;
    }

    @Override
    public void close() {
        client.close();
        if (embeddedServer != null) {
            embeddedServer.stop();
        }
    }
}
