package com.suecagame.ui;

import com.suecagame.controller.GameRules;
import com.suecagame.controller.GameSession;
import com.suecagame.events.GameEvent;
import com.suecagame.model.Card;
import com.suecagame.model.Game;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Mesa de jogo: mão do jogador em baixo (cartas clicáveis na sua vez),
 * cartas jogadas ao centro, parceiro em cima e adversários aos lados.
 * Todo o estado mostrado vem dos eventos do motor — a vista não decide regras,
 * exceto pré-validar a jogada para desativar cartas ilegais.
 */
public class GameView implements GameScreen {

    private final BorderPane root;
    private final GameSession session;
    private final SuecaApplication app;
    private final int mySeat;
    private final List<String> playerNames;

    private final List<Card> myHand = new ArrayList<>();
    private final List<Card> currentTrickCards = new ArrayList<>();
    private int currentSeat = -1;
    private boolean trickClearPending;
    private int team1Round;
    private int team2Round;
    private int team1Games;
    private int team2Games;

    private final HBox handBox = new HBox(8);
    private final Label statusLabel = new Label();
    private final Label scoreLabel = new Label();
    private final HBox trumpBox = new HBox(8);
    private final Label[] nameLabels = new Label[Game.NUM_PLAYERS];
    private final StackPane[] cardSlots = new StackPane[Game.NUM_PLAYERS];

    public GameView(GameSession session, SuecaApplication app, List<String> playerNames) {
        this.session = session;
        this.app = app;
        this.mySeat = session.mySeat();
        this.playerNames = playerNames;

        root = new BorderPane();
        root.setStyle("-fx-background-color: radial-gradient(center 50% 50%, radius 80%, #2e7d32, #1b5e20);");
        root.setPadding(new Insets(12));

        root.setTop(buildTopBar());
        root.setCenter(buildTable());
        root.setBottom(buildBottomBar());
        updateScoreLabel();
    }

