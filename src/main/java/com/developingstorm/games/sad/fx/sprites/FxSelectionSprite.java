package com.developingstorm.games.sad.fx.sprites;

import com.developingstorm.games.hexboard.Location;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Animated selection sprite that shows a "marching ants" dashed hexagonal border around a selected unit.
 * Uses pointy-top hexagon geometry matching the map canvas.
 * Animation is time-based to maintain consistent speed regardless of refresh rate.
 */
public class FxSelectionSprite extends FxSprite {

    private final double centerX;
    private final double centerY;
    private double dashOffset;
    private long lastUpdateTime;
    private static final double DASH_LENGTH = 4.0;
    private static final double DASH_SPEED_PER_SECOND = 20.0; // Pixels per second

    // Hex geometry - slightly smaller than the tile hex for inset appearance
    private static final double HEX_SIDE = 22; // Slightly smaller than tile (24)
    private static final double HEX_HALF_WIDTH = (HEX_SIDE * 1.7320508) / 2.0; // sqrt(3) * side / 2
    private static final double HEX_PEAK = HEX_SIDE / 2.0;

    public FxSelectionSprite(double centerX, double centerY, double size) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.dashOffset = 0.0;
        this.lastUpdateTime = System.nanoTime();
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Calculate time-based animation offset
        long currentTime = System.nanoTime();
        double deltaSeconds = (currentTime - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = currentTime;

        // Update dash offset based on elapsed time
        dashOffset =
            (dashOffset + (DASH_SPEED_PER_SECOND * deltaSeconds)) %
            (DASH_LENGTH * 2);

        // Save the current graphics state
        gc.save();

        // Set up the dashed line pattern
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0); // Thinner line
        gc.setLineDashes(DASH_LENGTH, DASH_LENGTH);
        gc.setLineDashOffset(dashOffset);

        // Calculate the 6 vertices of a pointy-top hexagon
        // Starting from top vertex, going clockwise
        double[] xPoints = new double[7]; // 7 points to close the path
        double[] yPoints = new double[7];

        // Top vertex
        xPoints[0] = centerX;
        yPoints[0] = centerY - HEX_SIDE;

        // Upper right
        xPoints[1] = centerX + HEX_HALF_WIDTH;
        yPoints[1] = centerY - HEX_PEAK;

        // Lower right
        xPoints[2] = centerX + HEX_HALF_WIDTH;
        yPoints[2] = centerY + HEX_PEAK;

        // Bottom vertex
        xPoints[3] = centerX;
        yPoints[3] = centerY + HEX_SIDE;

        // Lower left
        xPoints[4] = centerX - HEX_HALF_WIDTH;
        yPoints[4] = centerY + HEX_PEAK;

        // Upper left
        xPoints[5] = centerX - HEX_HALF_WIDTH;
        yPoints[5] = centerY - HEX_PEAK;

        // Close the path back to top
        xPoints[6] = xPoints[0];
        yPoints[6] = yPoints[0];

        // Draw the hexagonal path
        gc.strokePolyline(xPoints, yPoints, 7);

        // Restore graphics state (clears dash pattern for other drawing)
        gc.restore();
    }

    @Override
    public boolean done() {
        // Selection sprite never auto-removes, must be explicitly removed
        return false;
    }
}
