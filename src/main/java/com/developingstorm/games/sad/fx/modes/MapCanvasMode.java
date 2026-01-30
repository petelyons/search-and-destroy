package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Interface for MapCanvas interaction modes.
 * Each mode handles user input and rendering differently.
 *
 * Pattern: Strategy pattern for UI interaction modes.
 * Inspired by Swing's BaseCommander/BaseController separation.
 */
public interface MapCanvasMode {

    /**
     * Called when mouse is pressed on the canvas.
     * @param event The mouse event
     * @param location The hex location at the mouse position
     */
    void onMousePressed(MouseEvent event, Location location);

    /**
     * Called when mouse is released on the canvas.
     * @param event The mouse event
     * @param location The hex location at the mouse position
     */
    void onMouseReleased(MouseEvent event, Location location);

    /**
     * Called when mouse is moved on the canvas (no button pressed).
     * @param event The mouse event
     * @param location The hex location at the mouse position
     */
    void onMouseMoved(MouseEvent event, Location location);

    /**
     * Called when mouse is dragged on the canvas (button pressed).
     * @param event The mouse event
     * @param location The hex location at the mouse position
     */
    void onMouseDragged(MouseEvent event, Location location);

    /**
     * Called when a key is pressed while canvas has focus.
     * @param event The key event
     * @return true if the event was handled by this mode
     */
    boolean onKeyPressed(KeyEvent event);

    /**
     * Draw mode-specific graphics on the canvas.
     * Called during each render cycle.
     * @param gc The graphics context to draw on
     */
    void draw(GraphicsContext gc);

    /**
     * Called when entering this mode.
     * Use this to initialize mode-specific state.
     */
    void enter();

    /**
     * Called when leaving this mode.
     * Use this to clean up mode-specific state.
     */
    void exit();

    /**
     * Get the mode type.
     * @return The UIMode enum value for this mode
     */
    com.developingstorm.games.sad.fx.UIMode getMode();
}
