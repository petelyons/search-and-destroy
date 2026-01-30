package com.developingstorm.games.sad.events;

/**
 * Interface for components that want to listen to game events.
 * All event callbacks are invoked on the EDT (Event Dispatch Thread) automatically.
 */
public interface GameEventListener {
    /**
     * Called when a game event occurs.
     * This method is always called on the EDT, so UI updates can be made directly.
     *
     * @param event The event that occurred
     */
    void onGameEvent(GameEvent event);

    /**
     * Optional method to filter which events this listener cares about.
     * Return null to receive all events, or an array of types to filter.
     *
     * @return Array of event types to receive, or null for all events
     */
    default GameEventType[] getInterestedEventTypes() {
        return null; // Receive all events by default
    }
}
