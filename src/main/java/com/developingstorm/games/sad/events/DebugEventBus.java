package com.developingstorm.games.sad.events;

/**
 * Separate event bus for high-volume debugging events.
 *
 * This is intentionally a separate type from GameEventBus to prevent
 * accidentally using the wrong bus and to make the API more type-safe.
 *
 * Debug events include:
 * - Pathfinding algorithm progress (A* node exploration)
 * - Pathfinding errors
 * - Other high-frequency debugging information
 *
 * The debug event bus can be null/disabled during normal gameplay to
 * avoid performance overhead from publishing events nobody is listening to.
 */
public class DebugEventBus extends GameEventBus {

    /**
     * Creates a new debug event bus.
     * Debug event buses are typically created on-demand when debugging is enabled.
     */
    public DebugEventBus() {
        super();
    }
}
