package com.developingstorm.games.hexboard.sprites;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;

/**

 * 
 */
public abstract class StrokeSprite extends Sprite {

  protected Stroke[] strokes;
  protected Color[] colors;

  protected StrokeSprite() {
    strokes = null;
    colors = null;
  }

  /**
   * @param colors
   *          The colors to set.
   */
  public void setColors(Color[] colors) {

    colors = colors;
  }

  /**
   * @param strokes
   *          The strokes to set.
   */
  public void setStrokes(Stroke[] strokes) {

    strokes = strokes;
  }

  protected abstract void handleFrameChange(int old, int current);

  protected abstract void handleDraw(long time, Image[] images, Graphics2D g);
}