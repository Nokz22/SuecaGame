package com.suecagame.commands;

import com.suecagame.model.Card;
import com.suecagame.service.GameService;

public class PlayCardCommand implements Command {

    private final Card card;

    public PlayCardCommand(Card card) {
        this.card = card;
    }

    public Card getCard() {
        return card;
    }

    @Override
    public void execute(GameService service, int seat) {
        service.playCard(seat, card);
    }
}
