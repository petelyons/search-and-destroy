package com.developingstorm.games.sad;

/**
 * Types of edicts that can be issued to cities
 */
public enum EdictType {
    NONE("None"),
    SEND_LAND_UNITS("SendLandUnits"),
    SEND_SEA_UNITS("SendSeaUnits"),
    SEND_AIR_UNITS("SendAirUnits"),
    AUTO_SENTRY("AutomaticSentry"),
    AIR_PATROL("AirPatrol");

    private final String displayName;

    EdictType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    // Compatibility method for migration from custom enum
    public String getName() {
        return name();
    }
}
