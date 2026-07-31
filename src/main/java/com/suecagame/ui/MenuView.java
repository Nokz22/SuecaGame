package com.suecagame.ui;

import com.suecagame.events.GameEvent;
import com.suecagame.network.Protocol;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Menu inicial: nome do jogador e escolha do modo de jogo
 * (single player, criar jogo LAN ou juntar-se a um jogo LAN).
 */
public class MenuView implements GameScreen {

    private final VBox root;
    private final Label errorLabel = new Label();

    public MenuView(SuecaApplication app) {
        Label title = new Label("♠ SUECA ♥");
        title.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: white;");

        TextField nameField = new TextField("Jogador");
        nameField.setPromptText("O teu nome");
        nameField.setMaxWidth(240);

        Button singleButton = new Button("Jogar Sozinho (vs Bots)");
        singleButton.setPrefWidth(240);
        singleButton.setOnAction(e -> app.startSinglePlayer(playerName(nameField)));

        TextField hostPortField = new TextField(String.valueOf(Protocol.DEFAULT_PORT));
        hostPortField.setPrefWidth(80);
        Button hostButton = new Button("Criar Jogo LAN");
        hostButton.setPrefWidth(150);
        hostButton.setOnAction(e ->
                app.hostGame(playerName(nameField), parsePort(hostPortField)));
        HBox hostBox = new HBox(10, hostButton, hostPortField);
        hostBox.setAlignment(Pos.CENTER);

        TextField joinAddressField = new TextField();
        joinAddressField.setPromptText("IP do anfitrião");
        joinAddressField.setPrefWidth(130);
        TextField joinPortField = new TextField(String.valueOf(Protocol.DEFAULT_PORT));
        joinPortField.setPrefWidth(80);
        Button joinButton = new Button("Entrar em Jogo LAN");
        joinButton.setPrefWidth(150);
        joinButton.setOnAction(e ->
                app.joinGame(playerName(nameField), joinAddressField.getText().trim(), parsePort(joinPortField)));
        HBox joinBox = new HBox(10, joinButton, joinAddressField, joinPortField);
        joinBox.setAlignment(Pos.CENTER);

        errorLabel.setStyle("-fx-text-fill: #ffd54f;");

        root = new VBox(16, title, nameField, singleButton,
                new Separator(), hostBox, joinBox, errorLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #1b5e20, #2e7d32);");
    }

    private String playerName(TextField field) {
        String name = field.getText().trim();
        return name.isEmpty() ? "Jogador" : name;
    }

    private int parsePort(TextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return Protocol.DEFAULT_PORT;
        }
    }

    public void showError(String message) {
        errorLabel.setText(message);
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void handleEvent(GameEvent event) {
        // o menu não reage a eventos de jogo
    }
}
