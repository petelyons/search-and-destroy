package com.developingstorm.games.hexboard;

/**
 * The directions a unit may move
 */
public enum Direction {
    NORTH_WEST("NORTH_WEST"),
    NORTH_EAST("NORTH_EAST"),
    EAST("EAST"),
    SOUTH_EAST("SOUTH_EAST"),
    SOUTH_WEST("SOUTH_WEST"),
    WEST("WEST");

    private final String id;

    Direction(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
