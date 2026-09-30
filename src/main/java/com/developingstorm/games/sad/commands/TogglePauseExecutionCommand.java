package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;

/**
 * Toggle game-loop execution between RUNNING and PAUSED.
 * Has no effect when the game is in AWAITING_ORDERS or GAME_OVER.
 */
public class TogglePauseExecutionCommand implements GameCommand {

    @Override
    public void execute(Game game) {
        game.togglePauseExecution();
    }
}
