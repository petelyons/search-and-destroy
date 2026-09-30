package com.developingstorm.games.sad.events;

import com.developingstorm.games.sad.Player;

/**
 * Fired after a player has been swapped between Human and AI control.
 * The carried Player is the new instance.
 */
public class PlayerTypeChangedEvent extends AbstractGameEvent {

    private final Player player;

    public PlayerTypeChangedEvent(Player player) {
        super(GameEventType.PLAYER_TYPE_CHANGED);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public String toString() {
        return "PlayerTypeChangedEvent[player=" + player + ", isRobot=" + player.isRobot() + "]";
    }
}
