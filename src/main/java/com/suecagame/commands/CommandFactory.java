package com.suecagame.commands;

import com.suecagame.model.Card;

/**
 * Factory de comandos a partir de mensagens do protocolo de rede.
 * Formato: "PLAY|NAIPE:VALOR" e "START".
 */
public final class CommandFactory {

    private CommandFactory() {
    }

    /** Devolve null para mensagens desconhecidas ou malformadas. */
    public static Command fromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String[] parts = message.trim().split("\\|");
        try {
            return switch (parts[0]) {
                case "PLAY" -> parts.length == 2 ? new PlayCardCommand(Card.fromId(parts[1])) : null;
                case "START" -> new StartGameCommand();
                default -> null;
            };
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
