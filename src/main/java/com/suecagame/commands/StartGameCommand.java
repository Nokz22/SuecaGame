package com.suecagame.commands;

import com.suecagame.service.GameService;

public class StartGameCommand implements Command {

    /** Só o anfitrião (seat 0) pode começar o jogo. */
    public static final int HOST_SEAT = 0;

    @Override
    public void execute(GameService service, int seat) {
        if (seat != HOST_SEAT) {
            return;
        }
        service.start();
    }
}
