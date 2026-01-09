package com.developingstorm.games.hexboard.sprites;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;

/**

 * 
 */
public abstract class ImageSprite extends Sprite {

  protected int[] imageSelectors;
  private Point point;

  protected ImageSprite() {
  }

  protected void handleFrameChange(int old, int current) {

  }

  protected void handleDraw(long time, Image[] images, Graphics2D g) {
    g.drawImage(images[this.imageSelectors[this.current]], this.point.x, this.point.y, null);
  }

  /**
   * @param imageSelectors
   *          The imageSelectors to set.
   */
  public void setImageSelectors(int[] imageSelectors) {

    imageSelectors = imageSelectors;
    setFrames(this.imageSelectors.length);
  }

  public void setPoint(Point p) {
    point = p;
  }

}
