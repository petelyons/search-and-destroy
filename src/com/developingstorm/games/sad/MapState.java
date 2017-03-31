package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.List;

import org.omg.PortableServer.POAManagerPackage.State;

import com.developingstorm.games.astar.AStarPosition;
import com.developingstorm.games.astar.AStarState;
import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;

public class MapState implements AStarState {

  Location _loc;

  static Board _b = null;
  static Travel _travel;
  static Location _goal;
  static ArrayList<Unit> _units;
  static Player _player;
  static Game _game;
  static boolean _checkedBlocked;
  static boolean _canExplore;

  public static void start(Game game, Board b, Travel travel, Player player,
      Location goal, boolean checkBlocked, boolean canExplore) {
    _game = game;
    _b = b;
    _travel = travel;
    _goal = goal;
    _units = null;
    _player = player;
    _checkedBlocked = checkBlocked;
    _canExplore = canExplore;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.developingstorm.astar.State#key()
   */
  public AStarPosition pos() {
    return _loc;
  }

  public static void start(Board b, Travel travel, Player player,
      Location goal, ArrayList<Unit> units) {
    _b = b;
    _travel = travel;
    _goal = goal;
    _units = units;
    _player = player;
  }

  public static MapState getUntested(Location loc) {
    if (_player.isExplored(loc)) {
      if (_travel == Travel.SEA) {
        if (_b.isWater(loc) || isPlayersCity(loc)) {
          return new MapState(loc);
        }
      } else if (_travel == Travel.LAND) {
        return new MapState(loc);
      }
    }
    return new MapState(loc);

  }

  public static MapState get(Location loc) {
    if (_player.isExplored(loc) || _canExplore) {
      if (_travel == Travel.SEA) {
        if (_b.isWater(loc) || isPlayersCity(loc)) {
          if (!isBlocked(loc)) {
            return new MapState(loc);
          }
          return null;
        }
      } else if (_travel == Travel.LAND) {
        if (!isBlocked(loc)) {
          return new MapState(loc);
        }
        return null;
      } else if (!isBlocked(loc)) {
        return new MapState(loc);
      }
      return null;
    } else {
      if (_canExplore) {
        return new MapState(loc);
      }
      else
        return null;
    }

  }

  private MapState(Location loc) {
    _loc = loc;
  }

  public Location getLocation() {
    return _loc;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_loc == null) ? 0 : _loc.hashCode());
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
    MapState other = (MapState) obj;
    if (_loc == null) {
      if (other._loc != null)
        return false;
    } else if (!_loc.equals(other._loc))
      return false;
    return true;
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return _loc.toString();
  }

  /**
   * @see com.marlinspike.astar.State#estimate(State)
   */
  public int estimate(AStarState goal) {
    MapState ms = (MapState) goal;
    return _loc.distance(ms._loc);
  }

  public static boolean isBlocked(Location loc) {
    if (_checkedBlocked) {
      if (isPlayersCity(loc)) {
        return false;
      }
      if (_travel != Travel.LAND && isNonPlayersCity(loc)) {
        return true;
      }
      List<Unit> list = _game.unitsAtLocation(loc);
      if (!(list == null || list.isEmpty())) {
        Unit u = list.get(0);
        if (u != null && _player.equals(u.getOwner())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isPlayersCity(Location loc) {
    City c = _b.getCity(loc);
    if (c != null && _player != null) {
      return _player.ownsCity(c);
    }
    return false;
  }

  private static boolean isNonPlayersCity(Location loc) {
    City c = _b.getCity(loc);
    if (c != null && _player != null) {
      return !_player.ownsCity(c);
    }
    return false;
  }

  /**
   * @see com.marlinspike.astar.State#successors()
   */
  public List<AStarState> successors() {

    List<BoardHex> list = _b.getRing(_loc, 1);

    ArrayList<AStarState> v = new ArrayList<AStarState>();

    for (BoardHex bh : list) {
      Location loc = bh.getLocation();

      if (_player.isExplored(loc)) {
        if (_travel == Travel.SEA) {
          if (_b.isWater(loc) || isPlayersCity(loc)) {
            if (!isBlocked(loc))
              v.add(new MapState(loc));
          }
        } else if (_travel == Travel.LAND) {
          if (_b.isLand(loc)) {
            if (!isBlocked(loc))
              v.add(new MapState(loc));
          }
        } else {
          if (!isBlocked(loc))
            v.add(new MapState(loc));
        }
      } else {
        if (_canExplore) {
          v.add(new MapState(loc));
        }
      }
    }
    /*
    // Put the location closest to the goal first
    Direction dir = _loc.direction(_goal);
    MapState ms = new MapState(_loc.relative(dir));
    for (x = 0; x < v.size(); x++) {
      MapState temp = (MapState) v.get(x);
      if (temp.equals(ms)) {
        if (x != 0) {
          v.remove(temp);
          v.add(0, ms);
        }
        break;
      }
    }
    */
    return v;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.developingstorm.astar.State#x()
   */
  public int x() {
    return _loc.x;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.developingstorm.astar.State#y()
   */
  public int y() {
    return _loc.y;
  }

}
