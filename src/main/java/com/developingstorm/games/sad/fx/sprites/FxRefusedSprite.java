package com.developingstorm.games.sad.fx.sprites;

import java.util.function.LongSupplier;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

/**
 * A red X over a hex the player tried to move a unit to but couldn't.
 * Fades out and removes itself after {@link #DURATION_MS}.
 */
public class FxRefusedSprite extends FxSprite {

    public static final long DURATION_MS = 2000;

    /** Half the length of each stroke of the X, in pixels (hex side is 24). */
    private static final double ARM = 11;

    private final double centerX;
    private final double centerY;
    private final LongSupplier clock;
    private final long start;

    public FxRefusedSprite(double centerX, double centerY) {
        this(centerX, centerY, System::currentTimeMillis);
    }

    FxRefusedSprite(double centerX, double centerY, LongSupplier clock) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.clock = clock;
        this.start = clock.getAsLong();
        setZPos(3);
    }

    /** 1.0 when shown, falling linearly to 0 at the end of its life. */
    double opacity() {
        long elapsed = this.clock.getAsLong() - this.start;
        return Math.max(0.0, 1.0 - (double) elapsed / DURATION_MS);
    }

    @Override
    public boolean done() {
        return this.clock.getAsLong() - this.start >= DURATION_MS;
    }

    @Override
    public void draw(GraphicsContext gc) {
        double a = opacity();
        if (a <= 0) {
            return;
        }
        gc.save();
        gc.setLineCap(StrokeLineCap.ROUND);
        // Dark outline first so the X reads on any terrain
        stroke(gc, Color.rgb(0, 0, 0, 0.6 * a), 6);
        stroke(gc, Color.rgb(220, 30, 30, a), 3.5);
        gc.restore();
    }

    private void stroke(GraphicsContext gc, Color color, double width) {
        gc.setStroke(color);
        gc.setLineWidth(width);
        gc.strokeLine(centerX - ARM, centerY - ARM, centerX + ARM, centerY + ARM);
        gc.strokeLine(centerX - ARM, centerY + ARM, centerX + ARM, centerY - ARM);
    }
}
