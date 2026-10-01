package com.developingstorm.games.sad.events;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Unit;

/**
 * A human player's order was refused (cancelled) before reaching its target hex.
 */
public class MoveRefusedEvent extends AbstractGameEvent {

    private final Unit unit;
    private final Location target;

    public MoveRefusedEvent(Unit unit, Location target) {
        super(GameEventType.MOVE_REFUSED);
        this.unit = unit;
        this.target = target;
    }

    public Unit getUnit() {
        return unit;
    }

    /** The hex the unit was ordered to. */
    public Location getTarget() {
        return target;
    }
}
