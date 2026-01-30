package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Player;

/**
 * Event fired when a player is selected.
 * Replaces GameListener.selectPlayer() callback.
 */
public class PlayerSelectedEvent extends AbstractGameEvent {
    private final Player player;

    public PlayerSelectedEvent(Player player) {
        super(GameEventType.PLAYER_SELECTED);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return "PlayerSelectedEvent[player=" + player + "]";
    }
}
