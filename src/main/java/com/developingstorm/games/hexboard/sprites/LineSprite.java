package com.developingstorm.games.hexboard.sprites;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Stroke;

/**
 * Simple line sprite for drawing lines between two points
 */
public class LineSprite extends StrokeSprite {

    private Point start;
    private Point end;

    public LineSprite() {
        this.start = null;
        this.end = null;

        setRate(200);
        setZPos(2);
        setRepeat(true);

        initFrames(1, Color.DARK_GRAY, 2.0f);
    }

    public LineSprite(Color color, float width) {
        this.start = null;
        this.end = null;

        setRate(200);
        setZPos(2);
        setRepeat(true);

        initFrames(1, color, width);
    }

    private void initFrames(int count, Color color, float width) {
        setFrames(count);
        this.strokes = new BasicStroke[count];
        this.colors = new Color[count];

        for (int x = 0; x < count; x++) {
            this.strokes[x] = new BasicStroke(width);
            this.colors[x] = color;
        }
    }

    @Override
    protected void handleFrameChange(int old, int current) {
    }

    @Override
    protected void handleDraw(long time, Image[] images, Graphics2D g) {
        if (this.start != null && this.end != null) {
            Color c = g.getColor();
            Stroke s = g.getStroke();

            g.setColor(this.colors[this.current]);
            g.setStroke(this.strokes[this.current]);
            g.drawLine(this.start.x, this.start.y, this.end.x, this.end.y);

            g.setColor(c);
            g.setStroke(s);
        }
    }

    public void setLine(Point start, Point end) {
        this.start = start;
        this.end = end;
    }
}
