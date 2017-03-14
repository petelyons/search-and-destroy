package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.swing.event.ListSelectionEvent;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;

public class Unit {

  static long s_unitCounter = 1;

  private volatile Type _type; // This type of unit
  private volatile Player _owner;
  private volatile Location _loc;
  private volatile Board _board;
  private volatile Travel _travel;
  private volatile Game _game;
  private volatile ArrayList<Unit> _carries;
  private volatile Unit _onboard;
  private volatile int _hits;
  private volatile int _dist;
  private volatile Order _order;
  private volatile int _moved;
  private volatile int _totalMoved;
  private volatile String _name;
  private volatile long _id;
  private volatile TurnFlow _turnFlow;

  private boolean _isDead = false;  //Used to prevent endless looping during kill processing u->game->u->game->u->game...

  public Unit(Type t, Player owner, Location loc, Game game) {
    _type = t;
    _loc = loc;
    _name = null;
    _hits = _type.getHits();
    _dist = _type.getDist();
    _travel = _type.getTravel();
    _owner = owner;
    _game = game;
    _board = _game.getBoard();
    _order = null;
    _game.initUnit(this);
    _turnFlow = new TurnFlow();
    synchronized (this) {
      s_unitCounter++;
      _id = s_unitCounter;
    }
  }

  private void changeLoc(Location loc) {
    _game.changeUnitLoc(this, loc);
    _loc = loc;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append('[');
    sb.append(toUIString());
    sb.append(": TS=");
    sb.append(turn());
    sb.append(": Own=");
    sb.append(_owner);
    sb.append(": Loc=(");
    sb.append(_loc);
    sb.append(": Mv=");
    sb.append(_moved);
    sb.append(": Dist=");
    sb.append(_dist);
    if (_onboard != null) {
      sb.append(": On=");
      sb.append(_onboard.toUIString());
    }
    if (_carries != null) {
      sb.append(": Carries=");
      sb.append(carriesDesc());
    }
    sb.append(']');
    return sb.toString();
  }
  
  
  public String carriesDesc() {
    if (!canCarry()) {
      return "N/A";
    }
    if (_carries == null || _carries.isEmpty()) {
      return "None";
    }
    StringBuffer sb = new StringBuffer();
    for (Unit u : _carries) {
      sb.append(u.getType().getAbr());
    }
    return sb.toString();
  }

  public String toUIString() {
    StringBuffer sb = new StringBuffer();
    
    sb.append(_type);
    sb.append(' ');
    sb.append(_id);
    sb.append(':');
    sb.append((_order != null) ? _order.getType().toString() : "No Orders");
    return sb.toString();
  }

  public boolean hasOrders() {
    return (_order != null);
  }

  public String typeDesc() {
    if (_name != null) {
      return (_type + " " + _name);
    } else {
      return _type.toString() + " " + _id;
    }
  }

  public String healthDesc() {
    return "" + _hits + " of " + _type.getHits();
  }

  public String moveDesc() {
    String s = "" + _moved + " of " + _dist;
    int maxTravel = getMaxTravel();
    if (maxTravel > 0) {
      s += " [" + _totalMoved + " of " + maxTravel + "]";
    }
    return s;
  }
  
  public int turnAroundDist() {
    if (_travel == Travel.AIR) {
      return (getMaxTravel() / 2);
    }
    return 0;
  }

  public boolean mustLand() {
    if (_travel == Travel.AIR) {
      if (_totalMoved >= turnAroundDist()) {
        return true;
      }
    }
    return false;
  }

  public String locationDesc() {
    return _loc.toString();
  }

  public Location getLocation() {
    return _loc;
  }
  
 
  public void move(Location loc) {
    
    if (_loc.distance(loc) == 0) {
      Log.error(this, "Attempting to spend move with no move");
      throw new SaDException("Invalid non-move");
    }

    if (_loc.distance(loc) > 1) {
      Log.error(this, "Invalid Move to location " + loc);
      throw new SaDException("Invalid Move:");
    }
    
    if (movesLeft() == 0) {
      return;
    }

   // Location oldLoc = _loc;
    _moved++;
    _totalMoved++;
    changeLoc(loc);
    _owner.adjustVisibility(this);
    _game.trackUnit(this);

    
    // If this unit was on-board another unit, and it moved then it's no longer carried
    if (_onboard != null) {
      _onboard.removeCarried(this);
    }

    // Move the carried units 
    if (_carries != null) {
      for(Unit u2 : _carries) {
        u2.changeLoc(_loc);
      }
    }

    // Land the aircraft
    if (_travel == Travel.AIR) {
      City city = _board.getCity(loc);
      if (city != null && city.getOwner() == getOwner()) {
        _totalMoved = 0;
        clearOrderAndCompleteTurn();
      }
    }

    // If the unit ran out of fuel, it dies
    int maxTravel = getMaxTravel();
    if (maxTravel > 0 && _totalMoved >= maxTravel) {
      _game.killUnit(this);
    }

  }
  
