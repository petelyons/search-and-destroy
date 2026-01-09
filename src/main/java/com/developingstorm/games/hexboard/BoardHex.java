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

    private HexBoard board;
    private boolean focus; // Only one hex will have focus
    private Location loc;
    private boolean selected; // many hexes may be selected
    private int type;
    private Hex hex;

    BoardHex(HexBoard board, int x, int y, Hex h) {
        if (
            board == null ||
            x < 0 ||
            y < 0 ||
            x >= board.getWidth() ||
            y >= board.getHeight()
        ) {
            throw new IllegalArgumentException();
        }

        this.hex = h;
        this.loc = Location.get(x, y);
        this.board = board;
        this.focus = false;
        this.selected = false;
    }

    public Point center() {
        return this.hex.getCenter();
    }

    public boolean contains(Point p) {
        if (p == null) {
            throw new IllegalArgumentException();
        }

        return this.hex.contains(p);
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
        return (this.loc.equals(h.loc));
    }

    public boolean isFocus() {
        return focus;
    }

    public boolean isSelected() {
        return selected;
    }

    public Location getLocation() {
        return loc;
    }

    public void setFocus(boolean f) {
        focus = f;
    }

    public void setSelected(boolean sel) {
        selected = sel;
    }

    public Hex getHex() {
        return hex;
    }

    public void setImageSelector(int i) {
        type = i;
    }

    public int getImageSelector() {
        return type;
    }

    public List<BoardHex> getRing(int dist) {
        List<Location> locations = this.loc.getRing(dist);
        List<BoardHex> hexes = new ArrayList<BoardHex>();

        Iterator<Location> itr = locations.iterator();
        while (itr.hasNext()) {
            Location loc = (Location) itr.next();
            if (this.board.onBoard(loc)) {
                hexes.add(this.board.get(loc));
            }
        }
        return hexes;
    }

    public int hashCode() {
        return this.loc.hashCode();
    }

    public BoardHex relative(Direction dir) {
        Location c = this.loc.relative(dir);
        if (this.board.onBoard(c)) {
            return this.board.get(c.x, c.y);
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
        return this.loc.toString();
    }
}
