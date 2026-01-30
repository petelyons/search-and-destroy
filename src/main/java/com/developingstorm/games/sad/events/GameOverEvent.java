package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Player;

/**
 * Event fired when the game ends.
 * Replaces GameListener.gameOver() callback.
 */
public class GameOverEvent extends AbstractGameEvent {
    private final Player winner;

    public GameOverEvent(Player winner) {
        super(GameEventType.GAME_OVER);
        this.winner = winner;
    }

    public Player getWinner() {
        return winner;
    }

    @Override
    public String toString() {
        return "GameOverEvent[winner=" + winner + "]";
    }
}
