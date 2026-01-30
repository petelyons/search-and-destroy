package com.developingstorm.games.sad.events;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.CombatResult;

/**
 * Event fired when combat is resolved.
 * Includes the full combat result for UI display (battle history, etc).
 */
public class CombatResolvedEvent extends AbstractGameEvent {

    private final Location location;
    private final CombatResult result;

    public CombatResolvedEvent(Location location) {
        super(GameEventType.COMBAT_RESOLVED);
        this.location = location;
        this.result = null;
    }

    public CombatResolvedEvent(Location location, CombatResult result) {
        super(GameEventType.COMBAT_RESOLVED);
        this.location = location;
        this.result = result;
    }

    public Location getLocation() {
        return location;
    }

    public CombatResult getResult() {
        return result;
    }

    @Override
    public String toString() {
        return (
            "CombatResolvedEvent[location=" +
            location +
            ", result=" +
            result +
            "]"
        );
    }
}
