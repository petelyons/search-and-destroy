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

  private int _width;
  private int _height;
  private BoardHex[][] _hexes;
  private HexBoardContext _ctx;
  private BoardHex _focus;
  private List<HexBoardView> _views;

  public HexBoard(HexBoardContext ctx) {
    _ctx = ctx;
    _focus = null;
    _views = new ArrayList<HexBoardView>();

    _width = ctx.getWidth();
    _height = ctx.getHeight();

    if (_width > 999 || _height > 999) {
      throw new ConfigException("board too big");
    }

    HexFactory fac = new HexFactory(ctx.getHexSide());

    _hexes = new BoardHex[_width][_height];
    for (int x = 0; x < _width; x++) {
      for (int y = 0; y < _height; y++) {
        Hex h = fac.newHex(x, y);
        _hexes[x][y] = new BoardHex(this, x, y, h);
        _hexes[x][y].setImageSelector(ctx.getTerrainImageSelector(x, y));
      }
    }
  }

  public void resetImages() {
    for (int x = 0; x < _width; x++) {
      for (int y = 0; y < _height; y++) {
        _hexes[x][y].setImageSelector(_ctx.getTerrainImageSelector(x, y));
      }
    }
  }

  HexBoardContext config() {
    return _ctx;
  }

  public void addHexBoardView(HexBoardView view) {
    _views.add(view);
  }

  private void notifyFocusLost(BoardHex h) {

    if (_views.isEmpty() == false) {
      Iterator<HexBoardView> itr = _views.iterator();
      while (itr.hasNext()) {
        HexBoardView v = (HexBoardView) itr.next();
        v.focusLost(h);
      }
    }

  }

  private void notifyFocusSet(BoardHex h) {
    if (_views.isEmpty() == false) {
      Iterator<HexBoardView> itr = _views.iterator();
      while (itr.hasNext()) {
        HexBoardView v = (HexBoardView) itr.next();
        v.focusSet(h);
      }
    }
  }

  public BoardHex getFocus() {
    return _focus;
  }

  public BoardHex setFocus(BoardHex h) {
    BoardHex old = _focus;
    if (old != null) {
      old.setFocus(false);
      notifyFocusLost(old);
    }

    _focus = h;
    if (h != null) {
      _focus.setFocus(true);
      notifyFocusSet(_focus);
    }
    return old;
  }

  public void clearSelected() {
    for (int x = 0; x < _width; x++) {
      for (int y = 0; y < _height; y++) {
        BoardHex h = _hexes[x][y];
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
    return !(location.x < 0 || location.y < 0 || location.x >= _width || location.y >= _height);
  }

  public int getWidth() {
    return _width;
  }

  public int getHeight() {
    return _height;
  }

  public List<BoardHex> getRing(Location location, int dist) {
    BoardHex hex = get(location);
    return hex.getRing(dist);
  }

  public BoardHex get(Point p) {

    for (int x = 0; x < _width; x++) {
      for (int y = 0; y < _height; y++) {
        if (_hexes[x][y].contains(p)) {
          return _hexes[x][y];
        }
      }
    }
    return null;
  }

  public BoardHex get(Location loc) {

    return _hexes[loc.x][loc.y];
  }

  public BoardHex get(int x, int y) {

    return _hexes[x][y];
  }

  public BoardHex random() {
    int x = Math.abs(s_rand.nextInt() % _width);
    int y = Math.abs(s_rand.nextInt() % _height);
    return get(x, y);
  }

}
