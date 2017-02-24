package com.developingstorm.games.hexboard.sprites;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;

/**

 * 
 */
public abstract class ImageSprite extends Sprite {

  protected int[] _imageSelectors;
  private Point _point;

  protected ImageSprite() {
  }

  protected void handleFrameChange(int old, int current) {

  }

  protected void handleDraw(long time, Image[] images, Graphics2D g) {
    g.drawImage(images[_imageSelectors[_current]], _point.x, _point.y, null);
  }

  /**
   * @param imageSelectors
   *          The imageSelectors to set.
   */
  public void setImageSelectors(int[] imageSelectors) {

    _imageSelectors = imageSelectors;
    setFrames(_imageSelectors.length);
  }

  public void setPoint(Point p) {
    _point = p;
  }

}
