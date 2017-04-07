package com.developingstorm.games.sad;

import java.util.HashSet;
import java.util.Set;

import com.developingstorm.games.hexboard.Location;

public class PathFinder {
  private Game _game;
  private Unit _unit;
  private Location _dest;  
  private Set<Location> _blacklist; // lazy init
  private boolean _obstructed;
  private Board _board;
 
   
  public PathFinder(Unit u, Location dest) {
    _unit = u;
    _dest = dest;
    _obstructed = false;  
    _game = _unit.getOwner().getGame();
    _board = _game.getBoard();
  }
  
  public Path getPath() {
    Travel travel = _unit.getTravel();
    
    Location start = _unit.getLocation();
    if (start.distance(_dest) == 1) {
      
    }
    
    if (travel == Travel.SEA && knownInvalidSeaDest(_dest)) {
      return null;
    } else if (travel == Travel.LAND && knownInvalidLandDest(_dest)) {
      return null;
    }
    Path p = _game.calcTravelPath(_unit.getOwner(), start, _dest, travel, _obstructed, true);
    return p;
  }
  
  private boolean knownInvalidSeaDest(Location to) {
    return (_game.isExplored(to) && !(_board.isCity(to) || _board.isWater(to)));
  }
  
  private boolean knownInvalidLandDest(Location to) {
    return (_game.isExplored(to) && !_board.isLand(to));
  }

  public Location getDest() {
    return _dest;
  }

  public void addObstruction(Location ob) {
    _obstructed = true;
    if (_blacklist == null) {
      _blacklist = new HashSet<Location>();
    }
    _blacklist.add(ob);
    
  }

  public boolean isKnownObstruction(Location ob) {
    if (_blacklist != null) {
      return _blacklist.contains(ob);
    }
    return false;
  }  

}
