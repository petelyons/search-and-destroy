package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Fleet;

public class FleetChangedEvent extends AbstractGameEvent {

    public final Fleet fleet;

    public FleetChangedEvent(Fleet fleet) {
        super(GameEventType.FLEET_CHANGED);
        this.fleet = fleet;
    }
}
