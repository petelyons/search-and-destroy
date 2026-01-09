package com.developingstorm.games.sad;

/**
 * Types of orders that can be given to units
 */
public enum OrderType {
    NONE("None"),
    EXPLORE("Explore"),
    SENTRY("Sentry"),
    MOVE("Move"),
    HEAD_HOME("Head Home"),
    SKIPTURN("Skip Turn"),
    UNLOAD("UnLoad"),
    MOVE_NORTH("North"),
    MOVE_SOUTH("South"),
    MOVE_EAST("East"),
    MOVE_WEST("West"),
    MOVE_NORTH_EAST("NorthEast"),
    MOVE_NORTH_WEST("NorthWest"),
    MOVE_SOUTH_EAST("SouthEast"),
    MOVE_SOUTH_WEST("SouthWest"),
    DISBAND("Disband");

    private final String displayName;

    OrderType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
