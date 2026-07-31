package com.suecagame.ui;

import com.suecagame.model.Card;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Carta desenhada com nodes JavaFX (sem imagens externas):
 * retângulo branco com valor e símbolo do naipe.
 */
public final class CardView {

    private CardView() {
    }

    public static Button createButton(Card card) {
        Button button = new Button();
        button.setGraphic(face(card, 16, 26));
        button.setPrefSize(74, 106);
        button.setStyle(baseStyle());
        return button;
    }

    public static VBox createStatic(Card card, double scale) {
        VBox face = face(card, 16 * scale, 26 * scale);
        face.setPrefSize(74 * scale, 106 * scale);
        face.setMaxSize(74 * scale, 106 * scale);
        face.setMinSize(74 * scale, 106 * scale);
        face.setStyle(baseStyle());
        return face;
    }

    private static VBox face(Card card, double valueSize, double suitSize) {
        String color = card.getSuit().isRed() ? "#c0392b" : "#2c3e50";

        Label value = new Label(card.getValue().getShortName());
        value.setStyle("-fx-font-size: " + valueSize + "px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label suit = new Label(card.getSuit().getSymbol());
        suit.setStyle("-fx-font-size: " + suitSize + "px; -fx-text-fill: " + color + ";");

        VBox box = new VBox(2, value, suit);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static String baseStyle() {
        return """
                -fx-background-color: white;
                -fx-background-radius: 8;
                -fx-border-color: #b0b0b0;
                -fx-border-radius: 8;
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 4, 0, 1, 1);
                """;
    }
}
