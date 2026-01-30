package com.developingstorm.games.sad.events;

/**
 * Event fired when the game is aborted/terminated.
 * Replaces GameListener.abort() callback.
 */
public class GameAbortedEvent extends AbstractGameEvent {

    public GameAbortedEvent() {
        super(GameEventType.GAME_ABORTED);
    }

    @Override
    public String toString() {
        return "GameAbortedEvent[]";
    }
}
