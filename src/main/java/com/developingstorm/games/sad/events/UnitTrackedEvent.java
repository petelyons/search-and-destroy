package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Unit;

/**
 * Event fired when the camera should track/follow a unit.
 * Replaces GameListener.trackUnit() callback.
 */
public class UnitTrackedEvent extends AbstractGameEvent {
    private final Unit unit;

    public UnitTrackedEvent(Unit unit) {
        super(GameEventType.UNIT_TRACKED);
        this.unit = unit;
    }

    public Unit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "UnitTrackedEvent[unit=" + unit + "]";
    }
}
