package com.developingstorm.games.hexboard.sprites;

import java.awt.BasicStroke;
import java.awt.Color;
import com.developingstorm.games.hexboard.Hex;

/**
 *
 */
public class CursorSprite extends PolygonSprite {

  public CursorSprite() {

    setRate(200);
    setFrames(6);
    setZPos(2);
    setRepeat(true);

    _strokes = new BasicStroke[6];
    _colors = new Color[6];

    for (int x = 0; x < 6; x++) {
      _strokes[x] = new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
          BasicStroke.JOIN_BEVEL, 10.0f, new float[] { 3.0f, 3.0f }, x);
      _colors[x] = Color.BLACK;
    }

    setStrokes(_strokes);
    setColors(_colors);
  }

  public void setHex(Hex h) {
    setPolygon(h);
  }

}
