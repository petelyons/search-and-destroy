package com.developingstorm.games.hexboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.developingstorm.games.astar.AStarPosition;
import com.developingstorm.games.sad.SaDException;

/**
 * 
 */
public class Location implements AStarPosition {

  static final Direction[] s_directions = new Direction[] { Direction.NORTH_WEST, Direction.NORTH_WEST, Direction.NORTH_EAST, Direction.WEST, Direction.WEST, Direction.EAST, Direction.SOUTH_WEST, Direction.SOUTH_EAST, Direction.SOUTH_EAST };

  public final int x;
  public final int y;
  private List<Location> _neighbors;
  private Map<Direction, Location> _directionMap;
  
  
  
  private Location(int x1, int y1) {
    x = x1;
    y = y1;
  }
  
  static Location init(int x, int y) {
    return new Location(x, y);
  }
  
  public static Location get(int x, int y) {
    return LocationMap.INSTANCE.get(x, y);
  }
  
  public Location relative(Direction dir) {
    Location loc = _directionMap.get(dir);
    if (loc == null) {
      if (dir == Direction.NORTH_EAST) {
        return _directionMap.get(Direction.NORTH_WEST);
      } else if (dir == Direction.NORTH_WEST) {
        return _directionMap.get(Direction.NORTH_EAST);
        
      }
      if (dir == Direction.SOUTH_EAST) {
        return _directionMap.get(Direction.SOUTH_WEST);
      } else if (dir == Direction.SOUTH_WEST) {
        return _directionMap.get(Direction.SOUTH_EAST);
        
      }
    }
    return loc;
  }
  
  private static class LocStruct {
    public int x;
    public int y;
    public LocStruct(int x1, int y1) {
      x = x1;
      y = y1;
    }
  };
  
  private LocStruct internalRelative(Direction dir) {

    if (dir == null) {
      throw new IllegalArgumentException();
    }

    int xx;
    int yy;
    boolean oddRow = ((y % 2) != 0);
    int oddMod = (oddRow) ? 1 : 0;
    int evenMod = (oddRow) ? 0 : 1;

    if (dir == Direction.NORTH_WEST) {
      xx = x - evenMod;
      yy = y - 1;
    } else if (dir == Direction.NORTH_EAST) {
      xx = x + oddMod;
      yy = y - 1;
    } else if (dir == Direction.EAST) {
      xx = x + 1;
      yy = y;
    } else if (dir == Direction.SOUTH_EAST) {
      xx = x + oddMod;
      yy = y + 1;
    } else if (dir == Direction.SOUTH_WEST) {
      xx = x - evenMod;
      yy = y + 1;
    } else { // dir == Direction.WEST
      xx = x - 1;
      yy = y;
    }

    return new LocStruct(xx, yy);
  }
  
  
  private LocStruct internalRelative(Direction dir, int count) {
    LocStruct val = null;
    for (int x = 0; x < count; x++) {
       val = internalRelative(dir);
    }
    return val;
  }
  
  
  public static void test() {
    
    Location loc1 = get(48, 9);
    Location loc2 = get(49, 47);
    int dist = loc1.distance(loc2);

  }

  public Location relative(Direction dir, int dist) {

    if (dir == null || dist <= 0) {
      throw new IllegalArgumentException();
    }

    Location c = this;
    for (int i = 0; i < dist; i++) {
      c = c.relative(dir);
      if (c == null) {
        break;
      }
    }
    return c;
  }
 


  private static void loadRow(int startX, int endX, int y, Set<Location> list, Location exclude) {
    for (int xx = startX; xx <= endX; xx++) {
      if (xx < 0 || y < 0) {
        continue;
      }
      Location loc = get(xx, y);
      if (loc == null) {
        continue;
      }
      if (exclude == null || !exclude.equals(loc)) {
        list.add(loc);
      }
    }
  }

