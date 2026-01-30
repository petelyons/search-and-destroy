package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Unit;

/**
 * Event fired when a unit is killed/destroyed.
 * Replaces GameListener.killUnit() callback.
 */
public class UnitKilledEvent extends AbstractGameEvent {
    private final Unit unit;
    private final boolean showDeath;

    public UnitKilledEvent(Unit unit, boolean showDeath) {
        super(GameEventType.UNIT_KILLED);
        this.unit = unit;
        this.showDeath = showDeath;
    }

    public Unit getUnit() {
        return unit;
    }

    public boolean isShowDeath() {
        return showDeath;
    }

    @Override
    public String toString() {
        return "UnitKilledEvent[unit=" + unit + ", showDeath=" + showDeath + "]";
    }
}
