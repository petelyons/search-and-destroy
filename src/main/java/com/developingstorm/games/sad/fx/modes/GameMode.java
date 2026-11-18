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
    private final MouseGesture gesture = new MouseGesture();
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
        gesture.pressed(location);
    }

    @Override
    public void onMouseReleased(MouseEvent event, Location location) {
        if (!isPrimaryButton(event)) {
            return;
        }

        // Clear drag line
        showDragLine(null, null);

        MouseGesture.Result r = gesture.released(location);
        switch (r.kind()) {
            case CLICK:
                canvas.activate(location);
                break;
            case DRAG:
                Unit dragged = MouseGesture.draggedUnit(r, query.getSelectedUnit());
                if (dragged != null) {
                    canvas.moveUnit(dragged, location);
                }
                // A drag that didn't start on the selected unit does nothing (never deselects)
                break;
            default:
                // The press went to another mode (attack, patrol, ...): not ours to act on
                break;
        }

        canvas.refresh();
    }

    @Override
    public void onMouseDragged(MouseEvent event, Location location) {
        Location from = gesture.origin();
        Unit selected = query.getSelectedUnit();
        if (from == null || selected == null || !selected.getLocation().equals(from)) {
            return;
        }

        if (!location.equals(from)) {
            // Show line from mouse down location to current drag location
            showDragLine(from, location);
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
        if (dragArrow != null && gesture.origin() != null) {
            dragArrow.draw(gc);
        }
    }

    @Override
    public void enter() {
        gesture.reset();
    }

    @Override
    public void exit() {
        // Clear any drag state when leaving mode
        gesture.reset();
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
