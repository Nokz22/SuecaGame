package com.suecagame.commands;

import com.suecagame.service.GameService;

/**
 * Padrão Command: uma ação de um jogador, executável localmente ou
 * recebida pela rede. O lugar (seat) é atribuído pelo servidor a partir
 * da ligação de origem — o cliente nunca escolhe o próprio seat.
 */
public interface Command {

    void execute(GameService service, int seat);
}
