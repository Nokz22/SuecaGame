package com.suecagame.network;

import com.suecagame.commands.Command;
import com.suecagame.commands.CommandFactory;
import com.suecagame.events.GameEvent;
import com.suecagame.events.GameEventListener;
import com.suecagame.model.Player;
import com.suecagame.service.GamePhase;
import com.suecagame.service.GameService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Servidor LAN: é o único dono do GameService (autoridade sobre o jogo).
 * Observa os eventos do motor e reencaminha-os aos clientes — as mãos
 * e os erros de jogada só vão para o jogador a quem dizem respeito.
 * Lugares vazios são preenchidos com bots quando o anfitrião começa.
 */
public class SuecaServer implements GameEventListener {

    private static final long BOT_DELAY_MS = 800;

    private final int port;
    private final GameService service;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService botScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sueca-bot");
        t.setDaemon(true);
        return t;
    });
    private ServerSocket serverSocket;
    private volatile boolean running;

    public SuecaServer(int port) {
        this.port = port;
        this.service = new GameService();
        this.service.addListener(this);
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "sueca-server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    public int getPort() {
        return serverSocket == null ? port : serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, this);
                Thread t = new Thread(handler, "sueca-client-" + socket.getRemoteSocketAddress());
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Erro a aceitar ligação: " + e.getMessage());
                }
            }
        }
    }

    void handleMessage(ClientHandler handler, String line) {
        String[] parts = line.trim().split("\\|", 2);

        if (Protocol.JOIN.equals(parts[0])) {
            handleJoin(handler, parts.length > 1 ? parts[1] : "");
            return;
        }
        if (handler.getSeat() < 0) {
            return; // ainda não fez JOIN
        }
        Command command = CommandFactory.fromMessage(line);
        if (command != null) {
            command.execute(service, handler.getSeat());
        }
    }

    private synchronized void handleJoin(ClientHandler handler, String rawName) {
        if (handler.getSeat() >= 0) {
            return; // já está sentado
        }
        if (service.getPhase() != GamePhase.WAITING_FOR_PLAYERS || service.getGame().isFull()) {
            handler.send("FULL");
            handler.close();
            return;
        }
        String name = Protocol.sanitizeName(rawName);
        // enviar a quem entra a lista dos que já estão sentados
        List<Player> seated = service.getGame().getPlayers();
        for (int seat = 0; seat < seated.size(); seat++) {
            handler.send(Protocol.encode(new GameEvent.PlayerJoined(seat, seated.get(seat).getName())));
        }
        // entra na lista antes do addPlayer para receber o próprio evento JOINED
        clients.add(handler);
        int seat = service.addPlayer(name, Player.Type.HUMAN);
        handler.setSeat(seat);
        handler.send(Protocol.WELCOME + "|" + seat);
    }

    @Override
    public void onGameEvent(GameEvent event) {
        switch (event) {
            case GameEvent.HandDealt e -> sendToSeat(e.seat(), Protocol.encode(e));
            case GameEvent.InvalidPlay e -> sendToSeat(e.seat(), Protocol.encode(e));
            case GameEvent.TurnStarted e -> {
                broadcast(Protocol.encode(e));
                scheduleBotIfNeeded();
            }
            default -> broadcast(Protocol.encode(event));
        }
    }

    private void scheduleBotIfNeeded() {
        if (service.isBotTurn()) {
            botScheduler.schedule(service::playBotTurn, BOT_DELAY_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
    }

    private void sendToSeat(int seat, String message) {
        for (ClientHandler client : clients) {
            if (client.getSeat() == seat) {
                client.send(message);
            }
        }
    }

    void onClientDisconnected(ClientHandler handler) {
        clients.remove(handler);
        int seat = handler.getSeat();
        if (seat < 0 || service.getPhase() == GamePhase.GAME_OVER) {
            return;
        }
        // o jogador que caiu passa a bot para o jogo poder continuar
        Player player = service.getGame().playerAt(seat);
        player.setType(Player.Type.BOT);
        player.setName(player.getName() + " (bot)");
        scheduleBotIfNeeded();
    }

    public void stop() {
        running = false;
        botScheduler.shutdownNow();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // já fechado
        }
        clients.forEach(ClientHandler::close);
        clients.clear();
    }
}
