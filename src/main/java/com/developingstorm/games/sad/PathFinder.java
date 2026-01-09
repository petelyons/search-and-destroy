package com.developingstorm.games.sad;

import java.util.HashSet;
import java.util.Set;

import com.developingstorm.games.hexboard.Location;

public class PathFinder {
  private Game game;
  private Unit unit;
  private Location dest;  
  private Set<Location> blacklist; // lazy init
  private boolean obstructed;
  private Board board;
 
   
  public PathFinder(Unit u, Location dest) {
    unit = u;
    this.dest = dest;
    obstructed = false;  
    game = this.unit.getOwner().getGame();
    board = this.game.getBoard();
  }
  
  public Path getPath() {
    Travel travel = this.unit.getTravel();
    
    Location start = this.unit.getLocation();
    if (start.distance(this.dest) == 1) {
      
    }
    
    if (travel == Travel.SEA && knownInvalidSeaDest(this.dest)) {
      return null;
    } else if (travel == Travel.LAND && knownInvalidLandDest(this.dest)) {
      return null;
    }
    Path p = this.game.calcTravelPath(this.unit.getOwner(), start, this.dest, travel, this.obstructed, true);
    return p;
  }
  
  private boolean knownInvalidSeaDest(Location to) {
    return (this.game.isExplored(to) && !(this.board.isCity(to) || this.board.isWater(to)));
  }
  
  private boolean knownInvalidLandDest(Location to) {
    return (this.game.isExplored(to) && !this.board.isLand(to));
  }

  public Location getDest() {
    return dest;
  }

  public void addObstruction(Location ob) {
    obstructed = true;
    if (blacklist == null) {
      blacklist = new HashSet<Location>();
    }
    this.blacklist.add(ob);
    
  }

  public boolean isKnownObstruction(Location ob) {
    if (this.blacklist != null) {
      return this.blacklist.contains(ob);
    }
    return false;
  }  

}
