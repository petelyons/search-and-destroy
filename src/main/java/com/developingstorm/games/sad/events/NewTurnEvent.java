package com.developingstorm.games.sad.events;

/**
 * Event fired when a new turn begins.
 * Replaces GameListener.newTurn() callback.
 */
public class NewTurnEvent extends AbstractGameEvent {
    private final int turnNumber;

    public NewTurnEvent(int turnNumber) {
        super(GameEventType.NEW_TURN);
        this.turnNumber = turnNumber;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    @Override
    public String toString() {
        return "NewTurnEvent[turnNumber=" + turnNumber + "]";
    }
}
