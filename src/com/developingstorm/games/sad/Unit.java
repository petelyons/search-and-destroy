package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.orders.Explore;
import com.developingstorm.games.sad.orders.HeadHome;
import com.developingstorm.games.sad.orders.Move;
import com.developingstorm.games.sad.orders.MoveEast;
import com.developingstorm.games.sad.orders.MoveNorthEast;
import com.developingstorm.games.sad.orders.MoveNorthWest;
import com.developingstorm.games.sad.orders.MoveSouthEast;
import com.developingstorm.games.sad.orders.MoveSouthWest;
import com.developingstorm.games.sad.orders.MoveWest;
import com.developingstorm.games.sad.orders.Sentry;
import com.developingstorm.games.sad.orders.SkipTurn;
import com.developingstorm.games.sad.orders.Unload;
import com.developingstorm.games.sad.turn.UnitTurnState;
import com.developingstorm.games.sad.util.Log;


/**
 * Units are the moveable pieces of the game.  
 * Units have attributes based on their Type. Infantry, Battleships and Bombers are all examples of Types of units.
 * Different Types of units can travel in different terrains.  
 * Units have a 'life' which controls how much damage they can absorb, and how much they can move.
 * Some units can carry other units.
 * Units are always owned by one of the games players. 
 */
public abstract class Unit {

  static long s_unitCounter = 1;

  private volatile Type _type; // This type of unit
  private volatile Player _owner;
  private volatile Location _loc;
  private volatile Board _board;
  private volatile Travel _travel;
  private volatile Game _game;
  private volatile ArrayList<Unit> _carries;
  private volatile Unit _onboard;
  private volatile int _dist;
  private volatile Order _order;
  private volatile String _name;
  private volatile long _id;
  private volatile UnitTurnState _turn;
  private boolean _isDead = false;  //Used to prevent endless looping during kill processing u->game->u->game->u->game...
  private Life _life;
  
  
  
  
  public class Life {
    private volatile int _hits;
    private volatile int _fuel;
    private volatile int _moves;
    private volatile boolean _sleeping;
    
    
    public Life() {
      _hits = _type.getHits();
      _fuel = _type.getFuel();
      _moves = _type.getDist();
      _sleeping = false;
    }

    public void repair() {
      if (_hits < _type.getHits()) {
        _hits++;
      }
    }

    public void fuel() {
      _fuel = _type.getFuel();
    }
    
    public void resetForTurn() {
      _moves = _type.getDist();
    }

    public void move() {
      _moves--;
      
      if ( _type.getFuel() == 0) {
        _fuel--;
      }
    }
    
    public void wake() {
      _sleeping = false;
    }
        
    public void sleep() {
      _sleeping = true;
    }
    
    public boolean isSleeping() {
      return _sleeping;
    }
    
    public void burnMoves() {
      while (movesLeft() > 0) {
        move();
      }
    }

    public void burnMovesButNotFuel() {
      while (movesLeft() > 0) {
        _moves--;
      }
    }
    
    public boolean hasDied() {
      return _hits <= 0 || !hasFuel();
    }
   
    public boolean hasFuel() {
      if ( _type.getFuel() == 0) {
        return true;
      }
      return _fuel <= 0;
    }
 
    public boolean hasMoves() {
      return _sleeping == false && _moves > 0 && !hasDied();
    }
    
    public int turnAroundDist() {
      if (_travel == Travel.AIR) {
        return (getMaxTravel() / 2);
      }
      return 0;
    }

    public boolean mustLand() {
      if (_travel == Travel.AIR) {
        if (_fuel < turnAroundDist()) {
          return true;
        }
      }
      return false;
    }
    
    public String healthDesc() {
      return "" + _hits + " of " + _type.getHits();
    }  

    public String moveDesc() {
      String s = "" + _moves + " of " + _dist;
      int maxTravel = getMaxTravel();
      if (maxTravel > 0) {
        s += " [" + _fuel + " of " + maxTravel + "]";
      }
      return s;
    }
    
    public int movesLeft() {
      return _moves;
    }
    
