package com.suecagame.ui;

import com.suecagame.controller.GameSession;
import com.suecagame.events.GameEvent;
import com.suecagame.model.Game;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;

/**
 * Sala de espera do multiplayer: mostra quem já se sentou.
 * O anfitrião pode começar quando quiser — os lugares vazios
 * são preenchidos com bots.
 */
public class LobbyView implements GameScreen {

    private final VBox root;
    private final SuecaApplication app;
    private final Label[] seatLabels = new Label[Game.NUM_PLAYERS];
    private final Map<Integer, String> names = new HashMap<>();

    public LobbyView(GameSession session, SuecaApplication app) {
        this.app = app;

        Label title = new Label("Sala de Espera");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox seatsBox = new VBox(8);
        seatsBox.setAlignment(Pos.CENTER);
        for (int seat = 0; seat < Game.NUM_PLAYERS; seat++) {
            seatLabels[seat] = new Label();
            seatLabels[seat].setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
            seatsBox.getChildren().add(seatLabels[seat]);
        }

        Label info = new Label(session.isHost()
                ? "Os lugares vazios serão preenchidos por bots."
                : "À espera que o anfitrião comece o jogo...");
        info.setStyle("-fx-text-fill: #c8e6c9;");

        root = new VBox(20, title, seatsBox, info);
        if (session.isHost()) {
            Button startButton = new Button("Começar Jogo");
            startButton.setOnAction(e -> session.start());
            root.getChildren().add(startButton);
        }
        Button backButton = new Button("Voltar ao Menu");
        backButton.setOnAction(e -> app.showMenu());
        root.getChildren().add(backButton);

        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1b5e20, #2e7d32);");

        names.putAll(session.playerNames());
        refreshSeats();
    }

    private void refreshSeats() {
        for (int seat = 0; seat < Game.NUM_PLAYERS; seat++) {
            String name = names.get(seat);
            String team = seat % 2 == 0 ? "Equipa 1" : "Equipa 2";
            seatLabels[seat].setText("Lugar " + seat + " (" + team + "): "
                    + (name == null ? "— livre —" : name));
        }
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void handleEvent(GameEvent event) {
        switch (event) {
            case GameEvent.PlayerJoined e -> {
                names.put(e.seat(), e.name());
                refreshSeats();
            }
            case GameEvent.GameStarted e -> app.showGame(e.playerNames());
            default -> {
                // restantes eventos não afetam o lobby
            }
        }
    }
}
