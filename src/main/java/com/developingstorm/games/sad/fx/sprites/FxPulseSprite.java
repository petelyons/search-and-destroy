package com.developingstorm.games.sad.fx.sprites;

import java.util.function.LongSupplier;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Sonar-style pulse for a patrol that has spotted something: hexagonal rings ripple out from the
 * unit's tile to the edge of its vision, settle there briefly, and fade. Removes itself when done.
 */
public class FxPulseSprite extends FxSprite {

    static final int RINGS = 3;
    static final long STAGGER_MS = 450;
    static final long RING_MS = 1400;
    static final long TOTAL_MS = (RINGS - 1) * STAGGER_MS + RING_MS;

    /** Share of a ring's life spent expanding; the rest it sits on the vision edge fading out. */
    private static final double EXPAND_SHARE = 0.7;
    private static final double START_ALPHA = 0.9;

    // Map hex geometry (pointy-top, side 24), matching MapCanvas
    private static final double HEX_SIDE = 24;
    private static final double HEX_WIDTH = HEX_SIDE * 1.7320508;
    private static final double START_RADIUS = HEX_SIDE * 0.6;

    private static final Color COLOUR = Color.web("#e9b13c"); // the Reports panel's sighting amber

    private final double centerX;
    private final double centerY;
    private final double maxRadius;
    private final LongSupplier clock;
    private final long start;

    public FxPulseSprite(double centerX, double centerY, int visionHexes) {
        this(centerX, centerY, visionHexes, System::currentTimeMillis);
    }

    FxPulseSprite(double centerX, double centerY, int visionHexes, LongSupplier clockMillis) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.maxRadius = outlineRadius(visionHexes);
        this.clock = clockMillis;
        this.start = clockMillis.getAsLong();
        setZPos(10);
    }

    /**
     * Corner radius of the hexagon enclosing every hex within {@code hexes} of the centre. Its
     * corners point at the six neighbour directions and sit on the far edge of the last hex.
     */
    static double outlineRadius(int hexes) {
        return hexes * HEX_WIDTH + HEX_WIDTH / 2;
    }

    /** Radius of ring {@code ring} {@code elapsed} ms after the pulse began; negative if not showing. */
    double ringRadius(int ring, long elapsed) {
        double t = ringProgress(ring, elapsed);
        if (t < 0) {
            return -1;
        }
        double grow = Math.min(1, t / EXPAND_SHARE);
        double eased = 1 - (1 - grow) * (1 - grow); // ease out: fast from the tile, slowing at the edge
        return START_RADIUS + (this.maxRadius - START_RADIUS) * eased;
    }

    /** Opacity of ring {@code ring} at {@code elapsed} ms: steady while expanding, then fading to 0. */
    double ringAlpha(int ring, long elapsed) {
        double t = ringProgress(ring, elapsed);
        if (t < 0) {
            return 0;
        }
        if (t <= EXPAND_SHARE) {
            return START_ALPHA;
        }
        return START_ALPHA * (1 - (t - EXPAND_SHARE) / (1 - EXPAND_SHARE));
    }

    /** 0..1 through the ring's life, or -1 before it starts / after it ends. */
    private static double ringProgress(int ring, long elapsed) {
        long local = elapsed - ring * STAGGER_MS;
        if (local < 0 || local > RING_MS) {
            return -1;
        }
        return (double) local / RING_MS;
    }

    @Override
    public void draw(GraphicsContext gc) {
        long elapsed = this.clock.getAsLong() - this.start;
        gc.save();
        for (int ring = 0; ring < RINGS; ring++) {
            double r = ringRadius(ring, elapsed);
            double alpha = ringAlpha(ring, elapsed);
            if (r <= 0 || alpha <= 0) {
                continue;
            }
            double[] xs = new double[6];
            double[] ys = new double[6];
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(60 * i);
                xs[i] = this.centerX + r * Math.cos(a);
                ys[i] = this.centerY + r * Math.sin(a);
            }
            gc.setFill(COLOUR.deriveColor(0, 1, 1, alpha * 0.08));
            gc.fillPolygon(xs, ys, 6);
            gc.setStroke(COLOUR.deriveColor(0, 1, 1, alpha));
            gc.setLineWidth(2.5);
            gc.strokePolygon(xs, ys, 6);
        }
        gc.restore();
    }

    @Override
    public boolean done() {
        return this.clock.getAsLong() - this.start >= TOTAL_MS;
    }
}
