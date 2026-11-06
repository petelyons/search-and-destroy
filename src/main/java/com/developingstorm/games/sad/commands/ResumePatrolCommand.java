package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;

/**
 * Put a lookout that stopped to report back on its suspended patrol.
 */
public class ResumePatrolCommand implements GameCommand {

    private final Unit unit;

    public ResumePatrolCommand(Unit unit) {
        this.unit = unit;
    }

    @Override
    public void execute(Game game) {
        if (unit == null || unit.isDead() || unit.getOwner().isRobot()) {
            return;
        }
        if (unit.resumePatrol()) {
            unit.getOwner().pushPendingPlay(unit);
        }
    }
}
