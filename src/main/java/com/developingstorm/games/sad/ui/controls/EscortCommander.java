package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Commander for handling escort selection
 */
public class EscortCommander extends BaseCommander {

    private Unit unit;
    private List<Location> validTargets;

    public EscortCommander(SaDFrame frame, Game game) {
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
            // Find all friendly sea units that can be escorted
            List<Unit> allUnits = u.getOwner().getUnits();
            for (Unit candidate : allUnits) {
                if (candidate.equals(u)) continue; // Can't escort self

                if (
                    candidate.getTravel() == Travel.SEA && !candidate.isDead()
                ) {
                    this.validTargets.add(candidate.getLocation());
                }
            }

            Log.info(
                "Found " + this.validTargets.size() + " valid escort targets"
            );
            updateVisualization();
        }
    }

    public boolean isValidTarget(Location loc) {
        return this.validTargets.contains(loc);
    }

    public void selectTarget(Location target) {
        if (!isValidTarget(target)) {
            Log.warn("Invalid escort target: " + target);
            cancelEscort();
            return;
        }

        // Find the unit at the target location
        Unit targetUnit = this.game.unitAtLocation(target);
        if (targetUnit == null) {
            Log.warn("No unit found at escort target: " + target);
            cancelEscort();
            return;
        }

        // Create and issue the escort order
        this.game.postAndRunGameAction(() -> {
            try {
                this.unit.orderEscort(targetUnit);
                Log.info(this.unit, "Escort order set for: " + targetUnit);
            } catch (Exception e) {
                Log.error("Failed to set escort order: " + e.getMessage());
            }
        });

        // Return to game mode
        this.frame.returnGameMode();
    }

    public void cancelEscort() {
        Log.info("Escort cancelled");
        BoardCanvas canvas = this.frame.getCanvas();
        canvas.clearSelected();
        this.frame.returnGameMode();
    }

    private void updateVisualization() {
        BoardCanvas canvas = this.frame.getCanvas();
        canvas.clearSelected();
        canvas.setLocationsSelected(this.validTargets, true);
        canvas.repaint();
    }

    public Unit getUnit() {
        return this.unit;
    }
}
