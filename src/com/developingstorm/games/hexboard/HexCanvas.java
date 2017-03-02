package com.developingstorm.games.hexboard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.util.List;

import javax.swing.JComponent;

import com.developingstorm.games.hexboard.sprites.ArrowSprite;
import com.developingstorm.games.hexboard.sprites.CursorSprite;
import com.developingstorm.games.hexboard.sprites.Sprite;
import com.developingstorm.games.hexboard.sprites.SpriteEngine;

/**
 * 
 *  
 */
public class HexCanvas extends JComponent implements HexBoardView {

  private HexBoardContext _ctx;
  private HexBoard _board;
  private Dimension _size;
  private SpriteEngine _sprites;
  private Image[] _images;
  private int _iconHeight;
  private int _iconWidth;
  private int _zs;
  private int _w;
  private int _h;
  private CursorSprite _cursor;
  private ArrowSprite _arrow;
  private Image _background;
  private boolean _cacheLevel0;
  protected LocationLens _lens;

  public HexCanvas(HexBoardContext ctx, HexBoard board) {

    _ctx = ctx;
    _w = ctx.getWidth();
    _h = ctx.getHeight();
    _zs = ctx.getZs();
    _board = board;
    _images = ctx.getImages();
    _sprites = new SpriteEngine(this, _images, _zs);
    _iconWidth = _images[ctx.getPrototypeHex()].getWidth(null);
    _iconHeight = _images[ctx.getPrototypeHex()].getHeight(null);

    Dimension iconSize = getIconDimension();

    int w = _board.getWidth() * iconSize.width + (iconSize.width / 2);
    int y2 = (_board.getHeight() / 2);
    int h = (y2 * iconSize.height) + (y2 * (_ctx.getHexSide() + 1));
    _size = new Dimension(w, h);
    _cacheLevel0 = true;
    _cursor = new CursorSprite();
    _arrow = new ArrowSprite();
    _sprites.add(_arrow);
    _sprites.add(_cursor);
    _board.addHexBoardView(this);
    _lens = null;
  }

  public void setLens(LocationLens lens) {
    _lens = lens;
  }

  public void setLevelZeroCache(boolean b) {
    _cacheLevel0 = b;
  }

  public void removeNotify() {

    _sprites.stop();
  }

  public Dimension getIconDimension() {

    return new Dimension(_iconWidth, _iconHeight);
  }

  public Dimension getPreferredSize() {

    if (_size == null) {
      return new Dimension(100, 100);
    }
    return _size;
  }

  private void initBackground() {

    Graphics2D g2 = (Graphics2D) _background.getGraphics();
    drawBoard(0, g2, false);
  }

  public void addSprite(Sprite s) {
    _sprites.add(s);
  }

  public void removeSprite(Sprite s) {
    _sprites.remove(s);
  }

  public void removeSprites(List<Sprite> list) {
    if (list == null) {
      return;
    }
    for (Sprite s : list) {
      removeSprite(s);
    }
  }

  public void addSprites(List<Sprite> list) {
    if (list == null) {
      return;
    }
    for (Sprite s : list) {
      addSprite(s);
    }
  }

  public void paint(Graphics g) {

    try {

      if (_background == null && _cacheLevel0 == true) {
        try {
          _background = createImage(_size.width, _size.height);
          initBackground();
        } catch (OutOfMemoryError me) {
          _background = null;
          _cacheLevel0 = false;
        }
      }

      _sprites.beginDraw((Graphics2D) g);
      for (int z = 0; z < _zs; z++) {
        if (z == 0 && _background != null) {
          g.drawImage(_background, 0, 0, null);
        } else {
          drawBoard(z, (Graphics2D) g);
        }
        _sprites.draw(z);
      }
      _sprites.endDraw();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void focusLost(BoardHex h) {

  }

  public void focusSet(BoardHex h) {

    _cursor.setHex(h.getHex());
  }

  private void drawBoard(int z, Graphics2D g, boolean useLens) {

    int w = _ctx.getWidth();
    int h = _ctx.getHeight();
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        BoardHex bh = _board.get(x, y);
        drawBoardHex(bh, z, g, useLens);
      }
    }
  }

  protected void drawBoard(int z, Graphics2D g) {
    drawBoard(z, g, true);
  }

  public void stopAmination() {

    _sprites.stop();
  }

  public void startAmination() {

    _sprites.start();
  }

  private void drawBoardHex(BoardHex bh, int z, Graphics g, boolean useLens) {

    Hex hex = bh.getHex();
    Point origin = hex.getOrigin();
    Location loc = bh.getLocation();

    if (useLens && _lens != null && _lens.isExplored(loc) == false) {
      g.drawImage(_images[_ctx.getUnexploredImageSelector()], origin.x,
          origin.y, _iconWidth, _iconHeight, null);
    } else {
      if (z == 0) {
        g.drawImage(_images[bh.getImageSelector()], origin.x, origin.y,
            _iconWidth, _iconHeight, null);
        if (_ctx.showBorder()) {
          g.setColor(_ctx.getBorderColor());
          g.drawPolygon(hex);
        }
      } else if (z == 1) {
        Color xcol = _ctx.getXorColor();
        if (bh.isFocus() && xcol != null) {
          g.setXORMode(xcol);
          g.drawImage(_images[bh.getImageSelector()], origin.x, origin.y,
              _iconWidth, _iconHeight, null);
          g.setPaintMode();
        }
        if (bh.isSelected()) {
          g.setColor(_ctx.getSelectionColor());
          g.drawPolygon(hex);
        }
      }
    }
  }

  public void setArrow(Point tail, Point head) {

    _arrow.setArrow(tail, head);
  }

}