  public TurnFlow turn() {
    return _turnFlow;
  }

  public Travel getTravel() {
    return _travel;
  }

  public int getMaxTravel() {
    return _type.getMaxTravel();
  }

  public Type getType() {
    return _type;
  }


  public boolean inSentryMode() {
    return (_order != null && _order.getType() == OrderType.SENTRY);
  }
  
  public Player getOwner() {
    return _owner;
  }

  public int getAttack() {
    return _type.getAttack();
  }

  public boolean canTravel(Location loc) {
    Travel t = _type.getTravel();
    if (t == Travel.AIR)
      return true;
    if (t == Travel.SEA && _board.isWater(loc))
      return true;
    if (t == Travel.LAND && _board.isLand(loc))
      return true;
    return false;
  }

  public Path getPath(Location loc) {
    Path p = _owner.getTravelPath(_travel, _loc, loc);
    if (p != null && p.isEmpty()) {
      return null;
    }
    return p;
  }

  public boolean isDead() {
    return _hits <= 0 || _isDead;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_game == null) ? 0 : _game.hashCode());
    result = prime * result + (int) (_id ^ (_id >>> 32));
    result = prime * result + ((_type == null) ? 0 : _type.hashCode());
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
    Unit other = (Unit) obj;
    if (_game == null) {
      if (other._game != null)
        return false;
    } else if (!_game.equals(other._game))
      return false;
    if (_id != other._id)
      return false;
    if (_type == null) {
      if (other._type != null)
        return false;
    } else if (!_type.equals(other._type))
      return false;
    return true;
  }

  public boolean hit(int attackVal) {
    if (attackVal == 0) {
      return isDead();
    }
    if (attackVal < 0) {
      throw new SaDException("BAD ATTACK VALUE");
    }
    _hits -= attackVal;
    return isDead();
  }

  // Should only be called by (Game).killUnit
  void kill() {
    if (_isDead) {
      return;
    }
    killCarried();
    _isDead  = true;
    _hits = 0;
    if (_onboard != null) {
      _onboard.removeCarried(this);
    }
  }

  public boolean isCarried() {
    return _onboard != null;
  }

  public int carriedWeight() {
    if (_carries == null) {
      return 0;
    }

    int weight = 0;
    for(Unit u : _carries) {
      Type t = u.getType();
      weight += t.getWeight();
    }
    return weight;
  }

  public int carriableWeight() {
    return _type.getCarryCount();
  }

  public Type[] getCarryTypes() {
    return _type.getCarryTypes();
  }

  public int getWeight() {
    return _type.getWeight();
  }

  public boolean canCarry(Unit u) {
    return (_type.canCarry(u.getType()) && carriedWeight() + u.getWeight() <= carriableWeight());
  }

  public boolean canCarry(Type t) {
    return (_type.canCarry(t));
  }

  public void addCarried(Unit u) {
    if (_carries == null) {
      _carries = new ArrayList<Unit>();
    }
    if (u._onboard != null) {
      throw new SaDException("Unit already loaded" + u);
    }
    if (!u.getLocation().equals(_loc)) {
      Log.error(this, "Attempting to carry unit not at location");
      u.move(_loc);
    }
    
    
    u._onboard = this;
    u.orderSentry();
    _carries.add(u);
  }

  public void unload() {
    if (_carries == null) {
      return;
    }

    for(Unit u : _carries) {
      u.clearOrders();
      u.turn().setWaitingOrders();
      _owner.pushPendingOrders(u);
    }
  }

  public void removeCarried(Unit u) {
    if (_carries != null) {
      _carries.remove(u);
    }
    u._onboard = null;
  }

  public boolean isVisible(Vision v) {
    if (v == Vision.NONE)
      return false;
    if (v == Vision.SURFACE) {
      if (_type == Type.SUBMARINE)
        return false;
      return true;
    }
    if (v == Vision.WATER) {
      if (_travel == Travel.AIR || _travel == Travel.LAND) {
        return false;
      }
      return true;
    }
    return true;
  }

  public ArrayList<Location> getAreaOfInfluence() {

    ArrayList<Location> list = new ArrayList<Location>();
    for (Location loc : list) {
      if (canTravel(loc)) {
        list.add(loc);
      }
    }
    return list;
  }
  
  
  void killCarried() {
   
    if (_carries != null) {
      @SuppressWarnings("unchecked")
      List<Unit> copy =  (List<Unit>) _carries.clone();
      _game.killUnits(copy);
    }
  }
  
  
  private void autoLoad() {
    if (_game.isCity(_loc)) {
      if (carriableWeight() > 0  && carriedWeight() < carriableWeight()) {
        List<Unit> ul = _game.unitsAtLocation(_loc);
        for (Unit u : ul) {
          if (u.isCarried() == false) {
            if (canCarry(u)) {
              addCarried(u);
            }
          }
        }
      }
    }
  }
  
  public void startTurnPass(TurnState state) {
    // CALLED FOR EVERY PASS OF THE TURN.  IF YOU WANT TO ONLY DO SOMETHING AT THE START OF THE TURN
    ///  YOU HAVE TO CHECK THE STATE!
    
    if (state == TurnState.START) {
      _moved = 0;
      if (_order != null && _order.getType() == OrderType.SKIPTURN) {
        clearOrders();
      }
    }
    
    autoLoad();
    _turnFlow.startPass(state, this);
  }

  public void completeTurn() {
    
    // Planes run out of fuel even when they don't move
    int maxTravel = getMaxTravel();
    if (maxTravel > 0 && _moved < _type.getDist()) {
      _totalMoved += (_type.getDist() - _moved);
    }
    if (maxTravel > 0 && _totalMoved >= maxTravel) {
      _game.killUnit(this);
    }
    _turnFlow.completeTurn();
  }

  public void clearOrders() {
    if (_order != null && _order.getType().equals(OrderType.SKIPTURN)) {
       Log.stack("Clearing skip turn");
    }
    _order = null;
    if (_turnFlow.isReady()) {
      _turnFlow.setWaitingOrders();
    }
    
  }
  public void clearOrderAndCompleteTurn() {
    _order = null;
    _turnFlow.completeTurn();
  }


  public void orderSentry() {
    assignOrder(Order.factory(_game, this, OrderType.SENTRY, null, null));
  }

  public void orderMove(Location loc) {
    assignOrder(Order.factory(_game, this, OrderType.MOVE, loc, null));
  }

  public void assignOrder(Order order) {
    _order = order;
    if (_turnFlow.awaitingOrders()) {
      _turnFlow.setReady();
    }
  }

  public Location getClosestLocation(List<?> locations) {

    Collections.sort(locations, new DistanceComparator(_loc));

    Iterator<?> itr = locations.iterator();
    Location shortest = null;
    int shortestLen = 99999;

    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      Path p = getPath(loc);
      int pLen = p.length();
      int tLen = _loc.distance(loc);
      if (pLen == tLen) {
        return loc;
      } else if (pLen < shortestLen) {
        shortestLen = pLen;
        shortest = loc;
      }
    }
    return shortest;
  }


  private static class DistanceComparator implements Comparator<Object> {

    private Location _loc;

    public DistanceComparator(Location loc) {
      _loc = loc;
    }

    /**
         *
         */
    public int compare(Object arg0, Object arg1) {

      Location loc0 = (Location) arg0;
      Location loc1 = (Location) arg1;

      int dist0 = _loc.distance(loc0);
      int dist1 = _loc.distance(loc1);
      if (dist0 == dist1) {
        return 0;
      }
      if (dist0 < dist1) {
        return -1;
      }
      return 1;
    }

  }


  public Order getOrder() {
    return _order;
  }

  public boolean canCarry() {
    return _type.getCarryCount() > 0;
  }

  public int movesLeft() {
    return _dist - _moved;
  }

  public boolean hasLanded() {
    if (_travel == Travel.AIR) {
      City city = _board.getCity(_loc);
      return (city != null && city.getOwner() == getOwner());
    }
    throw new SaDException("hasLanded called for non air unit");
  }

  public boolean canAttackCity() {
    return (_travel == Travel.LAND || _type == Type.BOMBER);
  }

}
