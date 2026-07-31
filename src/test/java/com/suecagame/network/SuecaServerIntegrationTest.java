package com.suecagame.network;

import com.suecagame.events.GameEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integração real: servidor e cliente ligados por socket
 * em porta efémera, do JOIN até às cartas na mão.
 */
class SuecaServerIntegrationTest {

    private SuecaServer server;
    private SuecaClient client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void clientJoinsStartsAndReceivesHand() throws IOException, InterruptedException {
        server = new SuecaServer(0); // porta efémera
        server.start();

        ConcurrentLinkedQueue<GameEvent> events = new ConcurrentLinkedQueue<>();
        CountDownLatch handReceived = new CountDownLatch(1);

        client = new SuecaClient();
        client.addListener(event -> {
            events.add(event);
            if (event instanceof GameEvent.HandDealt) {
                handReceived.countDown();
            }
        });
        client.connect("localhost", server.getPort(), "Nuno");

        assertEquals(0, client.getMySeat(), "o primeiro a entrar é o anfitrião (seat 0)");
        assertEquals("Nuno", client.getPlayers().get(0));

        client.sendStart();
        assertTrue(handReceived.await(5, TimeUnit.SECONDS), "não recebeu a mão a tempo");

        GameEvent.HandDealt hand = events.stream()
                .filter(e -> e instanceof GameEvent.HandDealt)
                .map(e -> (GameEvent.HandDealt) e)
                .findFirst().orElseThrow();
        assertEquals(0, hand.seat(), "só devia receber a própria mão");
        assertEquals(10, hand.hand().size());

        List<String> started = events.stream()
                .filter(e -> e instanceof GameEvent.GameStarted)
                .map(e -> ((GameEvent.GameStarted) e).playerNames())
                .findFirst().orElseThrow();
        assertEquals(4, started.size(), "os lugares vazios devem ser preenchidos com bots");
    }
}
