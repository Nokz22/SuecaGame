package com.suecagame.ui.console;

import com.suecagame.controller.GameRules;
import com.suecagame.events.GameEvent;
import com.suecagame.model.Card;
import com.suecagame.model.Game;
import com.suecagame.model.Player;
import com.suecagame.service.GamePhase;
import com.suecagame.service.GameService;

import java.util.List;
import java.util.Scanner;

/**
 * Versão de consola do jogo: single player contra 3 bots.
 * Com o argumento --demo corre uma partida totalmente automática
 * (útil para ver o motor a funcionar de ponta a ponta).
 */
public class ConsoleGame {

    private final GameService service;
    private final Scanner scanner = new Scanner(System.in);
    private final boolean demo;
    private int humanSeat = -1;

    public static void main(String[] args) {
        boolean demo = args.length > 0 && "--demo".equals(args[0]);
        new ConsoleGame(demo).run();
    }

    public ConsoleGame(boolean demo) {
        this.demo = demo;
        this.service = new GameService();
        this.service.addListener(this::printEvent);
    }

    private void run() {
        System.out.println("==============================");
        System.out.println("          SUECA");
        System.out.println("==============================");

        if (demo) {
            service.addPlayer("Bot Demo", Player.Type.BOT);
        } else {
            System.out.print("O teu nome: ");
            String name = scanner.nextLine().trim();
            humanSeat = service.addPlayer(name.isEmpty() ? "Jogador" : name, Player.Type.HUMAN);
        }

        service.start();

        while (service.getPhase() != GamePhase.GAME_OVER) {
            if (service.isBotTurn()) {
                service.playBotTurn();
            } else if (service.getPhase() == GamePhase.PLAYING) {
                humanTurn();
            }
        }
    }

    private void humanTurn() {
        Game game = service.getGame();
        List<Card> hand = service.handOf(humanSeat);
        Card firstCard = game.getCurrentTrick().firstCard();

        System.out.println();
        System.out.println("A tua mão:");
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            boolean legal = GameRules.isValidPlay(card, hand, firstCard);
            System.out.printf("  %2d) %-16s %s%n", i + 1, card, legal ? "" : "(não podes)");
        }
        System.out.print("Escolhe a carta (número): ");

        int choice = readInt(1, hand.size());
        boolean played = service.playCard(humanSeat, hand.get(choice - 1));
        if (!played) {
            humanTurn();
        }
    }

    private int readInt(int min, int max) {
        while (true) {
            String line = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // volta a pedir
            }
            System.out.printf("Escolhe um número entre %d e %d: ", min, max);
        }
    }

    private void printEvent(GameEvent event) {
        Game game = service.getGame();
        switch (event) {
            case GameEvent.GameStarted e ->
                    System.out.println("Jogo começado: " + String.join(", ", e.playerNames())
                            + "  [Equipa 1: lugares 0 e 2 | Equipa 2: lugares 1 e 3]");
            case GameEvent.TrumpRevealed e ->
                    System.out.printf("%nTrunfo: %s (carteador: %s)%n",
                            e.trumpCard(), game.playerAt(e.dealerSeat()).getName());
            case GameEvent.CardPlayed e ->
                    System.out.printf("  %s joga %s%n", game.playerAt(e.seat()).getName(), e.card());
            case GameEvent.TrickCompleted e ->
                    System.out.printf("  >> Vaza para %s (+%d pontos)%n%n",
                            game.playerAt(e.winnerSeat()).getName(), e.points());
            case GameEvent.InvalidPlay e -> {
                if (e.seat() == humanSeat) {
                    System.out.println("Jogada inválida: " + e.reason());
                }
            }
            case GameEvent.RoundEnded e ->
                    System.out.printf("%n--- Fim da ronda: Equipa 1 %d pts | Equipa 2 %d pts"
                                    + "  (jogos: %d-%d) ---%n%n",
                            e.team1RoundPoints(), e.team2RoundPoints(),
                            e.team1GamePoints(), e.team2GamePoints());
            case GameEvent.GameOver e ->
                    System.out.println("### VITÓRIA da " + e.winnerTeamName() + "! ###");
            default -> {
                // HandDealt, TurnStarted, PlayerJoined, PhaseChanged: sem output próprio
            }
        }
    }
}