  public List<Location> getRing(int dist) {
    if (dist == 1) {
      return _neighbors;
    }
    
    Set<Location> set = new HashSet<Location>();
    LocStruct nw = internalRelative(Direction.NORTH_WEST, dist);
    LocStruct ne = internalRelative(Direction.NORTH_EAST, dist);
    LocStruct e = internalRelative(Direction.EAST, dist);
    LocStruct se = internalRelative(Direction.SOUTH_EAST, dist);
    LocStruct sw = internalRelative(Direction.SOUTH_WEST, dist);
    LocStruct w = internalRelative(Direction.WEST, dist);
   
    /*
    Log.debug("--- Ring for: " + this + " -----");
    Log.debug("NW is " + nw);
    Log.debug("NE is " + ne);
    Log.debug("W is " + w);
    Log.debug("E is " + e);
    Log.debug("SW is " + sw);
    Log.debug("SE is " + se);
    */


    int startX = nw.x;
    int endX = ne.x;
    int startY = ne.y;
    for (int yn = startX; yn < y; yn++) {
      loadRow(startX, endX, startY, set, null);
      startX--;
      endX++;
    }
    loadRow(w.x, e.x, y, set, this);
    startX = sw.x;
    endX = se.x;
    startY = se.y;
    for (int yn = startY; yn > y; yn--) {
      loadRow(startX, endX, startY, set, null);
      startX--;
      endX++;
    }
    return new ArrayList<Location>(set);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + x;
    result = prime * result + y;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Location other = (Location) obj;
    if (x != other.x)
      return false;
    if (y != other.y)
      return false;
    return true;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(x);
    sb.append(',');
    sb.append(y);
    return sb.toString();
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public boolean isNear(Location loc, int dist) {
    int d = distance(loc);
    return d <= dist;
  }

  public Location closest(ArrayList<Location> list) {
   
    Location best = null;
    for (Location test : list) {
      if (test == this)
        continue;
      if (best == null) {
        best = test;
      } else {
        if (distance(test) < distance(best)) {
          best = test;
        }
      }
    }
    return best;
  }

  public Direction direction(Location loc) {
    int xx;
    int yy;

    if (loc.x == x)
      xx = 0;
    else if (loc.x < x)
      xx = -1;
    else
      xx = 1;
    if (loc.y == y)
      yy = 0;
    else if (loc.y < y)
      yy = -1;
    else
      yy = 1;

    int offset = (3 * (yy + 1) + (xx + 1));
    return s_directions[offset];
  }
  
  

  public int distance(Location loc) {
  
    if (loc.y == y && loc.x == x) {
      return 0;
    }
    if (_neighbors.contains(loc)) {
      return 1;
    }
    
    int dist = 0;
    Location walker = this;
    while (true) {
      Direction dir = walker.direction(loc);
      walker = walker.relative(dir);
      //Log.debug("MOVED " + dir + " to " + walker);
      if (walker == null) {
        throw new SaDException("Could not calculate distance between " + this + " and " + loc);
      }
      dist++;
      if (dist > 100) {
       
        throw new SaDException("Could not calculate distance between " + this + " and " + loc);
      }
      if (walker.equals(loc)) {
        return dist;
      }
    }
  }

  
 
  
  void addNeighbor(Location loc) {
    if (loc != null)
      _neighbors.add(loc);
  }

  void addDir(Direction dir, Location loc) {
    if (loc != null)
      _directionMap.put(dir, loc);
  }

  void initNeighbors() {
    ArrayList<Location> list = new ArrayList<Location>();
    LocStruct nw = internalRelative(Direction.NORTH_WEST);
    LocStruct ne = internalRelative(Direction.NORTH_EAST);
    LocStruct e = internalRelative(Direction.EAST);
    LocStruct se = internalRelative(Direction.SOUTH_EAST);
    LocStruct sw = internalRelative(Direction.SOUTH_WEST);
    LocStruct w = internalRelative(Direction.WEST);
    _neighbors = new ArrayList<Location>();
    addNeighbor(get(nw));
    addNeighbor(get(ne));
    addNeighbor(get(e));
    addNeighbor(get(se));
    addNeighbor(get(sw));
    addNeighbor(get(w));
    _directionMap = new HashMap<Direction, Location>();
    
    addDir(Direction.NORTH_WEST, get(nw));
    addDir(Direction.NORTH_EAST, get(ne));
    addDir(Direction.EAST, get(e));
    addDir(Direction.SOUTH_EAST, get(se));
    addDir(Direction.SOUTH_WEST, get(sw));
    addDir(Direction.WEST, get(w));
  }

  private static Location get(LocStruct val) {
    return get(val.x, val.y);
  }

}
