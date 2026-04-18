package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Fleet;

public class FleetChangedEvent extends AbstractGameEvent {

    /** The affected fleet, or null if it was just disbanded. */
    public final Fleet fleet;

    public FleetChangedEvent(Fleet fleet) {
        super(GameEventType.FLEET_CHANGED);
        this.fleet = fleet;
    }
}
