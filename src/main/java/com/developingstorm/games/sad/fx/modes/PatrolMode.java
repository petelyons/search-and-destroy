package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
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
 * Patrol mode for setting patrol routes.
 *
 * Usage:
 * - Left-click to add waypoints to the patrol path
 * - Right-click or press ENTER to complete the patrol
 * - ESC to cancel
 *
 * For ships: Creates LINEAR patrol (back and forth between waypoints)
 * For aircraft: Creates LOOP patrol (returns to start, must end at a city)
 *
 * Matches Swing's PatrolCommander/PatrolModeController behavior.
 */
public class PatrolMode extends AbstractMapCanvasMode {

    private Unit patrolUnit;
    private java.util.List<Location> waypoints;
    private FxLineSprite currentLine;
    private java.util.List<FxLineSprite> waypointLines;

    public PatrolMode(
        MapCanvas canvas,
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        super(canvas, game, controller, query);
        this.waypoints = new java.util.ArrayList<>();
        this.waypointLines = new java.util.ArrayList<>();
    }

    @Override
    public UIMode getMode() {
        return UIMode.PATROL;
    }

    /**
     * Set the unit that will patrol.
     */
    public void setPatrolUnit(Unit unit) {
        this.patrolUnit = unit;
        this.waypoints.clear();
        this.waypointLines.clear();

        // Add unit's current location as first waypoint
        this.waypoints.add(unit.getLocation());

        this.currentLine = new FxLineSprite(Color.CYAN, 2.0);
    }

    @Override
    public void enter() {
        if (patrolUnit == null) {
            throw new IllegalStateException(
                "Must call setPatrolUnit() before entering PatrolMode"
            );
        }
    }

    @Override
    public void exit() {
        patrolUnit = null;
        waypoints.clear();
        waypointLines.clear();
        currentLine = null;
    }

    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        if (location == null) {
            return;
        }

        if (isPrimaryButton(event)) {
            // Left-click: Add waypoint
            addWaypoint(location);
        } else if (isSecondaryButton(event)) {
            // Right-click: Complete patrol
            completePatrol();
        }
    }

    /**
     * Add a waypoint to the patrol route.
     */
    private void addWaypoint(Location location) {
        // Don't add duplicate consecutive waypoints
        if (
            !waypoints.isEmpty() &&
            waypoints.get(waypoints.size() - 1).equals(location)
        ) {
            return;
        }

        waypoints.add(location);

        // Create a line sprite from the previous waypoint to this one
        if (waypoints.size() >= 2) {
            Location prev = waypoints.get(waypoints.size() - 2);
            double[] prevCenter = canvas.getHexCenter(prev);
            double[] currentCenter = canvas.getHexCenter(location);

            FxLineSprite waypointLine = new FxLineSprite(Color.CYAN, 2.0);
            waypointLine.setLine(
                prevCenter[0],
                prevCenter[1],
                currentCenter[0],
                currentCenter[1]
            );
            waypointLines.add(waypointLine);
        }

        canvas.refresh();
    }

    /**
     * Complete the patrol and issue the order.
     */
    private void completePatrol() {
        // Need at least 2 waypoints for a valid patrol
        if (waypoints.size() < 2) {
            System.out.println("Need at least 2 waypoints for patrol");
            canvas.getModeManager().switchMode(UIMode.GAME);
            return;
        }

        // Determine patrol mode based on unit type
        com.developingstorm.games.sad.orders.Patrol.PatrolMode mode;

        if (
            patrolUnit.getTravel() == com.developingstorm.games.sad.Travel.AIR
        ) {
            // Air units must loop back to start (which should be a city)
            Location startLoc = waypoints.get(0);
            Location endLoc = waypoints.get(waypoints.size() - 1);

            // If not already a loop, close it
            if (!startLoc.equals(endLoc)) {
                waypoints.add(startLoc);
            }

            // Validate that start is a city
            City startCity = game.getBoard().getCity(startLoc);
            if (startCity == null) {
                System.out.println("Air patrol must start at a city");
                canvas.getModeManager().switchMode(UIMode.GAME);
                return;
            }

            mode = com.developingstorm.games.sad.orders.Patrol.PatrolMode.LOOP;
        } else {
            // Ships use linear patrol (back and forth)
            mode =
                com.developingstorm.games.sad.orders.Patrol.PatrolMode.LINEAR;
        }

        // Create and issue the patrol order
        try {
            com.developingstorm.games.sad.orders.Patrol patrolOrder =
                new com.developingstorm.games.sad.orders.Patrol(
                    game,
                    patrolUnit,
                    waypoints,
                    mode
                );

            controller.issueOrder(patrolUnit, patrolOrder);
            controller.resumeGame(patrolUnit);
        } catch (Exception e) {
            System.out.println("Failed to create patrol: " + e.getMessage());
        }

        // Return to game mode
        canvas.getModeManager().switchMode(UIMode.GAME);
        canvas.refresh();
    }

    @Override
    public void onMouseMoved(MouseEvent event, Location location) {
        // Update line from last waypoint to cursor
        if (patrolUnit != null && currentLine != null && !waypoints.isEmpty()) {
            Location lastWaypoint = waypoints.get(waypoints.size() - 1);
            double[] lastCenter = canvas.getHexCenter(lastWaypoint);
            double[] cursorPos = { event.getX(), event.getY() };

            currentLine.setLine(
                lastCenter[0],
                lastCenter[1],
                cursorPos[0],
                cursorPos[1]
            );
            canvas.refresh();
        }
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ESCAPE:
                // Cancel patrol mode
                canvas.getModeManager().switchMode(UIMode.GAME);
                return true;
            case ENTER:
                // Complete patrol
                completePatrol();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Draw all waypoint lines
        for (FxLineSprite line : waypointLines) {
            line.draw(gc);
        }

        // Draw current line from last waypoint to cursor
        if (currentLine != null) {
            currentLine.draw(gc);
        }

        // Draw waypoint markers
        gc.setFill(Color.CYAN);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(2.0);

        for (int i = 0; i < waypoints.size(); i++) {
            Location wp = waypoints.get(i);
            double[] center = canvas.getHexCenter(wp);
            double radius = (i == 0) ? 6.0 : 4.0; // First waypoint is larger

            gc.fillOval(
                center[0] - radius,
                center[1] - radius,
                radius * 2,
                radius * 2
            );
            gc.strokeOval(
                center[0] - radius,
                center[1] - radius,
                radius * 2,
                radius * 2
            );
        }
    }
}
