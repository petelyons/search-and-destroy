package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;

/**
 * Command to disband (kill) a unit.
 * This ensures unit destruction happens on the game thread.
 *
 * UI updates happen automatically after command execution via Game.processCommands().
 */
public class DisbandUnitCommand implements GameCommand {

    private final long unitId;

    public DisbandUnitCommand(Unit unit) {
        this.unitId = unit != null ? unit.id : -1;
    }

    public DisbandUnitCommand(long unitId) {
        this.unitId = unitId;
    }

    @Override
    public void execute(Game game) {
        if (unitId < 0) {
            return;
        }

        Unit unit = game.getUnitById(unitId);
        if (unit != null) {
            game.killUnit(unit);
        }
    }
}
