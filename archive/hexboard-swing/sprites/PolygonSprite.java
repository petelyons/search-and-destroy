package com.developingstorm.games.hexboard.sprites;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Stroke;

/**
 * 
 */
public class PolygonSprite extends StrokeSprite {

  protected Polygon poly;

  protected PolygonSprite() {
    poly = null;
  }

  public void setPolygon(Polygon p) {
    poly = p;
  }

  protected void handleFrameChange(int old, int current) {

  }

  protected void handleDraw(long time, Image[] images, Graphics2D g) {
    if (this.poly != null) {
      Color c = g.getColor();
      Stroke s = g.getStroke();

      g.setColor(this.colors[this.current]);
      g.setStroke(this.strokes[this.current]);
      g.drawPolygon(this.poly);

      g.setColor(c);
      g.setStroke(s);

    }
  }
}
