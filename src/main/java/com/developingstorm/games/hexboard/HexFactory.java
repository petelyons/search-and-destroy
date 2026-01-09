package com.developingstorm.games.hexboard;

/**
 * A HexFactory builds hexes of a certain size
 */
public class HexFactory {
  private double hexHalfWidth;
  private double hexPeak;
  private double hexSide;
  private double hexWidth;

  public HexFactory(int hexSide) {
    this.hexSide = hexSide;
    hexWidth = (this.hexSide * 1.7320508);
    hexHalfWidth = (this.hexWidth / 2.0);
    hexPeak = (this.hexSide / 2.0);
  }

  public int getHexSide() {
    return (int) hexSide;
  }

  public Hex newHex(Location loc) {
    return newHex(loc.x, loc.y);
  }

  public Hex newHex(int x, int y) {

    double[] xCoord = new double[6];
    double[] yCoord = new double[6];

    double x2;
    double y2;

    if (y % 2 != 0)
      x2 = hexHalfWidth;
    else
      x2 = 0;

    y2 = y * (this.hexPeak + this.hexSide) + hexPeak;

    xCoord[3] = x2 + x * hexWidth;
    yCoord[3] = y2;
    xCoord[2] = xCoord[3] + hexHalfWidth;
    yCoord[2] = yCoord[3] - hexPeak;
    xCoord[1] = xCoord[3] + hexWidth;
    yCoord[1] = yCoord[3];
    xCoord[0] = xCoord[1];
    yCoord[0] = yCoord[1] + hexSide;
    xCoord[5] = xCoord[2];
    yCoord[5] = yCoord[3] + this.hexSide + hexPeak;
    xCoord[4] = xCoord[3];
    yCoord[4] = yCoord[0];

    int px[] = new int[7];
    int py[] = new int[7];

    for (int i = 0; i < 6; i++) {
      px[i] = (int) (xCoord[i]);
      py[i] = (int) (yCoord[i]);
    }
    px[6] = px[0];
    py[6] = py[0];

    return new Hex(px, py);
  }

}
