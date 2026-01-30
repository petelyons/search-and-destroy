package com.developingstorm.games.sad.events;

/**
 * Base class for game events providing common functionality.
 */
public abstract class AbstractGameEvent implements GameEvent {
    private final GameEventType eventType;
    private final long timestamp;

    protected AbstractGameEvent(GameEventType eventType) {
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public GameEventType getEventType() {
        return eventType;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[type=" + eventType + ", time=" + timestamp + "]";
    }
}
