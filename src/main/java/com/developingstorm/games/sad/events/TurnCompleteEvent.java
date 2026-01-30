package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Player;

/**
 * Event fired when a player completes their turn.
 * Includes timing and performance metrics for the turn.
 */
public class TurnCompleteEvent extends AbstractGameEvent {
    private final int turnNumber;
    private final Player player;
    private final long durationMs;
    private final int unitsPlayed;
    private final int unitsMoved;

    public TurnCompleteEvent(
        int turnNumber,
        Player player,
        long durationMs,
        int unitsPlayed,
        int unitsMoved
    ) {
        super(GameEventType.TURN_ENDED);
        this.turnNumber = turnNumber;
        this.player = player;
        this.durationMs = durationMs;
        this.unitsPlayed = unitsPlayed;
        this.unitsMoved = unitsMoved;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public Player getPlayer() {
        return player;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getUnitsPlayed() {
        return unitsPlayed;
    }

    public int getUnitsMoved() {
        return unitsMoved;
    }

    @Override
    public String toString() {
        return "TurnCompleteEvent[turn=" +
            turnNumber +
            ", player=" +
            player.getId() +
            ", duration=" +
            durationMs +
            "ms, units=" +
            unitsPlayed +
            "/" +
            unitsMoved +
            "]";
    }
}
