package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Unit;

/**
 * Event fired when a unit is selected.
 */
public class UnitSelectedEvent extends AbstractGameEvent {
    private final Unit unit;

    public UnitSelectedEvent(Unit unit) {
        super(GameEventType.UNIT_SELECTED);
        this.unit = unit;
    }

    public Unit getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return "UnitSelectedEvent[unit=" + unit + "]";
    }
}
