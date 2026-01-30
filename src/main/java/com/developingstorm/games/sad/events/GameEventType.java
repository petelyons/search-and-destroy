package com.developingstorm.games.sad.events;

/**
 * Enumeration of all game event types.
 * This allows listeners to quickly filter events they care about.
 */
public enum GameEventType {
    // Unit events
    UNIT_SELECTED,
    UNIT_MOVED,
    UNIT_DESTROYED,
    UNIT_CREATED,
    UNIT_KILLED,
    UNIT_TRACKED,

    // Combat events
    COMBAT_RESOLVED,

    // Location events
    LOCATION_HIT,
    LOCATION_TRACKED,

    // Turn events
    TURN_STARTED,
    TURN_ENDED,
    NEW_TURN,

    // Game flow events
    GAME_PAUSED,
    GAME_RESUMED,
    GAME_OVER,
    GAME_ABORTED,
    WAITING_FOR_ORDERS,

    // Player events
    PLAYER_SELECTED,

    // City events
    CITY_CAPTURED,
    CITY_PRODUCTION_CHANGED,

    // Map events
    MAP_UPDATED,

    // Pathfinding events (for debugging/visualization)
    PATHFINDING_PROGRESS,
    PATHFINDING_ERROR,

    // General notification
    MESSAGE,
}
