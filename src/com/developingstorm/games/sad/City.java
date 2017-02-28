package com.developingstorm.games.sad;

import java.util.List;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;

public class City {

  private volatile String _name;
  private volatile Player _owner;
  private volatile Type _produces;
  private volatile Location _location;
  private volatile long _round;
  private volatile long _productionStart;
  private volatile Board _board;
  private volatile Game _game;
  private volatile boolean _productionCompletedThisTurn;

  private volatile City _seaPath;
  private volatile City _airPath;
  private volatile City _groundPath;

  public City(Location loc, Game game) {
    _name = GameNames.getName();
    _owner = null;
    _produces = null;
    _location = loc;
    _round = 0;
    _productionStart = -1;
    _game = game;
    _board = _game.getBoard();
    _productionCompletedThisTurn = false;
    _seaPath = null;
    _airPath = null;
    _groundPath = null;
  }

  public String toString() {
    if (_owner == null) {
      return _name + " at " + _location + " unowned";
    }
    return _name + " at " + _location + " owned by:" + _owner;
  }

  public Location getLocation() {
    return _location;
  }

  public void setOwner(Player p) {

    if (_owner != null) {
      _owner.loseCity(this);
    }
    _owner = p;
    _produces = null;
    _productionStart = -1;
    _owner.captureCity(this);
  }

  public String getName() {
    return _name;
  }

  public Player getOwner() {
    return _owner;
  }

  public void produce(Type t) {
    _produces = t;
    _productionStart = _round;
  }

  public Type getProduction() {
    return _produces;
  }

  public boolean productionCompleted() {
    return _productionCompletedThisTurn;
  }

  public boolean isCoastal() {
    return _board.isCoast(_location);
  }

  public int getContinent() {
    return _board.getContinentId(_location);
  }

  /**
   * @return Returns the airPath.
   */
  public City getAirPath() {

    return _airPath;
  }

  /**
   * @param airPath
   *          The airPath to set.
   */
  public void setAirPath(City airPath) {

    _airPath = airPath;
  }

  /**
   * @return Returns the groundPath.
   */
  public City getGroundPath() {

    return _groundPath;
  }

  /**
   * @param groundPath
   *          The groundPath to set.
   */
  public void setGroundPath(City groundPath) {

    _groundPath = groundPath;
  }

  /**
   * @return Returns the seaPath.
   */
  public City getSeaPath() {

    return _seaPath;
  }

  /**
   * @param seaPath
   *          The seaPath to set.
   */
  public void setSeaPath(City seaPath) {

    _seaPath = seaPath;
  }

  public List<Unit> getUnits() {
    return _game.unitsAtLocation(_location);
  }
  

  public void bombCity() {
    long d = _round - _productionStart;
    if (d >= 0) {
      d = d / 4;
    }
    _productionStart += d;
  }

  public void startTurnPass(TurnState state) {
    if (state == TurnState.START) {
      int cost = _produces.getCost();
      _round++;
      _productionCompletedThisTurn = false;
      if (_productionStart != -1 && _productionStart + cost == _round) {
        Log.info(this, "Creating unit:" + _produces);
        Unit u = _game.createUnit(_produces, _owner, _location);
        Log.debug(u, "Created");
        _owner.addUnit(u);
        _productionStart = _round;
        _productionCompletedThisTurn = true;
      }
    }
  }
  
  public boolean isNear(City target, int dist) {
    
    return (target.getLocation().isNear(_location, dist));
    
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_location == null) ? 0 : _location.hashCode());
    result = prime * result + ((_name == null) ? 0 : _name.hashCode());
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
    City other = (City) obj;
    if (_location == null) {
      if (other._location != null)
        return false;
    } else if (!_location.equals(other._location))
      return false;
    if (_name == null) {
      if (other._name != null)
        return false;
    } else if (!_name.equals(other._name))
      return false;
    return true;
  }

    
}

/**
 * STATIC void citini() { int loc,i,j,k;
 * 
 * for (i = CITMAX; i--;) { memset(&city[i],0,sizeof(City)); city[i].loc =
 * city[i].own = 0; city[i].phs = -1; // no phase } for (i = 0, loc = MAPSIZE;
 * loc--;) if (typ[map[loc]] == X) city[i++].loc = loc; printf("%d cities\n",i);
 * assert(i <= CITMAX);
 * 
 * // shuffle cities around
 * 
 * for (i = CITMAX / 2; i--;) { j = random(CITMAX); k = random(CITMAX); loc =
 * city[j].loc; city[j].loc = city[k].loc; city[k].loc = loc; // swap city locs
 * } }
 */
