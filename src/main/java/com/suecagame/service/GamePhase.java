package com.suecagame.service;

/**
 * Fases do jogo (padrão State — cada fase tem uma classe própria
 * que define o comportamento permitido nesse momento).
 */
public enum GamePhase {
    WAITING_FOR_PLAYERS,
    DEALING,
    PLAYING,
    SCORING,
    GAME_OVER
}
