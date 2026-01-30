package com.developingstorm.games.sad.events;

import com.developingstorm.games.hexboard.Location;

/**
 * Event fired when a location is hit (e.g., during combat).
 * Replaces GameListener.hitLocation() callback.
 */
public class LocationHitEvent extends AbstractGameEvent {
    private final Location location;

    public LocationHitEvent(Location location) {
        super(GameEventType.LOCATION_HIT);
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "LocationHitEvent[location=" + location + "]";
    }
}
