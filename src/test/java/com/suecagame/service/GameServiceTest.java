package com.suecagame.service;

import com.suecagame.events.GameEvent;
import com.suecagame.model.Card;
import com.suecagame.model.Game;
import com.suecagame.model.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameServiceTest {

    private GameService newSeededService(List<GameEvent> sink) {
        GameService service = new GameService(new Random(42));
        if (sink != null) {
            service.addListener(sink::add);
        }
        return service;
    }

    @Test
    void startFillsTableWithBots() {
        GameService service = newSeededService(null);
        service.addPlayer("Nuno", Player.Type.HUMAN);
        service.start();

        Game game = service.getGame();
        assertTrue(game.isFull());
        assertEquals("Nuno", game.playerAt(0).getName());
        assertTrue(game.playerAt(1).isBot());
        assertTrue(game.playerAt(2).isBot());
        assertTrue(game.playerAt(3).isBot());
        assertEquals(GamePhase.PLAYING, service.getPhase());
    }

    @Test
    void dealGivesTenSortedCardsToEachPlayerAndSetsTrump() {
        GameService service = newSeededService(null);
        service.addPlayer("Nuno", Player.Type.HUMAN);
        service.start();

        Game game = service.getGame();
        for (int seat = 0; seat < Game.NUM_PLAYERS; seat++) {
            assertEquals(Game.TRICKS_PER_ROUND, game.playerAt(seat).getHand().size());
        }
        assertNotNull(game.getTrumpCard());
        // o trunfo fica na mão do carteador
        assertTrue(game.playerAt(game.getDealerIndex()).hasCard(game.getTrumpCard()));
        // quem abre é o jogador à esquerda do carteador
        assertEquals((game.getDealerIndex() + 1) % Game.NUM_PLAYERS, game.getCurrentPlayerIndex());
    }

    @Test
    void cannotPlayOutOfTurn() {
        List<GameEvent> events = new ArrayList<>();
        GameService service = newSeededService(events);
        service.addPlayer("Nuno", Player.Type.HUMAN);
        service.start();

        int wrongSeat = (service.getCurrentSeat() + 1) % Game.NUM_PLAYERS;
        Card someCard = service.handOf(wrongSeat).get(0);
        assertFalse(service.playCard(wrongSeat, someCard));
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.InvalidPlay));
    }

    @Test
    void mustFollowLeadSuitWhenPossible() {
        GameService service = newSeededService(null);
        service.addPlayer("Nuno", Player.Type.HUMAN);
        service.start();

        // primeiro jogador abre a vaza
        int leader = service.getCurrentSeat();
        Card lead = service.handOf(leader).get(0);
        assertTrue(service.playCard(leader, lead));

        // se o seguinte tiver o naipe, uma carta de outro naipe é rejeitada
        int next = service.getCurrentSeat();
        List<Card> hand = service.handOf(next);
        boolean hasLeadSuit = hand.stream().anyMatch(c -> c.getSuit() == lead.getSuit());
        Card offSuit = hand.stream().filter(c -> c.getSuit() != lead.getSuit()).findFirst().orElse(null);
        if (hasLeadSuit && offSuit != null) {
            assertFalse(service.playCard(next, offSuit));
        }
    }

    @Test
    void fullRoundDistributes120PointsAcrossTenTricks() {
        List<GameEvent> events = new ArrayList<>();
        GameService service = newSeededService(events);
        service.addPlayer("Bot Zero", Player.Type.BOT);
        service.start();

        int safety = 0;
        while (events.stream().noneMatch(e -> e instanceof GameEvent.RoundEnded) && safety++ < 200) {
            assertTrue(service.playBotTurn(), "o bot devia conseguir jogar sempre");
        }

        List<GameEvent.TrickCompleted> tricks = events.stream()
                .filter(e -> e instanceof GameEvent.TrickCompleted)
                .map(e -> (GameEvent.TrickCompleted) e)
                .toList();
        assertEquals(Game.TRICKS_PER_ROUND, tricks.size());
        assertEquals(120, tricks.stream().mapToInt(GameEvent.TrickCompleted::points).sum());

        GameEvent.RoundEnded round = events.stream()
                .filter(e -> e instanceof GameEvent.RoundEnded)
                .map(e -> (GameEvent.RoundEnded) e)
                .findFirst().orElseThrow();
        assertEquals(120, round.team1RoundPoints() + round.team2RoundPoints());
    }

    @Test
    void matchEndsWhenATeamReachesFourGamePoints() {
        List<GameEvent> events = new ArrayList<>();
        GameService service = newSeededService(events);
        service.addPlayer("Bot Zero", Player.Type.BOT);
        service.start();

        int safety = 0;
        while (service.getPhase() != GamePhase.GAME_OVER && safety++ < 5000) {
            service.playBotTurn();
        }

        assertEquals(GamePhase.GAME_OVER, service.getPhase());
        assertTrue(events.stream().anyMatch(e -> e instanceof GameEvent.GameOver));
        Game game = service.getGame();
        assertTrue(game.getTeam1().getGamePoints() >= 4 || game.getTeam2().getGamePoints() >= 4);
    }

    @Test
    void cannotJoinAfterGameStarts() {
        GameService service = newSeededService(null);
        service.addPlayer("Nuno", Player.Type.HUMAN);
        service.start();

        assertThrows(IllegalStateException.class,
                () -> service.addPlayer("Atrasado", Player.Type.HUMAN));
    }
}
