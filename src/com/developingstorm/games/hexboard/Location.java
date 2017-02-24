package com.developingstorm.games.hexboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.developingstorm.games.astar.AStarPosition;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.util.Log;

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
  
  private Location internalRelative(Direction dir) {

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

    return get(xx, yy);
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
 


  private static void loadRow(int startX, int endX, int y, List<Location> list, Location exclude) {
    for (int xx = startX; xx <= endX; xx++) {
      Location loc = get(xx, y);
      if (xx < 0 || y < 0) {
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
    
    Log.warn("Reliance on unverified function");
    
    ArrayList<Location> list = new ArrayList<Location>();
    Location nw = relative(Direction.NORTH_WEST, dist);
    Location ne = relative(Direction.NORTH_EAST, dist);
    Location e = relative(Direction.EAST, dist);
    Location se = relative(Direction.SOUTH_EAST, dist);
    Location sw = relative(Direction.SOUTH_WEST, dist);
    Location w = relative(Direction.WEST, dist);
   
    /*
    Log.debug("--- Ring for: " + this + " -----");
    Log.debug("NW is " + nw);
    Log.debug("NE is " + ne);
    Log.debug("W is " + w);
    Log.debug("E is " + e);
    Log.debug("SW is " + sw);
    Log.debug("SE is " + se);
    */


    Location startX = nw;
    Location endX = ne;
    for (int yy = startX.y; yy < y; yy++) {
      loadRow(startX.x, endX.x, startX.y, list, null);
      startX = startX.relative(Direction.SOUTH_WEST);
      endX = endX.relative(Direction.SOUTH_EAST);
    }
    loadRow(w.x, e.x, y, list, this);
    startX = sw;
    endX = se;
    for (int yy = startX.y; yy > y; yy--) {
      loadRow(startX.x, endX.x, startX.y, list, null);
      startX = startX.relative(Direction.NORTH_WEST);
      endX = endX.relative(Direction.NORTH_EAST);
    }
    return list;
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
    Location nw = internalRelative(Direction.NORTH_WEST);
    Location ne = internalRelative(Direction.NORTH_EAST);
    Location e = internalRelative(Direction.EAST);
    Location se = internalRelative(Direction.SOUTH_EAST);
    Location sw = internalRelative(Direction.SOUTH_WEST);
    Location w = internalRelative(Direction.WEST);
    _neighbors = new ArrayList<Location>();
    addNeighbor(nw);
    addNeighbor(ne);
    addNeighbor(e);
    addNeighbor(se);
    addNeighbor(sw);
    addNeighbor(w);
    _directionMap = new HashMap<Direction, Location>();
    
    addDir(Direction.NORTH_WEST, nw);
    addDir(Direction.NORTH_EAST, ne);
    addDir(Direction.EAST, e);
    addDir(Direction.SOUTH_EAST, se);
    addDir(Direction.SOUTH_WEST, sw);
    addDir(Direction.WEST, w);
  }

}
