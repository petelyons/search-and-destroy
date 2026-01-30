package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.fx.MapCanvas;
import com.developingstorm.games.sad.fx.UIMode;
import com.developingstorm.games.sad.fx.sprites.FxLineSprite;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * Escort mode for pairing ships with transports.
 * Shows a line from escort ship following cursor.
 * Click on transport to set up escort relationship.
 *
 * TODO: Implement full escort pairing logic.
 * Matches Swing's EscortCommander/EscortModeController behavior.
 */
public class EscortMode extends AbstractMapCanvasMode {

    private Unit escortShip;
    private FxLineSprite line;

    public EscortMode(
        MapCanvas canvas,
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        super(canvas, game, controller, query);
    }

    @Override
    public UIMode getMode() {
        return UIMode.ESCORT;
    }

    /**
     * Set the ship that will escort.
     */
    public void setEscortShip(Unit ship) {
        this.escortShip = ship;
        this.line = new FxLineSprite(Color.MAGENTA, 2.0);
    }

    @Override
    public void enter() {
        if (escortShip == null) {
            throw new IllegalStateException("Must call setEscortShip() before entering EscortMode");
        }
    }

    @Override
    public void exit() {
        escortShip = null;
        line = null;
    }

    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        if (!isPrimaryButton(event)) {
            return;
        }

        Unit targetUnit = query.getUnitAtLocation(location);
        if (targetUnit != null && isValidEscortTarget(targetUnit)) {
            // TODO: Implement escort pairing
            System.out.println("EscortMode: " + escortShip.name + " escorts " + targetUnit.name);

            // Return to game mode
            canvas.getModeManager().switchMode(UIMode.GAME);
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event, Location location) {
        // Update line to follow cursor
        if (escortShip != null && line != null) {
            double[] shipCenter = canvas.getHexCenter(escortShip.getLocation());
            double[] cursorPos = {event.getX(), event.getY()};

            // Change line color based on whether target is valid
            Unit targetUnit = query.getUnitAtLocation(location);
            if (targetUnit != null && isValidEscortTarget(targetUnit)) {
                line = new FxLineSprite(Color.MAGENTA, 2.0);
            } else {
                line = new FxLineSprite(Color.GRAY, 2.0);
            }

            line.setLine(
                shipCenter[0],
                shipCenter[1],
                cursorPos[0],
                cursorPos[1]
            );
            canvas.refresh();
        }
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            canvas.getModeManager().switchMode(UIMode.GAME);
            return true;
        }
        return false;
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (line != null) {
            line.draw(gc);
        }
    }

    /**
     * Check if a unit is a valid escort target.
     */
    private boolean isValidEscortTarget(Unit target) {
        if (target == null || escortShip == null) {
            return false;
        }

        // Must be same owner
        if (target.getOwner() != escortShip.getOwner()) {
            return false;
        }

        // Can't escort self
        if (target == escortShip) {
            return false;
        }

        // TODO: Check if target is a transport, in range, etc.
        return true;
    }
}
