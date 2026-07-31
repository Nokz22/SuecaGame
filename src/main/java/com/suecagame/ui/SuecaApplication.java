package com.suecagame.ui;

import com.suecagame.controller.GameSession;
import com.suecagame.controller.LocalGameSession;
import com.suecagame.controller.RemoteGameSession;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * Aplicação JavaFX. Mantém uma única sessão de jogo ativa e um único
 * listener de eventos: todos os eventos passam para o ecrã ativo através
 * da fila do JavaFX (Platform.runLater), o que preserva a ordem e evita
 * tocar na UI a partir de threads de rede.
 */
public class SuecaApplication extends Application {

    private Stage stage;
    private GameSession session;
    private GameScreen currentScreen;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Sueca");
        stage.setScene(new Scene(new MenuView(this).root(), 1000, 700));
        showMenu();
        stage.show();
    }

    @Override
    public void stop() {
        closeSession();
    }

    //NAVEGAÇÃO
    public void showMenu() {
        closeSession();
        setScreen(new MenuView(this));
    }

    public void showGame(List<String> playerNames) {
        setScreen(new GameView(session, this, playerNames));
    }

    private void setScreen(GameScreen screen) {
        currentScreen = screen;
        stage.getScene().setRoot(screen.root());
    }

    //MODOS DE JOGO
    public void startSinglePlayer(String playerName) {
        closeSession();
        LocalGameSession local = new LocalGameSession(playerName);
        session = local;
        wireSession();
        setScreen(new GameView(session, this,
                List.of(playerName, "Bot 1", "Bot 2", "Bot 3")));
        local.start();
    }

    public void hostGame(String playerName, int port) {
        connectInBackground(() -> RemoteGameSession.host(port, playerName));
    }

    public void joinGame(String playerName, String host, int port) {
        if (host.isEmpty()) {
            showMenuError("Indica o IP do anfitrião.");
            return;
        }
        connectInBackground(() -> RemoteGameSession.join(host, port, playerName));
    }

    private interface SessionFactory {
        GameSession create() throws IOException;
    }

    /** A ligação pode bloquear (IP errado, porta ocupada) — nunca na thread do JavaFX. */
    private void connectInBackground(SessionFactory factory) {
        closeSession();
        Thread connector = new Thread(() -> {
            try {
                GameSession newSession = factory.create();
                Platform.runLater(() -> {
                    session = newSession;
                    wireSession();
                    setScreen(new LobbyView(session, this));
                });
            } catch (IOException e) {
                Platform.runLater(() -> showMenuError("Não foi possível ligar: " + e.getMessage()));
            }
        }, "sueca-connect");
        connector.setDaemon(true);
        connector.start();
    }

    private void wireSession() {
        session.addListener(event -> Platform.runLater(() -> {
            if (currentScreen != null) {
                currentScreen.handleEvent(event);
            }
        }));
    }

    private void showMenuError(String message) {
        if (currentScreen instanceof MenuView menu) {
            menu.showError(message);
        } else {
            setScreen(new MenuView(this));
            ((MenuView) currentScreen).showError(message);
        }
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }
}
