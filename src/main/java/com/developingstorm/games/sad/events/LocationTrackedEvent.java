package com.developingstorm.games.sad.events;

import com.developingstorm.games.hexboard.Location;

/**
 * Event fired when the camera should track/focus on a location.
 * Replaces GameListener.trackLocation() callback.
 */
public class LocationTrackedEvent extends AbstractGameEvent {
    private final Location location;

    public LocationTrackedEvent(Location location) {
        super(GameEventType.LOCATION_TRACKED);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "LocationTrackedEvent[location=" + location + "]";
    }
}
