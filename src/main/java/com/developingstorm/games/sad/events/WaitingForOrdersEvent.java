package com.developingstorm.games.sad.events;

/**
 * Event fired when the game is waiting for player orders.
 * This replaces the blocking wait/notify pattern in Game.pause().
 */
public class WaitingForOrdersEvent extends AbstractGameEvent {
    private final String message;

    public WaitingForOrdersEvent(String message) {
        super(GameEventType.WAITING_FOR_ORDERS);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "WaitingForOrdersEvent[message=" + message + "]";
    }
}
