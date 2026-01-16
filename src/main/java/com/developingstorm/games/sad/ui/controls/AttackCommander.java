package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.orders.Attack;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Commander for handling naval bombardment attack selection
 */
public class AttackCommander extends BaseCommander {

    private Unit unit;
    private List<Location> validTargets;

    public AttackCommander(SaDFrame frame, Game game) {
        super(frame, game);
        this.validTargets = new ArrayList<>();
    }

    @Override
    public Location getCurrentLocation() {
        if (this.unit != null) {
            return this.unit.getLocation();
        }
        return null;
    }

    @Override
    public void choose(com.developingstorm.games.hexboard.BoardHex hex) {
        // Not used - targets selected via click
    }

    @Override
    public boolean isDraggable(
        com.developingstorm.games.hexboard.BoardHex hex
    ) {
        return false;
    }

    public void setUnit(Unit u) {
        this.unit = u;
        this.validTargets.clear();

        if (u != null) {
            // Find all adjacent hexes with enemy land units
            Location unitLoc = u.getLocation();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) continue;

                    Location target = Location.get(
                        unitLoc.x + dx,
                        unitLoc.y + dy
                    );
                    if (hasEnemyLandUnit(target)) {
                        this.validTargets.add(target);
                    }
                }
            }

            Log.info(
                "Found " +
                    this.validTargets.size() +
                    " valid bombardment targets"
            );
            updateVisualization();
        }
    }

    private boolean hasEnemyLandUnit(Location loc) {
        List<Unit> units = this.game.unitsAtLocation(loc);
        for (Unit u : units) {
            if (
                u.getOwner() != this.unit.getOwner() &&
                u.getTravel() == Travel.LAND
            ) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidTarget(Location loc) {
        return this.validTargets.contains(loc);
    }

    public void selectTarget(Location target) {
        if (!isValidTarget(target)) {
            Log.warn("Invalid bombardment target: " + target);
            cancelAttack();
            return;
        }

        // Create and issue the attack order
        this.game.postAndRunGameAction(() -> {
            Attack attack = this.unit.newAttackOrder(target);
            this.unit.assignOrder(attack);
            Log.info(this.unit, "Bombardment order set for target: " + target);
        });

        // Return to game mode
        this.frame.returnGameMode();
    }

    public void cancelAttack() {
        Log.info("Bombardment cancelled");
        BoardCanvas canvas = this.frame.getCanvas();
        canvas.clearSelected();
        this.frame.returnGameMode();
    }

    private void updateVisualization() {
        BoardCanvas canvas = this.frame.getCanvas();
        canvas.clearSelected();
        canvas.setLocationsSelected(this.validTargets, true);
    }

    public Unit getUnit() {
        return this.unit;
    }
}
