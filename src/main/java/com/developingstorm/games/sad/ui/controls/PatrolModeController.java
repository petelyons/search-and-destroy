package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JOptionPane;

/**
 * Controller for patrol definition mode.
 *
 * Controls:
 * - Left click: Add waypoint
 * - Double click: Add final waypoint and create patrol
 * - Right click: Remove last waypoint
 * - Enter key: Create patrol
 * - Escape key: Cancel patrol
 * - Backspace/Delete: Remove last waypoint
 *
 * Mode is auto-detected: LOOP if first and last waypoints are same, otherwise LINEAR
 */
public class PatrolModeController extends BaseController {

    private final PatrolCommander commander;
    private final SaDFrame frame;
    private final KeyListener keyListener;
    private final HexMouseListenerAdapter hexMouseListenerAdapter;
    private final HexMouseMotionListenerAdapter hexMouseMotionListenerAdapter;

    public PatrolModeController(SaDFrame frame, PatrolCommander commander) {
        this.frame = frame;
        this.commander = commander;

        keyListener = new KeyListener() {
            @Override
            public void keyPressed(KeyEvent ke) {
                switch (ke.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                        // Create patrol
                        createPatrol();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        // Cancel patrol
                        PatrolModeController.this.commander.cancelPatrol();
                        break;
                    case KeyEvent.VK_BACK_SPACE:
                    case KeyEvent.VK_DELETE:
                        // Remove last waypoint
                        PatrolModeController.this.commander.removeLastWaypoint();
                        updateStatusMessage();
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent ke) {}

            @Override
            public void keyTyped(KeyEvent ke) {}
        };

        hexMouseListenerAdapter = new HexMouseListenerAdapter(
            commander,
            new IHexMouseListener() {
                @Override
                public void hexMousePressed(MouseEvent e, BoardHex hex) {
                    // Not used - handle in clicked
                }

                @Override
                public void hexMouseReleased(MouseEvent e, BoardHex hex) {}

                @Override
                public void hexMouseClicked(MouseEvent e, BoardHex hex) {
                    int button = e.getButton();
                    Location loc = hex.getLocation();

                    if (button == MouseEvent.BUTTON1) {
                        // Check for double-click
                        if (e.getClickCount() == 2) {
                            // Double click - add final waypoint and create patrol
                            if (
                                PatrolModeController.this.commander.isValidWaypoint(
                                    loc
                                )
                            ) {
                                PatrolModeController.this.commander.addWaypoint(
                                    loc
                                );
                                updateStatusMessage();
                                createPatrol();
                            } else {
                                String msg = "Invalid waypoint";
                                if (
                                    PatrolModeController.this.commander.getUnit().getTravel() ==
                                        com.developingstorm.games.sad.Travel.AIR &&
                                    PatrolModeController.this.commander.getWaypointCount() ==
                                    0
                                ) {
                                    msg = "Air patrol must start at a city";
                                }
                                Log.warn(msg);
                                showTemporaryMessage(msg);
                            }
                        } else {
                            // Single click - add waypoint
                            if (
                                PatrolModeController.this.commander.isValidWaypoint(
                                    loc
                                )
                            ) {
                                PatrolModeController.this.commander.addWaypoint(
                                    loc
                                );
                                updateStatusMessage();

                                // Check if we clicked back at the start location
                                // If so, complete the patrol automatically
                                java.util.List<Location> waypoints =
                                    PatrolModeController.this.commander.getWaypoints();
                                if (waypoints.size() >= 3) {
                                    // Need at least start + 1 waypoint + back to start
                                    Location firstWaypoint = waypoints.get(0);
                                    Location lastWaypoint = waypoints.get(
                                        waypoints.size() - 1
                                    );
                                    if (firstWaypoint.equals(lastWaypoint)) {
                                        Log.info(
                                            "Returned to start location - completing patrol"
                                        );
                                        createPatrol();
                                    }
                                }
                            } else {
                                String msg = "Invalid waypoint";
                                if (
                                    PatrolModeController.this.commander.getUnit().getTravel() ==
                                        com.developingstorm.games.sad.Travel.AIR &&
                                    PatrolModeController.this.commander.getWaypointCount() ==
                                    0
                                ) {
                                    msg = "Air patrol must start at a city";
                                }
                                Log.warn(msg);
                                showTemporaryMessage(msg);
                            }
                        }
                    } else if (button == MouseEvent.BUTTON3) {
                        // Right click - remove last waypoint
                        PatrolModeController.this.commander.removeLastWaypoint();
                        updateStatusMessage();
                    }
                }

                @Override
                public void hexMouseEntered(MouseEvent e, BoardHex hex) {}

                @Override
                public void hexMouseExited(MouseEvent e, BoardHex hex) {}
            }
        );

        hexMouseMotionListenerAdapter = new HexMouseMotionListenerAdapter(
            commander,
            new IHexMouseMotionListener() {
                @Override
                public void hexMouseDragged(MouseEvent e, BoardHex hex) {}

                @Override
                public void hexMouseMoved(MouseEvent e, BoardHex hex) {
                    // Show line from last waypoint to cursor
                    if (
                        PatrolModeController.this.commander.getWaypointCount() >
                            0 &&
                        hex != null
                    ) {
                        java.util.List<Location> waypoints =
                            PatrolModeController.this.commander.getWaypoints();
                        Location lastWaypoint = waypoints.get(
                            waypoints.size() - 1
                        );
                        PatrolModeController.this.commander.showLine(
                            lastWaypoint,
                            hex.getLocation()
                        );
                    }
                }
            }
        );

        updateStatusMessage();
    }

    /**
     * Updates the status message shown to the user
     */
    private void updateStatusMessage() {
        int waypointCount = this.commander.getWaypointCount();
        String mode = this.commander.getMode().toString();
        String status = String.format(
            "Patrol: %d waypoints | Mode: %s | Double-click or Enter=create, Esc=cancel",
            waypointCount,
            mode
        );

        if (waypointCount >= 2) {
            String validation = this.commander.getValidationMessage();
            if (!"Valid".equals(validation)) {
                status += " | " + validation;
            }
        }

        Log.info(status);
        // Could also update a status bar or tooltip here
    }

    /**
     * Shows a temporary message to the user
     */
    private void showTemporaryMessage(String message) {
        // For now just log, could show a popup or status bar message
        Log.warn(message);
    }

    /**
     * Attempts to create the patrol
     */
    private void createPatrol() {
        if (!this.commander.isValidPatrol()) {
            String message = this.commander.getValidationMessage();
            Log.warn("Cannot create patrol: " + message);
            JOptionPane.showMessageDialog(
                this.frame,
                message,
                "Invalid Patrol",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (this.commander.createPatrol()) {
            Log.info("Patrol created successfully");
            // Clear patrol creation visuals before returning to game mode
            this.commander.clearPatrolCreationVisuals();
            this.frame.returnGameMode();
        } else {
            JOptionPane.showMessageDialog(
                this.frame,
                "Failed to create patrol",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    @Override
    public MouseListener mouseListener() {
        return hexMouseListenerAdapter;
    }

    @Override
    public MouseMotionListener mouseMotionListener() {
        return hexMouseMotionListenerAdapter;
    }

    @Override
    public KeyListener keyListener() {
        return keyListener;
    }

    @Override
    public void clearAction() {}
}
