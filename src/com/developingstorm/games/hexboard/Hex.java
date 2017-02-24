package com.developingstorm.games.hexboard;

import java.awt.Point;
import java.awt.Polygon;

/**
 * A basic hexagonal shape
 */
public class Hex extends Polygon {

  private Point _center;
  private Point _origin;

  public Hex(int[] xpoints, int[] ypoints) {
    super(xpoints, ypoints, 7);

    int originy = Integer.MAX_VALUE;
    int originx = Integer.MAX_VALUE;
    String s;
    for (int i = 0; i < 6; i++) {

      if (xpoints[i] < originx) {
        originx = xpoints[i];
      }
      if (ypoints[i] < originy) {
        originy = ypoints[i];
      }
    }

    int centerx = (xpoints[0] + xpoints[1] + xpoints[2] + xpoints[3]
        + xpoints[4] + xpoints[5]) / 6;
    int centery = (ypoints[0] + ypoints[1] + ypoints[2] + ypoints[3]
        + ypoints[4] + ypoints[5]) / 6;

    _center = new Point(centerx, centery);
    _origin = new Point(originx, originy);

  }

  public Point getCenter() {
    return _center;
  }

  public Point getOrigin() {
    return _origin;
  }

}
