package com.developingstorm.games.sad.controller;

import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.GameState;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.hexboard.Location;
import java.util.List;

/**
 * Read-only interface for querying game state.
 * All methods are thread-safe for reading from UI thread.
 *
 * This provides a clean separation:
 * - UI uses this to display current state
 * - No game state modification allowed through this interface
 * - Can be called from any thread (EDT or game thread)
 *
 * For JavaFX migration, the UI binds to these queries and updates
 * when GameEvents indicate state has changed.
 */
public interface GameQueryService {

    /**
     * Get the current game state (RUNNING, AWAITING_ORDERS, PAUSED, GAME_OVER).
     */
    GameState getGameState();

    /**
     * Get the currently selected unit, if any.
     *
     * @return The selected unit, or null if none selected
     */
    Unit getSelectedUnit();

    /**
     * Get the current player whose turn it is.
     *
     * @return The current player
     */
    Player getCurrentPlayer();

    /**
     * Get all players in the game.
     *
     * @return Array of all players
     */
    Player[] getPlayers();

    /**
     * Get the current turn number.
     *
     * @return Current turn number
     */
    int getTurn();

    /**
     * Get the game board.
     *
     * @return The game board
     */
    Board getBoard();

    /**
     * Get all units at a specific location.
     *
     * @param location The location to query
     * @return List of units at that location (may be empty)
     */
    List<Unit> getUnitsAtLocation(Location location);

    /**
     * Get the primary unit at a location (handles multiple units intelligently).
     *
     * @param location The location to query
     * @return The primary unit at that location, or null if none
     */
    Unit getUnitAtLocation(Location location);

    /**
     * Get the city at a location, if any.
     *
     * @param location The location to query
     * @return The city at that location, or null if none
     */
    City getCityAtLocation(Location location);

    /**
     * Check if a location contains a city.
     *
     * @param location The location to check
     * @return true if there's a city at that location
     */
    boolean isCity(Location location);

    /**
     * Get all units in the game.
     *
     * @return List of all units
     */
    List<Unit> getAllUnits();

    /**
     * Get a unit by its unique ID.
     *
     * @param unitId The unit's ID
     * @return The unit, or null if not found
     */
    Unit getUnitById(long unitId);

    /**
     * Check if the game is currently paused.
     *
     * @return true if paused
     */
    boolean isPaused();

    /**
     * Get the underlying Game instance.
     * Use sparingly - prefer using the query methods.
     *
     * @return The game instance
     */
    Game getGame();
}
