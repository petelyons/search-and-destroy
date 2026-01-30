package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.fx.MapCanvas;
import com.developingstorm.games.sad.fx.UIMode;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Abstract base class for MapCanvas modes.
 * Provides common functionality and default implementations.
 */
public abstract class AbstractMapCanvasMode implements MapCanvasMode {

    protected final MapCanvas canvas;
    protected final Game game;
    protected final GameController controller;
    protected final GameQueryService query;

    public AbstractMapCanvasMode(
        MapCanvas canvas,
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        this.canvas = canvas;
        this.game = game;
        this.controller = controller;
        this.query = query;
    }

    // Default implementations - subclasses override as needed

    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        // Default: do nothing
    }

    @Override
    public void onMouseReleased(MouseEvent event, Location location) {
        // Default: do nothing
    }

    @Override
    public void onMouseMoved(MouseEvent event, Location location) {
        // Default: track location for status display
        controller.trackLocation(location);
    }

    @Override
    public void onMouseDragged(MouseEvent event, Location location) {
        // Default: do nothing
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        // Default: not handled
        return false;
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Default: no mode-specific rendering
    }

    @Override
    public void enter() {
        // Default: no initialization needed
    }

    @Override
    public void exit() {
        // Default: no cleanup needed
    }

    /**
     * Helper method to check if mouse event is primary button.
     */
    protected boolean isPrimaryButton(MouseEvent event) {
        return event.getButton() == javafx.scene.input.MouseButton.PRIMARY;
    }

    /**
     * Helper method to check if mouse event is secondary button (right-click).
     */
    protected boolean isSecondaryButton(MouseEvent event) {
        return event.getButton() == javafx.scene.input.MouseButton.SECONDARY;
    }
}
