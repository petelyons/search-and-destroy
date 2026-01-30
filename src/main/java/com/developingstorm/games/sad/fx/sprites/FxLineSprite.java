package com.developingstorm.games.sad.fx.sprites;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * JavaFX sprite for drawing lines between two points.
 * Port of LineSprite from Swing version.
 */
public class FxLineSprite extends FxSprite {

    private double startX;
    private double startY;
    private double endX;
    private double endY;
    private Color color;
    private double width;
    private boolean dashed;

    public FxLineSprite() {
        this(Color.DARKGRAY, 2.0, false);
    }

    public FxLineSprite(Color color, double width) {
        this(color, width, false);
    }

    public FxLineSprite(Color color, double width, boolean dashed) {
        super();
        this.color = color;
        this.width = width;
        this.dashed = dashed;
        this.startX = 0;
        this.startY = 0;
        this.endX = 0;
        this.endY = 0;
        setZPos(2);
    }

    public void setLine(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Save current state
        Color oldStroke = (Color) gc.getStroke();
        double oldLineWidth = gc.getLineWidth();

        // Set line properties
        gc.setStroke(color);
        gc.setLineWidth(width);

        if (dashed) {
            // Dashed pattern: 5px dash, 5px gap (matching Swing version)
            gc.setLineDashes(5.0, 5.0);
        }

        // Draw the line
        gc.strokeLine(startX, startY, endX, endY);

        // Restore state
        if (dashed) {
            gc.setLineDashes(null); // Clear dashes
        }
        gc.setStroke(oldStroke);
        gc.setLineWidth(oldLineWidth);
    }
}
