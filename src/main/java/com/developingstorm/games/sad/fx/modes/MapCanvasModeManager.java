package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.fx.UIMode;
import java.util.HashMap;
import java.util.Map;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Manages UI interaction modes for MapCanvas.
 * Delegates all user input to the active mode.
 *
 * Pattern: Strategy pattern manager.
 * Inspired by Swing's UIController.
 */
public class MapCanvasModeManager {

    private UIMode currentModeType;
    private MapCanvasMode currentMode;
    private final Map<UIMode, MapCanvasMode> modes;

    public MapCanvasModeManager() {
        this.modes = new HashMap<>();
        this.currentModeType = null; // Not initialized yet
        this.currentMode = null;
    }

    /**
     * Register a mode with the manager.
     * @param modeType The mode type
     * @param mode The mode implementation
     */
    public void registerMode(UIMode modeType, MapCanvasMode mode) {
        modes.put(modeType, mode);
    }

    /**
     * Switch to a different mode.
     * @param modeType The mode to switch to
     */
    public void switchMode(UIMode modeType) {
        if (modeType == currentModeType) {
            return;
        }

        // Exit current mode
        if (currentMode != null) {
            currentMode.exit();
        }

        // Switch to new mode
        currentModeType = modeType;
        currentMode = modes.get(modeType);

        if (currentMode == null) {
            throw new IllegalStateException("Mode not registered: " + modeType);
        }

        // Enter new mode
        currentMode.enter();
    }

    /**
     * Get the current mode type.
     */
    public UIMode getCurrentModeType() {
        return currentModeType;
    }

    /**
     * Get the current mode implementation.
     */
    public MapCanvasMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Get a specific mode implementation.
     */
    public MapCanvasMode getMode(UIMode modeType) {
        return modes.get(modeType);
    }

    // Delegation methods

    public void delegateMousePressed(MouseEvent event, Location location) {
        if (currentMode != null) {
            currentMode.onMousePressed(event, location);
        }
    }

    public void delegateMouseReleased(MouseEvent event, Location location) {
        if (currentMode != null) {
            currentMode.onMouseReleased(event, location);
        }
    }

    public void delegateMouseMoved(MouseEvent event, Location location) {
        if (currentMode != null) {
            currentMode.onMouseMoved(event, location);
        }
    }

    public void delegateMouseDragged(MouseEvent event, Location location) {
        if (currentMode != null) {
            currentMode.onMouseDragged(event, location);
        }
    }

    public boolean delegateKeyPressed(KeyEvent event) {
        if (currentMode != null) {
            return currentMode.onKeyPressed(event);
        }
        return false;
    }

    public void drawCurrentMode(GraphicsContext gc) {
        if (currentMode != null) {
            currentMode.draw(gc);
        }
    }
}
