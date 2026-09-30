package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;

/**
 * Toggle a player between Human and AI control. Executed on the game thread.
 */
public class TogglePlayerTypeCommand implements GameCommand {

    private final int playerId;

    public TogglePlayerTypeCommand(Player player) {
        this.playerId = player.getId();
    }

    @Override
    public void execute(Game game) {
        for (Player p : game.getPlayers()) {
            if (p.getId() == playerId) {
                game.togglePlayerType(p);
                return;
            }
        }
    }
}
