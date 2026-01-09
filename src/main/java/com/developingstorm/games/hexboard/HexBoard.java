package com.developingstorm.games.hexboard;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.developingstorm.exceptions.ConfigException;

/*
 * Created on Nov 8, 2004
 *
 */

/**
 * HexBoard - the pure board model.
 * 
 */
public class HexBoard {

  private static Random s_rand = new Random(System.currentTimeMillis());

  private int width;
  private int height;
  private BoardHex[][] hexes;
  private HexBoardContext ctx;
  private BoardHex focus;
  private List<HexBoardView> views;

  public HexBoard(HexBoardContext ctx) {
    this.ctx = ctx;
    focus = null;
    views = new ArrayList<HexBoardView>();

    width = ctx.getWidth();
    height = ctx.getHeight();

    if (this.width > 999 || this.height > 999) {
      throw new ConfigException("board too big");
    }

    HexFactory fac = new HexFactory(ctx.getHexSide());

    hexes = new BoardHex[this.width][this.height];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Hex h = fac.newHex(x, y);
        this.hexes[x][y] = new BoardHex(this, x, y, h);
        this.hexes[x][y].setImageSelector(ctx.getTerrainImageSelector(x, y));
      }
    }
  }

  public void resetImages() {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        this.hexes[x][y].setImageSelector(this.ctx.getTerrainImageSelector(x, y));
      }
    }
  }

  HexBoardContext config() {
    return ctx;
  }

  public void addHexBoardView(HexBoardView view) {
    this.views.add(view);
  }

  private void notifyFocusLost(BoardHex h) {

    if (this.views.isEmpty() == false) {
      Iterator<HexBoardView> itr = this.views.iterator();
      while (itr.hasNext()) {
        HexBoardView v = (HexBoardView) itr.next();
        v.focusLost(h);
      }
    }

  }

  private void notifyFocusSet(BoardHex h) {
    if (this.views.isEmpty() == false) {
      Iterator<HexBoardView> itr = this.views.iterator();
      while (itr.hasNext()) {
        HexBoardView v = (HexBoardView) itr.next();
        v.focusSet(h);
      }
    }
  }

  public BoardHex getFocus() {
    return focus;
  }

  public BoardHex setFocus(BoardHex h) {
    BoardHex old = focus;
    if (old != null) {
      old.setFocus(false);
      notifyFocusLost(old);
    }

    focus = h;
    if (h != null) {
      this.focus.setFocus(true);
      notifyFocusSet(this.focus);
    }
    return old;
  }

  public void clearSelected() {
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        BoardHex h = this.hexes[x][y];
        h.setSelected(false);
      }
    }
  }

  public static void setSelected(List<BoardHex> listOfHexes, boolean b) {
    Iterator<BoardHex> itr = listOfHexes.iterator();
    while (itr.hasNext()) {
      BoardHex h = (BoardHex) itr.next();
      h.setSelected(b);
    }
  }

  public void setLocationsSelected(List<Location> listOfLocations, boolean b) {
    Iterator<Location> itr = listOfLocations.iterator();
    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      BoardHex h = get(loc);
      h.setSelected(b);
    }
  }

  public boolean onBoard(Location location) {
    return !(location.x < 0 || location.y < 0 || location.x >= this.width || location.y >= this.height);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public List<BoardHex> getRing(Location location, int dist) {
    BoardHex hex = get(location);
    return hex.getRing(dist);
  }

  public BoardHex get(Point p) {

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        if (this.hexes[x][y].contains(p)) {
          return this.hexes[x][y];
        }
      }
    }
    return null;
  }

  public BoardHex get(Location loc) {

    return this.hexes[loc.x][loc.y];
  }

  public BoardHex get(int x, int y) {

    return this.hexes[x][y];
  }

  public BoardHex random() {
    int x = Math.abs(s_rand.nextInt() % this.width);
    int y = Math.abs(s_rand.nextInt() % this.height);
    return get(x, y);
  }

}
