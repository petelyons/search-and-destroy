package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.GameState;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

/**
 * Command to resume game execution after waiting for orders.
 * This command signals the game thread to continue processing.
 */
public class ResumeGameCommand implements GameCommand {

    private final Unit unit;

    public ResumeGameCommand(Unit unit) {
        this.unit = unit;
    }

    @Override
    public void execute(Game game) {
        // If a unit was provided, select it and add to pending play
        if (unit != null) {
            game.selectUnit(unit);
            unit.getOwner().pushPendingPlay(unit);
        }

        // Transition from AWAITING_ORDERS to RUNNING
        GameState currentState = game.getGameState();
        if (currentState == GameState.AWAITING_ORDERS) {
            game.transitionState(GameState.AWAITING_ORDERS, GameState.RUNNING);
            Log.debug(game, "Game resumed from AWAITING_ORDERS");
        } else {
            Log.warn(game, "Resume called but state is: " + currentState);
        }
    }
}
