package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.orders.Patrol;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Commander for defining multi-waypoint patrol paths
 */
public class PatrolCommander extends BaseCommander {

    private Unit unit;
    private List<Location> waypoints;
    private Patrol.PatrolMode mode;

    public PatrolCommander(SaDFrame frame, Game game) {
        super(frame, game);
        this.waypoints = new ArrayList<>();
        this.mode = Patrol.PatrolMode.LOOP; // Default to LOOP
    }

    public void setUnit(Unit u) {
        this.unit = u;
        this.waypoints.clear();
        // Start with the unit's current location as the first waypoint
        if (u != null) {
            this.waypoints.add(u.getLocation());
            Log.info("Starting patrol at unit location: " + u.getLocation());
            updateVisualization();
        }
    }

    public Unit getUnit() {
        return this.unit;
    }

    public Location getCurrentLocation() {
        if (this.unit != null) {
            return this.unit.getLocation();
        }
        return null;
    }

    public void choose(BoardHex hex) {
        // Not used - waypoints added via click
    }

    public boolean isDraggable(BoardHex hex) {
        return false;
    }

    /**
     * Adds a waypoint to the patrol path
     */
    public void addWaypoint(Location loc) {
        this.waypoints.add(loc);
        Log.info("Added waypoint " + this.waypoints.size() + " at " + loc);
        updateVisualization();
    }

    /**
     * Removes the last waypoint
     */
    public void removeLastWaypoint() {
        if (!this.waypoints.isEmpty()) {
            Location removed = this.waypoints.remove(this.waypoints.size() - 1);
            Log.info("Removed waypoint at " + removed);
            updateVisualization();
        }
    }

    /**
     * Gets all waypoints
     */
    public List<Location> getWaypoints() {
        return new ArrayList<>(this.waypoints);
    }

    /**
     * Gets the number of waypoints
     */
    public int getWaypointCount() {
        return this.waypoints.size();
    }

    /**
     * Sets the patrol mode
     */
    public void setMode(Patrol.PatrolMode mode) {
        this.mode = mode;
        Log.info("Patrol mode set to: " + mode);
    }

    /**
     * Gets the current patrol mode, auto-detected based on waypoints.
     * If first and last waypoints are the same location, returns LOOP, otherwise LINEAR.
     */
    public Patrol.PatrolMode getMode() {
        if (this.waypoints.size() >= 2) {
            Location first = this.waypoints.get(0);
            Location last = this.waypoints.get(this.waypoints.size() - 1);
            if (first.equals(last)) {
                return Patrol.PatrolMode.LOOP;
            }
        }
        return Patrol.PatrolMode.LINEAR;
    }

    /**
     * Validates if the current waypoints form a valid patrol
     */
    public boolean isValidPatrol() {
        if (this.waypoints.size() < 2) {
            return false;
        }

        if (this.unit == null) {
            return false;
        }

        // Validate air patrol requirements
        if (this.unit.getTravel() == Travel.AIR) {
            Location firstWaypoint = this.waypoints.get(0);
            Location lastWaypoint = this.waypoints.get(
                this.waypoints.size() - 1
            );

            City firstCity = this.game.getBoard().getCity(firstWaypoint);
            if (firstCity == null) {
                return false; // Air patrol must start at a city
            }

            Patrol.PatrolMode mode = getMode();
            if (mode == Patrol.PatrolMode.LOOP) {
                // Must start and end at same city
                if (!firstWaypoint.equals(lastWaypoint)) {
                    return false;
                }
            } else {
                // LINEAR mode - both endpoints must be cities
                City lastCity = this.game.getBoard().getCity(lastWaypoint);
                if (lastCity == null) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets a validation message explaining why the patrol is invalid
     */
    public String getValidationMessage() {
        if (this.waypoints.size() < 2) {
            return "Need at least 2 waypoints";
        }

        if (this.unit == null) {
            return "No unit selected";
        }

        if (this.unit.getTravel() == Travel.AIR) {
            Location firstWaypoint = this.waypoints.get(0);
            Location lastWaypoint = this.waypoints.get(
                this.waypoints.size() - 1
            );

            City firstCity = this.game.getBoard().getCity(firstWaypoint);
            if (firstCity == null) {
                return "Air patrol must start at a city";
            }

            Patrol.PatrolMode mode = getMode();
            if (mode == Patrol.PatrolMode.LOOP) {
                if (!firstWaypoint.equals(lastWaypoint)) {
                    return "Air patrol loop must start and end at the same city";
                }
            } else {
                City lastCity = this.game.getBoard().getCity(lastWaypoint);
                if (lastCity == null) {
                    return "Air patrol endpoints must both be cities";
                }
            }
        }

        return "Valid";
    }

    /**
     * Creates and assigns the patrol order to the unit
     */
    public boolean createPatrol() {
        if (!isValidPatrol()) {
            Log.warn("Cannot create patrol: " + getValidationMessage());
            return false;
        }

        try {
            Patrol.PatrolMode mode = getMode();
            Patrol patrol = this.unit.newPatrolOrder(this.waypoints, mode);
            this.unit.assignOrder(patrol);
            Log.info(
                "Patrol created with " +
                    this.waypoints.size() +
                    " waypoints in " +
                    mode +
                    " mode"
            );
            return true;
        } catch (Exception e) {
            Log.error("Failed to create patrol: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cancels patrol definition and returns to game mode
     */
    public void cancelPatrol() {
        this.waypoints.clear();
        this.canvas.clearSelected();
        this.frame.returnGameMode();
    }

    /**
     * Updates the visualization of waypoints on the map
     */
    private void updateVisualization() {
        if (this.canvas != null) {
            this.canvas.setLocationsSelected(this.waypoints, true);
        }
    }

    /**
     * Checks if a location is valid for the current patrol type
     */
    public boolean isValidWaypoint(Location loc) {
        // First waypoint for air units must be at a city
        if (this.unit.getTravel() == Travel.AIR && this.waypoints.isEmpty()) {
            City city = this.game.getBoard().getCity(loc);
            if (city == null) {
                return false;
            }
        }

        // Check if unit can travel to this location
        if (!this.unit.canTravel(loc)) {
            return false;
        }

        return true;
    }
}
