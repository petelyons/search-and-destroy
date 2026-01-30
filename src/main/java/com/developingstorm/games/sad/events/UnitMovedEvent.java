package com.developingstorm.games.sad.events;

import com.developingstorm.games.hexboard.Location;

/**
 * Event fired when a unit moves to a new location.
 * This provides more granular notification than MapUpdatedEvent.
 */
public class UnitMovedEvent extends AbstractGameEvent {

    private final long unitId;
    private final Location oldLocation;
    private final Location newLocation;

    public UnitMovedEvent(
        long unitId,
        Location oldLocation,
        Location newLocation
    ) {
        super(GameEventType.UNIT_MOVED);
        this.unitId = unitId;
        this.oldLocation = oldLocation;
        this.newLocation = newLocation;
    }

    public long getUnitId() {
        return unitId;
    }

    public Location getOldLocation() {
        return oldLocation;
    }

    public Location getNewLocation() {
        return newLocation;
    }

    @Override
    public String toString() {
        return (
            "UnitMovedEvent{unitId=" +
            unitId +
            ", from=" +
            oldLocation +
            ", to=" +
            newLocation +
            "}"
        );
    }
}
