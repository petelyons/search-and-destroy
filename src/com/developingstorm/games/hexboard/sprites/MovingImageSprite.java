package com.developingstorm.games.hexboard.sprites;

import java.awt.Point;

/**

 * 
 */
public abstract class MovingImageSprite extends ImageSprite {

  private Point[] _wayPoints;
  private int _speed;

  public MovingImageSprite() {
    _wayPoints = null;
  }

  protected void handleFrameChange(int old, int current) {

    // TODO: move the point...

  }

  public void setWayPoints(Point[] ps) {
    _wayPoints = ps;
  }

  public void setSpeed(int speed) {
    _speed = speed;
  }

}
