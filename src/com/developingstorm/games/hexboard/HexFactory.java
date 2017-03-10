package com.developingstorm.games.hexboard;

/**
 * A HexFactory builds hexes of a certain size
 */
public class HexFactory {
  private double _hexHalfWidth;
  private double _hexPeak;
  private double _hexSide;
  private double _hexWidth;

  public HexFactory(int hexSide) {
    _hexSide = hexSide;
    _hexWidth = (_hexSide * 1.7320508);
    _hexHalfWidth = (_hexWidth / 2.0);
    _hexPeak = (_hexSide / 2.0);
  }

  public int getHexSide() {
    return (int) _hexSide;
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
      x2 = _hexHalfWidth;
    else
      x2 = 0;

    y2 = y * (_hexPeak + _hexSide) + _hexPeak;

    xCoord[3] = x2 + x * _hexWidth;
    yCoord[3] = y2;
    xCoord[2] = xCoord[3] + _hexHalfWidth;
    yCoord[2] = yCoord[3] - _hexPeak;
    xCoord[1] = xCoord[3] + _hexWidth;
    yCoord[1] = yCoord[3];
    xCoord[0] = xCoord[1];
    yCoord[0] = yCoord[1] + _hexSide;
    xCoord[5] = xCoord[2];
    yCoord[5] = yCoord[3] + _hexSide + _hexPeak;
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