package com.suecagame.ui;

/**
 * Ponto de entrada do JAR executável. O JavaFX recusa arrancar diretamente
 * de uma subclasse de Application quando vai no classpath (fat jar),
 * por isso o main vive numa classe separada.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        SuecaApplication.main(args);
    }
}
