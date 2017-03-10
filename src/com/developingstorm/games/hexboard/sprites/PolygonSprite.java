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

  protected Polygon _poly;

  protected PolygonSprite() {
    _poly = null;
  }

  public void setPolygon(Polygon p) {
    _poly = p;
  }

  protected void handleFrameChange(int old, int current) {

  }

  protected void handleDraw(long time, Image[] images, Graphics2D g) {
    if (_poly != null) {
      Color c = g.getColor();
      Stroke s = g.getStroke();

      g.setColor(_colors[_current]);
      g.setStroke(_strokes[_current]);
      g.drawPolygon(_poly);

      g.setColor(c);
      g.setStroke(s);

    }
  }
}
