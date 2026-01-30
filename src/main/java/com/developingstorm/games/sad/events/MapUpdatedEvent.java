package com.developingstorm.games.sad.events;

/**
 * Event fired when the map needs to be repainted.
 */
public class MapUpdatedEvent extends AbstractGameEvent {

    public MapUpdatedEvent() {
        super(GameEventType.MAP_UPDATED);
    }

    @Override
    public String toString() {
        return "MapUpdatedEvent[]";
    }
}
