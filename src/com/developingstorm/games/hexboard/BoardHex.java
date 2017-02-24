package com.developingstorm.games.hexboard;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A Hex represents one hex area on the canvas. Hexes are addressable via the
 * x/y order
 * 
 */
public class BoardHex {

  private HexBoard _board;
  private boolean _focus; // Only one hex will have focus
  private Location _loc;
  private boolean _selected; // many hexes may be selected
  private int _type;
  private Hex _hex;

  BoardHex(HexBoard board, int x, int y, Hex h) {

    if (board == null || x < 0 || y < 0 || x >= board.getWidth()
        || y >= board.getHeight()) {
      throw new IllegalArgumentException();
    }

    _hex = h;
    _loc = Location.get(x, y);
    _board = board;
    _focus = false;
    _selected = false;
  }

  public Point center() {
    return _hex.getCenter();
  }

  public boolean contains(Point p) {

    if (p == null) {
      throw new IllegalArgumentException();
    }

    return _hex.contains(p);
  }

  /**
   * Get the Hex at a location a number of hexes away. If the location is off
   * the board it returns the edge location in the specified direction
   * 
   * @param dir
   *          - the direction to look
   * @param dist
   *          - the distance to look
   * @return a Hex
   */
  public BoardHex edgeRelative(Direction dir, int dist) {

    if (dir == null || dist <= 0) {
      throw new IllegalArgumentException();
    }

    BoardHex h = this;
    BoardHex last = this;
    for (int i = 0; i < dist; i++) {
      h = h.relative(dir);
      if (h == null) {
        return last;
      }
      last = h;
    }
    return h;
  }

  public boolean equals(Object o) {

    if (o == null) {
      return false;
    } else if (o == this) {
      return true;
    } else if (!(o instanceof BoardHex)) {
      return false;
    }
    BoardHex h = (BoardHex) o;
    return (_loc.equals(h._loc));
  }

  public boolean isFocus() {
    return _focus;
  }

  public boolean isSelected() {
    return _selected;
  }

  public Location getLocation() {

    return _loc;
  }

  public void setFocus(boolean f) {
    _focus = f;
  }

  public void setSelected(boolean sel) {
    _selected = sel;
  }

  public Hex getHex() {
    return _hex;
  }

  public void setImageSelector(int i) {
    _type = i;
  }

  public int getImageSelector() {
    return _type;
  }

  public List<BoardHex> getRing(int dist) {

    List<Location> locations = _loc.getRing(dist);
    List<BoardHex> hexes = new ArrayList<BoardHex>();

    Iterator<Location> itr = locations.iterator();
    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      if (_board.onBoard(loc)) {
        hexes.add(_board.get(loc));
      }
    }
    return hexes;
  }

  public int hashCode() {

    return _loc.hashCode();
  }

  public BoardHex relative(Direction dir) {

    Location c = _loc.relative(dir);
    if (_board.onBoard(c)) {
      return _board.get(c.x, c.y);
    }
    return null;
  }

  /**
   * Get the Hex at a location a number of hexes away. If the location is off
   * the board a null is returned
   * 
   * @param dir
   *          - the direction to look
   * @param dist
   *          - the distance to look
   * @return a Hex or null
   */
  public BoardHex relative(Direction dir, int dist) {

    if (dir == null || dist <= 0) {
      throw new IllegalArgumentException();
    }

    BoardHex h = this;
    for (int i = 0; i < dist; i++) {
      h = h.relative(dir);
      if (h == null) {
        return null;
      }
    }
    return h;
  }

  public String toString() {

    return _loc.toString();
  }

}