package com.suecagame.ui;

import com.suecagame.events.GameEvent;
import javafx.scene.Parent;

/**
 * Um ecrã da aplicação que reage a eventos do jogo.
 * A aplicação encaminha todos os eventos para o ecrã ativo,
 * já na thread do JavaFX.
 */
public interface GameScreen {

    Parent root();

    void handleEvent(GameEvent event);
}
