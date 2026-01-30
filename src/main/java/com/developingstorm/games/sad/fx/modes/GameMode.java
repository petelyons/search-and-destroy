package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.fx.MapCanvas;
import com.developingstorm.games.sad.fx.UIMode;
import com.developingstorm.games.sad.fx.sprites.FxArrowSprite;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * Default game mode for MapCanvas.
 * Handles unit selection, dragging, and movement.
 *
 * Matches Swing's GameModeController behavior.
 */
public class GameMode extends AbstractMapCanvasMode {

    // Mouse interaction state
    private Location mouseDownLocation;
    private FxArrowSprite dragArrow;

    public GameMode(
        MapCanvas canvas,
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        super(canvas, game, controller, query);
        this.dragArrow = new FxArrowSprite(javafx.scene.paint.Color.DARKGRAY);
    }

    @Override
    public UIMode getMode() {
        return UIMode.GAME;
    }

    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        if (!isPrimaryButton(event)) {
            return;
        }

        // Check if this location has a draggable unit
        Unit selectedUnit = query.getSelectedUnit();
        if (
            selectedUnit != null && selectedUnit.getLocation().equals(location)
        ) {
            // Can drag the selected unit
            mouseDownLocation = location;
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event, Location location) {
        if (!isPrimaryButton(event)) {
            return;
        }

        // Clear drag line
        showDragLine(null, null);

        if (mouseDownLocation != null) {
            if (!mouseDownLocation.equals(location)) {
                // Dragged to a different location - move the unit
                canvas.moveUnit(query.getSelectedUnit(), location);
            } else {
                // Clicked on same location - activate it
                canvas.activate(location);
            }
            mouseDownLocation = null;
        } else {
            // No drag - just activate the clicked hex
            canvas.activate(location);
        }

        canvas.refresh();
    }

    @Override
    public void onMouseDragged(MouseEvent event, Location location) {
        if (mouseDownLocation == null) {
            return;
        }

        if (!location.equals(mouseDownLocation)) {
            // Show line from mouse down location to current drag location
            showDragLine(mouseDownLocation, location);
            canvas.refresh();
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event, Location location) {
        // Call super to track location
        super.onMouseMoved(event, location);
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        Unit selected = query.getSelectedUnit();

        switch (event.getCode()) {
            case C:
                // Center on selected unit - handled by GameView
                // GameMode doesn't handle centering (viewport concern)
                return false;
            case ESCAPE:
                // Deselect
                controller.selectUnit(null);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Draw drag arrow if active
        if (dragArrow != null && mouseDownLocation != null) {
            dragArrow.draw(gc);
        }
    }

    @Override
    public void exit() {
        // Clear any drag state when leaving mode
        mouseDownLocation = null;
        showDragLine(null, null);
    }

    /**
     * Show or hide the drag line.
     */
    private void showDragLine(Location start, Location end) {
        if (start == null || end == null) {
            // Clear arrow
            dragArrow.setArrow(0, 0, 0, 0);
        } else {
            double[] startCenter = canvas.getHexCenter(start);
            double[] endCenter = canvas.getHexCenter(end);
            dragArrow.setArrow(
                startCenter[0],
                startCenter[1],
                endCenter[0],
                endCenter[1]
            );
        }
    }
}
