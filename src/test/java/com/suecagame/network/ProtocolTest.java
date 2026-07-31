package com.suecagame.network;

import com.suecagame.events.GameEvent;
import com.suecagame.model.Card;
import com.suecagame.model.Suit;
import com.suecagame.model.Value;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProtocolTest {

    @Test
    void eventsSurviveEncodeDecodeRoundTrip() {
        List<GameEvent> events = List.of(
                new GameEvent.PlayerJoined(2, "Nuno"),
                new GameEvent.GameStarted(List.of("A", "B", "C", "D")),
                new GameEvent.HandDealt(1, List.of(
                        new Card(Suit.COPAS, Value.ÀS), new Card(Suit.PAUS, Value.DOIS))),
                new GameEvent.TrumpRevealed(new Card(Suit.OUROS, Value.SETE), 3),
                new GameEvent.TurnStarted(0),
                new GameEvent.CardPlayed(2, new Card(Suit.ESPADAS, Value.REI)),
                new GameEvent.InvalidPlay(1, "Não é a tua vez"),
                new GameEvent.TrickCompleted(3, 21),
                new GameEvent.RoundEnded(70, 50, 1, 0),
                new GameEvent.GameOver("Equipa 1"),
                new GameEvent.PhaseChanged("PLAYING"));

        for (GameEvent event : events) {
            assertEquals(event, Protocol.decodeEvent(Protocol.encode(event)),
                    "round-trip falhou para " + event.getClass().getSimpleName());
        }
    }

    @Test
    void unknownOrMalformedLinesDecodeToNull() {
        assertNull(Protocol.decodeEvent(null));
        assertNull(Protocol.decodeEvent(""));
        assertNull(Protocol.decodeEvent("XPTO|1|2"));
        assertNull(Protocol.decodeEvent("TURN|not-a-number"));
    }

    @Test
    void sanitizeNameStripsProtocolSeparators() {
        assertEquals("Nuno  Ferreira", Protocol.sanitizeName("Nuno|,Ferreira"));
        assertEquals("Jogador", Protocol.sanitizeName("  "));
        assertEquals("Jogador", Protocol.sanitizeName(null));
    }
}
