package com.developingstorm.games.sad;

/**
 * Represents the current state of game execution.
 * This replaces the boolean paused flag with a proper state machine.
 */
public enum GameState {
    /**
     * Game is actively running - processing turns, moving units, etc.
     */
    RUNNING,

    /**
     * Game is waiting for player to give orders to a unit.
     * The game thread will sleep/poll instead of blocking with wait().
     */
    AWAITING_ORDERS,

    /**
     * Game has been explicitly paused by user or system.
     */
    PAUSED,

    /**
     * Game has ended (someone won or all conditions met).
     */
    GAME_OVER
}
