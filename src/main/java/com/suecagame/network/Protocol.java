package com.suecagame.network;

import com.suecagame.events.GameEvent;
import com.suecagame.model.Card;

import java.util.Arrays;
import java.util.List;

/**
 * Protocolo de texto por linhas (UTF-8), um evento/comando por linha.
 * Campos separados por '|', listas por ',', cartas como "NAIPE:VALOR".
 *
 * Cliente → Servidor:  JOIN|nome   PLAY|NAIPE:VALOR   START
 * Servidor → Cliente:  WELCOME|seat  e os eventos codificados abaixo.
 */
public final class Protocol {

    public static final int DEFAULT_PORT = 5555;

    public static final String JOIN = "JOIN";
    public static final String WELCOME = "WELCOME";

    private Protocol() {
    }

    public static String encode(GameEvent event) {
        return switch (event) {
            case GameEvent.PlayerJoined e -> "JOINED|" + e.seat() + "|" + e.name();
            case GameEvent.GameStarted e -> "STARTED|" + String.join(",", e.playerNames());
            case GameEvent.HandDealt e -> "HAND|" + e.seat() + "|" + encodeCards(e.hand());
            case GameEvent.TrumpRevealed e -> "TRUMP|" + e.trumpCard().id() + "|" + e.dealerSeat();
            case GameEvent.TurnStarted e -> "TURN|" + e.seat();
            case GameEvent.CardPlayed e -> "PLAYED|" + e.seat() + "|" + e.card().id();
            case GameEvent.InvalidPlay e -> "INVALID|" + e.seat() + "|" + e.reason();
            case GameEvent.TrickCompleted e -> "TRICK|" + e.winnerSeat() + "|" + e.points();
            case GameEvent.RoundEnded e -> "ROUND|" + e.team1RoundPoints() + "|" + e.team2RoundPoints()
                    + "|" + e.team1GamePoints() + "|" + e.team2GamePoints();
            case GameEvent.GameOver e -> "OVER|" + e.winnerTeamName();
            case GameEvent.PhaseChanged e -> "PHASE|" + e.phase();
        };
    }

    /** Devolve null para linhas que não são eventos de jogo. */
    public static GameEvent decodeEvent(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] p = line.trim().split("\\|", -1);
        try {
            return switch (p[0]) {
                case "JOINED" -> new GameEvent.PlayerJoined(Integer.parseInt(p[1]), p[2]);
                case "STARTED" -> new GameEvent.GameStarted(Arrays.asList(p[1].split(",")));
                case "HAND" -> new GameEvent.HandDealt(Integer.parseInt(p[1]), decodeCards(p[2]));
                case "TRUMP" -> new GameEvent.TrumpRevealed(Card.fromId(p[1]), Integer.parseInt(p[2]));
                case "TURN" -> new GameEvent.TurnStarted(Integer.parseInt(p[1]));
                case "PLAYED" -> new GameEvent.CardPlayed(Integer.parseInt(p[1]), Card.fromId(p[2]));
                case "INVALID" -> new GameEvent.InvalidPlay(Integer.parseInt(p[1]), p[2]);
                case "TRICK" -> new GameEvent.TrickCompleted(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                case "ROUND" -> new GameEvent.RoundEnded(Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                        Integer.parseInt(p[3]), Integer.parseInt(p[4]));
                case "OVER" -> new GameEvent.GameOver(p[1]);
                case "PHASE" -> new GameEvent.PhaseChanged(p[1]);
                default -> null;
            };
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String encodeCards(List<Card> cards) {
        return String.join(",", cards.stream().map(Card::id).toList());
    }

    public static List<Card> decodeCards(String encoded) {
        if (encoded.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(encoded.split(",")).map(Card::fromId).toList();
    }

    /** Nomes não podem conter os separadores do protocolo. */
    public static String sanitizeName(String name) {
        String clean = name == null ? "" : name.replaceAll("[|,\\r\\n]", " ").trim();
        return clean.isEmpty() ? "Jogador" : clean;
    }
}
