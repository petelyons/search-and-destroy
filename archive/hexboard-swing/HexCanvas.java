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

  private static final long serialVersionUID = -8867069101522503660L;
  private HexBoardContext ctx;
  private HexBoard board;
  private Dimension size;
  private SpriteEngine sprites;
  private Image[] images;
  private int iconHeight;
  private int iconWidth;
  private int zs;
  private CursorSprite cursor;
  private ArrowSprite arrow;
  private Image background;
  private boolean cacheLevel0;
  protected LocationLens lens;

  public HexCanvas(HexBoardContext ctx, HexBoard board) {

    this.ctx = ctx;
    zs = ctx.getZs();
    this.board = board;
    images = ctx.getImages();
    sprites = new SpriteEngine(this, this.images, this.zs);
    iconWidth = this.images[ctx.getPrototypeHex()].getWidth(null);
    iconHeight = this.images[ctx.getPrototypeHex()].getHeight(null);

    Dimension iconSize = getIconDimension();

    int w = this.board.getWidth() * iconSize.width + (iconSize.width / 2);
    int y2 = (this.board.getHeight() / 2);
    int h = (y2 * iconSize.height) + (y2 * (this.ctx.getHexSide() + 1));
    size = new Dimension(w, h);
    cacheLevel0 = true;
    cursor = new CursorSprite();
    arrow = new ArrowSprite();
    this.sprites.add(this.arrow);
    this.sprites.add(this.cursor);
    this.board.addHexBoardView(this);
    lens = null;
  }

  public void setLens(LocationLens lens) {
    this.lens = lens;
  }

  public void setLevelZeroCache(boolean b) {
    cacheLevel0 = b;
  }

  public void removeNotify() {

    this.sprites.stop();
  }

  public Dimension getIconDimension() {

    return new Dimension(this.iconWidth, this.iconHeight);
  }

  public Dimension getPreferredSize() {

    if (size == null) {
      return new Dimension(100, 100);
    }
    return size;
  }

  private void initBackground() {

    Graphics2D g2 = (Graphics2D) this.background.getGraphics();
    drawBoard(0, g2, false);
  }

  public void addSprite(Sprite s) {
    this.sprites.add(s);
  }

  public void removeSprite(Sprite s) {
    this.sprites.remove(s);
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

      if (background == null && cacheLevel0 == true) {
        try {
          background = createImage(this.size.width, this.size.height);
          initBackground();
        } catch (OutOfMemoryError me) {
          background = null;
          cacheLevel0 = false;
        }
      }

      this.sprites.beginDraw((Graphics2D) g);
      for (int z = 0; z < zs; z++) {
        if (z == 0 && this.background != null) {
          g.drawImage(this.background, 0, 0, null);
        } else {
          drawBoard(z, (Graphics2D) g);
        }
        this.sprites.draw(z);
      }
      this.sprites.endDraw();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void focusLost(BoardHex h) {

  }

  public void focusSet(BoardHex h) {

    this.cursor.setHex(h.getHex());
  }

  private void drawBoard(int z, Graphics2D g, boolean useLens) {

    int w = this.ctx.getWidth();
    int h = this.ctx.getHeight();
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        BoardHex bh = this.board.get(x, y);
        drawBoardHex(bh, z, g, useLens);
      }
    }
  }

  protected void drawBoard(int z, Graphics2D g) {
    drawBoard(z, g, true);
  }

  public void stopAmination() {

    this.sprites.stop();
  }

  public void startAmination() {

    this.sprites.start();
  }

  private void drawBoardHex(BoardHex bh, int z, Graphics g, boolean useLens) {

    Hex hex = bh.getHex();
    Point origin = hex.getOrigin();
    Location loc = bh.getLocation();

    if (useLens && this.lens != null && this.lens.isExplored(loc) == false) {
      g.drawImage(this.images[this.ctx.getUnexploredImageSelector()], origin.x,
          origin.y, this.iconWidth, this.iconHeight, null);
    } else {
      if (z == 0) {
        g.drawImage(this.images[bh.getImageSelector()], origin.x, origin.y,
            this.iconWidth, this.iconHeight, null);
        if (this.ctx.showBorder()) {
          g.setColor(this.ctx.getBorderColor());
          g.drawPolygon(hex);
        }
      } else if (z == 1) {
        Color xcol = this.ctx.getXorColor();
        if (bh.isFocus() && xcol != null) {
          g.setXORMode(xcol);
          g.drawImage(this.images[bh.getImageSelector()], origin.x, origin.y,
              this.iconWidth, this.iconHeight, null);
          g.setPaintMode();
        }
        if (bh.isSelected()) {
          g.setColor(this.ctx.getSelectionColor());
          g.drawPolygon(hex);
        }
      }
    }
  }

  public void setArrow(Point tail, Point head) {

    this.arrow.setArrow(tail, head);
  }

}
