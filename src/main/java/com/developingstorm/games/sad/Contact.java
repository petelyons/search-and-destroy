package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;

/**
 * Represents what a player knows about a single enemy unit — either a live
 * sighting this turn (current) or a stale sighting from a prior turn (ghost).
 *
 * Contacts are owned and updated by {@link ContactMap}. Fields are mutable so
 * that multiple friendly spotters can merge their observations into one entry
 * per enemy unit per turn (see {@link ContactMap#updateContact}).
 */
public class Contact {

    /** Universal base ghost lifetime in turns; spotter quality adds to this. */
    public static final int BASE_GHOST_DURATION = 2;

    public final long unitId;

    public Location location;
    /** Null when {@code identified == false}. */
    public Type type;
    /** Travel class (LAND/SEA/AIR). Always known, even for unidentified contacts. */
    public final Travel travelType;
    public final Player owner;

    public int turnDetected;
    public boolean identified;
    /** Highest spotter quality that has touched this contact (0..2). */
    public int spotterQuality;

    public Contact(
        long unitId,
        Location location,
        Type type,
        Travel travelType,
        Player owner,
        int turnDetected,
        boolean identified,
        int spotterQuality
    ) {
        this.unitId = unitId;
        this.location = location;
        this.type = type;
        this.travelType = travelType;
        this.owner = owner;
        this.turnDetected = turnDetected;
        this.identified = identified;
        this.spotterQuality = spotterQuality;
    }

    public int age(int currentTurn) {
        return currentTurn - turnDetected;
    }

    public boolean isCurrent(int currentTurn) {
        return age(currentTurn) == 0;
    }

    /** Maximum age before the ghost is removed. */
    public int maxAge() {
        return BASE_GHOST_DURATION + spotterQuality;
    }

    public boolean isExpired(int currentTurn) {
        return age(currentTurn) > maxAge();
    }

    /** Turns remaining before expiry. Zero or negative means expired. */
    public int remainingLife(int currentTurn) {
        return maxAge() - age(currentTurn);
    }
}