    public boolean attack(int attackVal) {
      if (attackVal == 0) {
        return isDead();
      }
      if (attackVal < 0) {
        throw new SaDException("BAD ATTACK VALUE");
      }
      _hits -= attackVal;
      return isDead();
    }
    
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append('[');
      sb.append("HITS=");
      sb.append(_hits);
      sb.append("/");
      sb.append(_type.getHits());
      sb.append(": MOVE=");
      sb.append(_moves);
      sb.append("/");
      sb.append(_type.getDist());
      if (_type.getFuel() > 0) {
        sb.append(": FUEL=");
        sb.append(_fuel);
        sb.append("/");
        sb.append(_type.getFuel());
      }
      sb.append(']');
      return sb.toString();
    }

    public void kill() {
      _hits = 0;
    }
  }
  
  protected Unit(Type t, Player owner, Location loc, Game game) {
    _type = t;
    _loc = loc;
    _name = null;
    _dist = _type.getDist();
    _travel = _type.getTravel();
    _owner = owner;
    _game = game;
    _board = _game.getBoard();
    _order = null;
    _turn = new UnitTurnState(game, this);
    _life = new Life();
    synchronized (this) {
      s_unitCounter++;
      _id = s_unitCounter;
    }
    // A unit needs an ID before it can be placed!
    _game.placeUnitOnBoard(this);
  }
    
  private synchronized void changeLoc(Location loc) {
    Log.info(this, "change location to " + loc);
    _game.changeUnitLoc(this, loc);
    _loc = loc;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append('[');
    sb.append(toUIString());
    sb.append(": Own=");
    sb.append(_owner);
    sb.append(": Loc=(");
    sb.append(_loc);
    sb.append("): Life=");
    sb.append(_life);
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
    sb.append(": ");
    if (_life == null) {
      sb.append("INIT");
    }
    else if (_life.isSleeping()) {
      sb.append("SLEEP");
    } else {
      sb.append("AWAKE");
    }
    sb.append(": ");
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
    
    if (!_life.hasMoves()) {
      return;
    }

    _life.move();
    
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
        _turn.clearOrderAndCompleteTurn();
      }
    }

    // If the unit ran out of fuel, it dies
    if (!_life.hasFuel()) {
      _game.killUnit(this);
    }

  }
  
  public UnitTurnState turn() {
    return _turn;
  }

  public Travel getTravel() {
    return _travel;
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
    return _life.hasDied() || _isDead;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
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
    if (_id != other._id)
      return false;
    if (_type == null) {
      if (other._type != null)
        return false;
    } else if (!_type.equals(other._type))
      return false;
    return true;
  }



  // Should only be called by (Game).killUnit
  void kill() {
    if (_isDead) {
      return;
    }
    killCarried();
    _life.kill();
    _isDead  = true;
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
      u.activate();
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
  
  
  public void autoLoad() {
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
  
  
  public boolean hasCargo() {
    if (_carries == null) {
      return false;
    }
    return !_carries.isEmpty();
  }
  


  public void clearOrders() {
    if (_order != null && _order.getType().equals(OrderType.SKIPTURN)) {
       Log.stack("Clearing skip turn");
    }
    _order = null;
  }


  public void orderSentry() {
    assignOrder(newSentryOrder());
  }

  public void orderMove(Location loc) {
    assignOrder(newMoveOrder(loc));
  }

  public void assignOrder(Order order) {
    _order = order;
  }

  public Location getClosestLocation(Collection<Location> locationsCollection) {

    List<Location> locations = new ArrayList<>(locationsCollection);
    Collections.sort(locations, new DistanceComparator(_loc));

    Location shortest = null;
    int shortestLen = 99999;

    for (Location loc : locations) {
      if (_loc.equals(loc)) {
        return loc;
      }
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
  
  
  public Explore newExploreOrder() {
    return new Explore(_game, this);
  }
  
  public HeadHome newHeadHomeOrder() {
    return new HeadHome(_game, this);
  }
  
  public Move newMoveOrder(Location loc) {
    if (loc == null) {
      throw new SaDException("Cannot move to NULL location");
    }
    return new Move(_game, this, loc);
  }
  
  public Sentry newSentryOrder() {
    return new Sentry(_game, this);
  }
  
  public Unload newUnloadOrder() {
    return new Unload(_game, this);
  }
  
  public MoveEast newMoveEast() {
    return new MoveEast(_game, this);
  }
  public MoveWest newMoveWest() {
    return new MoveWest(_game, this);
  }

  public MoveNorthWest newMoveNorthWest() {
    return new MoveNorthWest(_game, this);
  }
  public MoveSouthWest newMoveSouthWest() {
    return new MoveSouthWest(_game, this);
  }
  
  public MoveNorthEast newMoveNorthEast() {
    return new MoveNorthEast(_game, this);
  }
  public MoveSouthEast newMoveSouthEast() {
    return new MoveSouthEast(_game, this);
  }
  
  public SkipTurn newSkipTurn() {
    return new SkipTurn(_game, this);
  }
  
  
  /**
   * Construct and order given an OrderType and an optional parameter.
   * 
   * WHEN OrderType.MOVE, p1 MUST BE Location 
   * 
   * Otherwise p1 is ignored 
   *  
   * @param order
   * @param p1
   * @return
   */
  public Order newOrder(OrderType order, Object p1) {
    if (order.equals(OrderType.EXPLORE)) {
      return newExploreOrder();
    } else if (order.equals(OrderType.HEAD_HOME)) {
      return newHeadHomeOrder();
    } else if (order.equals(OrderType.MOVE)) {
      return newMoveOrder((Location) p1);
    } else if (order.equals(OrderType.SENTRY)) {
      return newSentryOrder();
    } else if (order.equals(OrderType.UNLOAD)) {
      return newUnloadOrder();
    } else if (order.equals(OrderType.MOVE_WEST)) {
      return newMoveWest();
    } else if (order.equals(OrderType.MOVE_EAST)) {
      return newMoveEast();
    } else if (order.equals(OrderType.MOVE_NORTH_WEST)) {
      return newMoveNorthWest();
    } else if (order.equals(OrderType.MOVE_SOUTH_WEST)) {
      return newMoveSouthWest();
    } else if (order.equals(OrderType.MOVE_NORTH_EAST)) {
      return newMoveNorthEast();
    } else if (order.equals(OrderType.MOVE_SOUTH_EAST)) {
      return newMoveSouthEast();
    } else if (order.equals(OrderType.SKIPTURN)) {
        return newSkipTurn();
    } else  {
      throw new SaDException("Unknown order type:" + order.toString());
    }
  }

  
  public Life life() {
    return _life;
  }


  public int getMaxTravel() {
    return _type.getFuel();
  }

  
  public OrderResponse execOrder() {

    OrderResponse lastOrderResponse = null;
    if (isDead()) {
      throw new SaDException("Dead units should not be playing");
    }

    Log.debug(this, "Getting units orders");
    Order order = getOrder();
    if (order == null) {
      throw new SaDException("Attempting to play unit with no order!");
    }
    if (this != order.getAssignee()) {
      throw new SaDException("Order does not belong to unit running it!");
    }

    lastOrderResponse = order.execute();
    return lastOrderResponse;
  }

  public void activate() {
    clearOrders();
    _life.wake();
  }

  public boolean isInfantry() {
   return _type == Type.INFANTRY;
  }

  public boolean isArmour() {
    return _type == Type.ARMOR;
  }

  public boolean isBomber() {
    return _type == Type.BOMBER;
  }

  public boolean isTransport() {
    return _type == Type.TRANSPORT;
  }
  
  public boolean isCargo() {
    return _type == Type.CARGO;
  }
  
  public boolean isDestroyer() {
    return _type == Type.DESTROYER;
  }

  public boolean isFighter() {
    return _type == Type.FIGHTER;
  }

  public boolean isSubmarine() {
    return _type == Type.SUBMARINE;
  }
  
  public boolean isCruiser() {
    return _type == Type.CRUISER;
  }
  
  public boolean isBattleship() {
    return _type == Type.BATTLESHIP;
  }
}
