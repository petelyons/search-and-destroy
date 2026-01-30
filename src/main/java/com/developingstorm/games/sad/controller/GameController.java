package com.developingstorm.games.sad.controller;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.hexboard.Location;

/**
 * Interface for controlling game execution from the UI.
 * All methods are non-blocking and thread-safe.
 *
 * This provides a clean boundary between UI and game logic:
 * - UI calls these methods to issue commands
 * - Commands are queued and executed on the game thread
 * - Results are communicated back via GameEventBus
 *
 * For JavaFX migration, the UI layer only needs to:
 * 1. Call these controller methods to issue commands
 * 2. Listen to GameEventBus for state changes
 * 3. Query GameQueryService for current state
 */
public interface GameController {

    /**
     * Issue an order to a unit.
     * The order will be queued and executed on the game thread.
     *
     * @param unit The unit to receive the order
     * @param order The order to execute
     */
    void issueOrder(Unit unit, Order order);

    /**
     * Select a unit.
     * This will trigger a UnitSelectedEvent on the event bus.
     *
     * @param unit The unit to select, or null to deselect
     */
    void selectUnit(Unit unit);

    /**
     * Resume game execution after waiting for orders.
     * Call this after a unit has been given orders.
     *
     * @param unit The unit that received orders, or null
     */
    void resumeGame(Unit unit);

    /**
     * Explicitly pause the game.
     * This is different from waiting for orders - it's a user-initiated pause.
     */
    void pauseGame();

    /**
     * Track/center on a specific location.
     *
     * @param location The location to track
     */
    void trackLocation(Location location);

    /**
     * Track/center on a specific unit.
     *
     * @param unit The unit to track
     */
    void trackUnit(Unit unit);

    /**
     * Post a generic action to be executed on the game thread.
     * Use this for actions that don't fit the other methods.
     *
     * @param action The action to execute
     */
    void postGameAction(Runnable action);

    /**
     * Post an action and immediately resume the game.
     * Equivalent to postGameAction() + resumeGame().
     *
     * @param action The action to execute
     */
    void postAndResume(Runnable action);
}
