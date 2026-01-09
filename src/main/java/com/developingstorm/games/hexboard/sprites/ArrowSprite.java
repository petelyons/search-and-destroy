package com.developingstorm.games.hexboard.sprites;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Stroke;

/**
 *  
 */
public class ArrowSprite extends StrokeSprite {

  private Point tail;
  private Point head;

  public ArrowSprite() {

    tail = null;
    head = null;

    setRate(200);

    setZPos(2);
    setRepeat(true);
    setStrokes(this.strokes);
    setColors(this.colors);

    initFrames(1, Color.DARK_GRAY);
  }

  public ArrowSprite(int frames, Color c) {

    tail = null;
    head = null;

    setRate(200);

    setZPos(2);
    setRepeat(true);
    setStrokes(this.strokes);
    setColors(this.colors);

    initFrames(frames, c);
  }

  private void initFrames(int count, Color color) {
    setFrames(count);
    strokes = new BasicStroke[count];
    colors = new Color[count];

    for (int x = 0; x < count; x++) {
      this.strokes[x] = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
          BasicStroke.JOIN_BEVEL, 5.0f, new float[] { 6.0f, 3.0f }, x);
      this.colors[x] = color;
    }
  }

  protected void handleFrameChange(int old, int current) {

  }

  protected void handleDraw(long time, Image[] images, Graphics2D g) {

    if (this.head != null && this.tail != null) {
      Color c = g.getColor();
      Stroke s = g.getStroke();

      g.setColor(this.colors[this.current]);
      g.setStroke(this.strokes[this.current]);
      drawArrow(g, this.tail.x, this.tail.y, this.head.x, this.head.y, 15, 10, 0.3f);

      g.setColor(c);
      g.setStroke(s);

    }
  }

  public void setArrow(Point tail, Point head) {

    this.tail = tail;
    this.head = head;
  }

  // found on Suns
  // website:http://forum.java.sun.com/thread.jsp?thread=378460&forum=57&message=2752293
  private static void drawArrow(Graphics2D g2d, int xCenter, int yCenter,
      int x, int y, int edgeLen, int centerLen, float headAngle) {

    double aDir = Math.atan2(xCenter - x, yCenter - y);
    g2d.drawLine(x, y, xCenter, yCenter);
    g2d.setStroke(new BasicStroke(1f)); // make the arrow head solid even if
    // dash pattern has been specified
    Polygon tmpPoly = new Polygon();
    // regardless of the length length
    tmpPoly.addPoint(x, y); // arrow tip
    tmpPoly.addPoint(x + xCor(edgeLen, aDir + headAngle),
        y + yCor(edgeLen, aDir + headAngle));
    tmpPoly.addPoint(x + xCor(centerLen, aDir), y + yCor(centerLen, aDir));
    tmpPoly.addPoint(x + xCor(edgeLen, aDir - headAngle),
        y + yCor(edgeLen, aDir - headAngle));
    tmpPoly.addPoint(x, y); // arrow tip
    g2d.drawPolygon(tmpPoly);
    g2d.fillPolygon(tmpPoly); // remove this line to leave arrow head
    // unpainted
  }

  private static int yCor(int len, double dir) {

    return (int) (len * Math.cos(dir));
  }

  private static int xCor(int len, double dir) {

    return (int) (len * Math.sin(dir));
  }

}
