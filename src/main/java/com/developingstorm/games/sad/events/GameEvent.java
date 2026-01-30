package com.developingstorm.games.sad.events;

/**
 * Base interface for all game events.
 * Events represent things that have happened in the game that the UI may need to respond to.
 * All events are immutable and contain only the data needed to describe what happened.
 */
public interface GameEvent {
    /**
     * Returns the type of event for quick identification.
     */
    GameEventType getEventType();

    /**
     * Returns a timestamp of when the event occurred.
     */
    long getTimestamp();
}
