package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;

/**
 * Represents a command to be executed on the game thread.
 *
 * This is the foundation of the thread-safe command pattern:
 * - UI threads create commands and submit them to the game's command queue
 * - The game thread processes commands sequentially, ensuring no race conditions
 * - Commands have exclusive access to game state during execution
 *
 * Commands should be:
 * - Immutable (capture all parameters in constructor)
 * - Fast to execute (delegate heavy work to appropriate managers)
 * - Self-contained (don't rely on external mutable state)
 */
@FunctionalInterface
public interface GameCommand {

    /**
     * Execute this command on the game thread.
     *
     * @param game The game instance to operate on
     */
    void execute(Game game);
}