    //LAYOUT
    private HBox buildTopBar() {
        scoreLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label trumpTitle = new Label("Trunfo:");
        trumpTitle.setStyle("-fx-font-size: 15px; -fx-text-fill: white;");
        trumpBox.setAlignment(Pos.CENTER_LEFT);
        trumpBox.getChildren().add(trumpTitle);

        Button leaveButton = new Button("Sair");
        leaveButton.setOnAction(e -> app.showMenu());

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        HBox bar = new HBox(24, scoreLabel, trumpBox, spacer, leaveButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 8, 0));
        return bar;
    }

    private BorderPane buildTable() {
        for (int seat = 0; seat < Game.NUM_PLAYERS; seat++) {
            nameLabels[seat] = new Label(displayName(seat));
            nameLabels[seat].setStyle(nameStyle(false));
            cardSlots[seat] = new StackPane();
            cardSlots[seat].setPrefSize(80, 112);
        }

        BorderPane table = new BorderPane();
        table.setTop(seatBox(seatAtPosition(2)));       // parceiro
        table.setLeft(seatBox(seatAtPosition(3)));      // adversário
        table.setRight(seatBox(seatAtPosition(1)));     // adversário
        table.setBottom(seatBox(mySeat));               // eu
        BorderPane.setAlignment(table.getTop(), Pos.CENTER);
        BorderPane.setAlignment(table.getLeft(), Pos.CENTER);
        BorderPane.setAlignment(table.getRight(), Pos.CENTER);
        BorderPane.setAlignment(table.getBottom(), Pos.CENTER);
        return table;
    }

    /** posição visual → lugar real: 1 = direita, 2 = topo (parceiro), 3 = esquerda */
    private int seatAtPosition(int position) {
        return (mySeat + position) % Game.NUM_PLAYERS;
    }

    private VBox seatBox(int seat) {
        VBox box = new VBox(6, nameLabels[seat], cardSlots[seat]);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8));
        return box;
    }

    private VBox buildBottomBar() {
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #fff59d;");
        handBox.setAlignment(Pos.CENTER);
        handBox.setPadding(new Insets(8, 0, 0, 0));
        VBox bottom = new VBox(6, statusLabel, handBox);
        bottom.setAlignment(Pos.CENTER);
        return bottom;
    }

    private String displayName(int seat) {
        String name = seat < playerNames.size() ? playerNames.get(seat) : ("Lugar " + seat);
        return seat == mySeat ? name + " (tu)" : name;
    }

    private String nameStyle(boolean active) {
        return "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (active ? "#ffd54f" : "white") + ";";
    }

    //EVENTOS
    @Override
    public void handleEvent(GameEvent event) {
        switch (event) {
            case GameEvent.HandDealt e -> {
                myHand.clear();
                myHand.addAll(e.hand());
                rebuildHand();
            }
            case GameEvent.TrumpRevealed e -> showTrump(e.trumpCard(), e.dealerSeat());
            case GameEvent.TurnStarted e -> {
                currentSeat = e.seat();
                highlightTurn();
                rebuildHand();
            }
            case GameEvent.CardPlayed e -> onCardPlayed(e);
            case GameEvent.TrickCompleted e -> onTrickCompleted(e);
            case GameEvent.RoundEnded e -> onRoundEnded(e);
            case GameEvent.GameOver e -> onGameOver(e);
            case GameEvent.InvalidPlay e -> statusLabel.setText(e.reason());
            default -> {
                // PlayerJoined, GameStarted, PhaseChanged: sem efeito na mesa
            }
        }
    }

    private void showTrump(Card trumpCard, int dealerSeat) {
        trumpBox.getChildren().removeIf(node -> node instanceof VBox);
        trumpBox.getChildren().add(CardView.createStatic(trumpCard, 0.55));
        statusLabel.setText("Trunfo: " + trumpCard + " (carteador: " + displayName(dealerSeat) + ")");
    }

    private void onCardPlayed(GameEvent.CardPlayed e) {
        if (trickClearPending) {
            clearTable();
        }
        currentTrickCards.add(e.card());
        cardSlots[e.seat()].getChildren().setAll(CardView.createStatic(e.card(), 0.8));
        if (e.seat() == mySeat) {
            myHand.remove(e.card());
            rebuildHand();
            statusLabel.setText("");
        }
    }

    private void onTrickCompleted(GameEvent.TrickCompleted e) {
        boolean myTeam = e.winnerSeat() % 2 == mySeat % 2;
        if (e.winnerSeat() % 2 == 0) {
            team1Round += e.points();
        } else {
            team2Round += e.points();
        }
        updateScoreLabel();
        statusLabel.setText("Vaza para " + displayName(e.winnerSeat())
                + " (+" + e.points() + " pontos)" + (myTeam ? " ✔" : ""));

        trickClearPending = true;
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(ev -> {
            if (trickClearPending) {
                clearTable();
            }
        });
        pause.play();
    }

    private void onRoundEnded(GameEvent.RoundEnded e) {
        team1Round = e.team1RoundPoints();
        team2Round = e.team2RoundPoints();
        team1Games = e.team1GamePoints();
        team2Games = e.team2GamePoints();
        updateScoreLabel();
        statusLabel.setText("Fim da ronda: Equipa 1 " + team1Round + " pts, Equipa 2 "
                + team2Round + " pts — jogos " + team1Games + "-" + team2Games);
        team1Round = 0;
        team2Round = 0;
    }

    private void onGameOver(GameEvent.GameOver e) {
        clearTable();
        handBox.getChildren().clear();
        statusLabel.setText("🏆 Vitória da " + e.winnerTeamName() + "!");
        Button backButton = new Button("Voltar ao Menu");
        backButton.setOnAction(ev -> app.showMenu());
        handBox.getChildren().add(backButton);
    }

    //HELPERS
    private void clearTable() {
        trickClearPending = false;
        currentTrickCards.clear();
        for (StackPane slot : cardSlots) {
            slot.getChildren().clear();
        }
    }

    private void highlightTurn() {
        for (int seat = 0; seat < Game.NUM_PLAYERS; seat++) {
            nameLabels[seat].setStyle(nameStyle(seat == currentSeat));
        }
        if (currentSeat == mySeat) {
            statusLabel.setText("É a tua vez!");
        }
    }

    private void rebuildHand() {
        handBox.getChildren().clear();
        boolean myTurn = currentSeat == mySeat;
        // com uma vaza fechada ainda na mesa, a próxima vaza está logicamente vazia
        Card firstCard = (trickClearPending || currentTrickCards.isEmpty())
                ? null : currentTrickCards.get(0);
        for (Card card : myHand) {
            Button cardButton = CardView.createButton(card);
            boolean legal = GameRules.isValidPlay(card, myHand, firstCard);
            cardButton.setDisable(!myTurn || !legal);
            cardButton.setOpacity(myTurn && legal ? 1.0 : 0.55);
            cardButton.setOnAction(e -> session.playCard(card));
            handBox.getChildren().add(cardButton);
        }
    }

    private void updateScoreLabel() {
        scoreLabel.setText(String.format("Equipa 1: %d pts (jogos %d)   •   Equipa 2: %d pts (jogos %d)",
                team1Round, team1Games, team2Round, team2Games));
    }

    @Override
    public Parent root() {
        return root;
    }
}
