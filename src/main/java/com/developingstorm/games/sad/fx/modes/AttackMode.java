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
import javafx.scene.paint.Color;

/**
 * Attack mode for targeting enemy units.
 * Shows an arrow from attacking unit to target following cursor.
 * Click on enemy unit to initiate attack.
 *
 * TODO: Implement full attack targeting logic.
 * Matches Swing's AttackCommander/AttackModeController behavior.
 */
public class AttackMode extends AbstractMapCanvasMode {

    private Unit attackingUnit;
    private FxArrowSprite arrow;

    public AttackMode(
        MapCanvas canvas,
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        super(canvas, game, controller, query);
    }

    @Override
    public UIMode getMode() {
        return UIMode.ATTACK;
    }

    /**
     * Set the unit that will attack.
     */
    public void setAttackingUnit(Unit unit) {
        this.attackingUnit = unit;
        this.arrow = new FxArrowSprite(Color.RED);
    }

    @Override
    public void enter() {
        if (attackingUnit == null) {
            throw new IllegalStateException(
                "Must call setAttackingUnit() before entering AttackMode"
            );
        }
    }

    @Override
    public void exit() {
        attackingUnit = null;
        arrow = null;
    }

    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        if (!isPrimaryButton(event)) {
            return;
        }

        Unit targetUnit = query.getUnitAtLocation(location);
        if (targetUnit != null && isValidTarget(targetUnit)) {
            // Issue bombardment attack order
            com.developingstorm.games.sad.orders.Attack attackOrder =
                attackingUnit.newAttackOrder(location);
            controller.issueOrder(attackingUnit, attackOrder);
            controller.resumeGame(attackingUnit);

            // Return to game mode
            canvas.getModeManager().switchMode(UIMode.GAME);
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event, Location location) {
        // Update arrow to follow cursor
        if (attackingUnit != null && arrow != null) {
            double[] unitCenter = canvas.getHexCenter(
                attackingUnit.getLocation()
            );
            double[] cursorPos = { event.getX(), event.getY() };

            // Change arrow color based on whether target is valid
            Unit targetUnit = query.getUnitAtLocation(location);
            if (targetUnit != null && isValidTarget(targetUnit)) {
                arrow = new FxArrowSprite(Color.RED);
            } else {
                arrow = new FxArrowSprite(Color.GRAY);
            }

            arrow.setArrow(
                unitCenter[0],
                unitCenter[1],
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
        if (arrow != null) {
            arrow.draw(gc);
        }
    }

    /**
     * Check if a unit is a valid attack target.
     */
    private boolean isValidTarget(Unit target) {
        if (target == null || attackingUnit == null) {
            return false;
        }

        // Can't attack own units
        if (target.getOwner() == attackingUnit.getOwner()) {
            return false;
        }

        // TODO: Check range, line of sight, etc.
        return true;
    }
}
