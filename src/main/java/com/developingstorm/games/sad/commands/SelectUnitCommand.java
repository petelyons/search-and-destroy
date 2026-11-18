package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;

/**
 * Command to select a unit (or deselect by passing null).
 */
public class SelectUnitCommand implements GameCommand {

    private final Unit unit;

    public SelectUnitCommand(Unit unit) {
        this.unit = unit;
    }

    @Override
    public void execute(Game game) {
        if (unit == null) {
            // Logged so a blank unit panel can be traced back to the UI action that caused it
            com.developingstorm.games.sad.util.Log.info(game, "Selection cleared by the UI");
            game.deselectUnit();
        } else {
            game.selectUnit(unit);
        }
    }
}
