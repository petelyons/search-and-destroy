package com.developingstorm.games.sad.ui.sprites;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;

import com.developingstorm.games.hexboard.sprites.ImageSprite;
import com.developingstorm.games.sad.ui.GameIcons;

/**

 * 
 */
public class ExplosionSprite extends ImageSprite {

  protected int[] imageSelectors;
  private Point point;

  public ExplosionSprite() {
    int[] imgs = new int[5];
    imgs[0] = GameIcons.iEXPLOSION0;
    imgs[1] = GameIcons.iEXPLOSION1;
    imgs[2] = GameIcons.iEXPLOSION2;
    imgs[3] = GameIcons.iEXPLOSION3;
    imgs[4] = GameIcons.iEXPLOSION4;
    setImageSelectors(imgs);

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
