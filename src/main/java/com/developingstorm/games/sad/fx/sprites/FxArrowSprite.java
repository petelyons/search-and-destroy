package com.developingstorm.games.sad.fx.sprites;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * JavaFX sprite for drawing arrows between two points.
 * Port of ArrowSprite from Swing version with animation support.
 */
public class FxArrowSprite extends FxSprite {

    private double tailX;
    private double tailY;
    private double headX;
    private double headY;
    private Color color;
    private double width;

    // Animation state
    private int currentFrame;
    private int totalFrames;
    private long lastUpdate;
    private long frameRate; // milliseconds per frame

    public FxArrowSprite() {
        this(Color.DARKGRAY);
    }

    public FxArrowSprite(Color color) {
        super();
        this.color = color;
        this.width = 1.0;
        this.tailX = 0;
        this.tailY = 0;
        this.headX = 0;
        this.headY = 0;
        this.currentFrame = 0;
        this.totalFrames = 9; // Match Swing default
        this.frameRate = 200; // Match Swing rate (200ms)
        this.lastUpdate = 0;
        setZPos(2);
    }

    public void setArrow(
        double tailX,
        double tailY,
        double headX,
        double headY
    ) {
        this.tailX = tailX;
        this.tailY = tailY;
        this.headX = headX;
        this.headY = headY;
    }

    /**
     * Update animation frame based on time.
     * Call this before drawing to advance the animation.
     */
    public void updateAnimation(long currentTime) {
        if (lastUpdate == 0) {
            lastUpdate = currentTime;
        }

        if (currentTime - lastUpdate > frameRate) {
            currentFrame = (currentFrame + 1) % totalFrames;
            lastUpdate = currentTime;
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Save current state
        Color oldStroke = (Color) gc.getStroke();
        Color oldFill = (Color) gc.getFill();
        double oldLineWidth = gc.getLineWidth();

        // Set properties
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(width);

        // Set dashed pattern with animated offset (matching Swing: 6px dash, 3px gap, offset by currentFrame)
        gc.setLineDashOffset(currentFrame);
        gc.setLineDashes(6.0, 3.0);

        // Draw arrow line and head
        drawArrow(
            gc,
            (int) tailX,
            (int) tailY,
            (int) headX,
            (int) headY,
            15,
            10,
            0.3
        );

        // Restore state
        gc.setLineDashes(null);
        gc.setLineDashOffset(0);
        gc.setStroke(oldStroke);
        gc.setFill(oldFill);
        gc.setLineWidth(oldLineWidth);
    }

    /**
     * Draw an arrow from tail to head with arrowhead at head position.
     * Matches the algorithm from Swing ArrowSprite.
     */
    private void drawArrow(
        GraphicsContext gc,
        int xCenter,
        int yCenter,
        int x,
        int y,
        int edgeLen,
        int centerLen,
        double headAngle
    ) {
        double aDir = Math.atan2(xCenter - x, yCenter - y);

        // Draw the line
        gc.strokeLine(x, y, xCenter, yCenter);

        // Make arrow head solid
        gc.setLineDashes(null);

        // Build arrow head polygon
        double[] xPoints = new double[5];
        double[] yPoints = new double[5];

        xPoints[0] = x; // arrow tip
        yPoints[0] = y;

        xPoints[1] = x + xCor(edgeLen, aDir + headAngle);
        yPoints[1] = y + yCor(edgeLen, aDir + headAngle);

        xPoints[2] = x + xCor(centerLen, aDir);
        yPoints[2] = y + yCor(centerLen, aDir);

        xPoints[3] = x + xCor(edgeLen, aDir - headAngle);
        yPoints[3] = y + yCor(edgeLen, aDir - headAngle);

        xPoints[4] = x; // arrow tip
        yPoints[4] = y;

        // Draw and fill polygon
        gc.strokePolygon(xPoints, yPoints, 5);
        gc.fillPolygon(xPoints, yPoints, 5);
    }

    private static double yCor(int len, double dir) {
        return len * Math.cos(dir);
    }

    private static double xCor(int len, double dir) {
        return len * Math.sin(dir);
    }
}
