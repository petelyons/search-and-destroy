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
            game.deselectUnit();
        } else {
            game.selectUnit(unit);
        }
    }
}
