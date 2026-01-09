package com.developingstorm.games.sad.turn;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.PathFinder;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

public class UnitTurnState {
  
  private Game game;
  
  private OrderState orderState;
  private final Unit unit;
  private int attemptCounter;
  private PathFinder pathFinder;

  
  public UnitTurnState(Game g, Unit u) {
    game = g;
    unit = u;
    attemptCounter = 0;
    updateOrderState();
  }
  
  
  private boolean executeOrder(Unit u, Order alt) {
    boolean blocked = false;
    OrderResponse response = u.execOrder(alt);
    if (response == null) {
      throw new SaDException("NO RESPONSE FROM UNIT!");
    }
    ResponseCode code = response.getCode();
    Log.debug(u, "Response from order: " + code);
    if (code == ResponseCode.CANCEL_ORDER) {
      Log.info(u, "Cancelling order");
      u.clearOrders();
    } else if (code == ResponseCode.STEP_COMPLETE) {
      u.clearOrders();
    } else if (code == ResponseCode.ORDER_AND_TURN_COMPLETE) {
      u.turn().clearOrderAndCompleteTurn();
    } else if (code == ResponseCode.ORDER_COMPLETE) {
      u.clearOrders();
    } else if (code == ResponseCode.YIELD_PASS) {
      Log.debug(u, "Yielding this turn pass");
    } else if (code == ResponseCode.TURN_COMPLETE) {
      u.turn().completeTurn();
    } else if (code == ResponseCode.DIED) {
      u.turn().completeTurn();
    } else if (code == ResponseCode.BLOCKED) {
      Log.warn("Unit blocked");
      blocked = true;
    } else {
      Log.debug(this, "****Unhandled response code:" + code);
    }
    u.getOwner().adjustVisibility(u);
    return blocked;
  }

  
  public boolean attemptTurn() {
    Unit u = unit;
    Order alternate = null;
    this.attemptCounter++;
    
    if (this.attemptCounter > 2 && u.getOwner().isRobot()) {
      Log.warn("**** RANDOM MOVE ****");
      alternate = u.newRandomMoveOrder();
    }
    
    if (this.attemptCounter > 5 &&  u.getOwner().isRobot()) {
      //u.getOrder();
      Log.warn(u, "***ATTEMPT COUNTER EXCEEEDED****");
      u.turn().completeTurn();
    }
    if (u.isDead()) {
      Log.error(u, "Unit is dead.  Cannot be played!");
      throw new SaDException("DEAD UNIT!" );
    }
    Log.debug(this, "playing unit: " + u + " with order " + u.getOrder());

    if (!u.turn().isDone()) {
      if (executeOrder(u, alternate) && u.getOwner().isRobot()) {
        Log.warn("**** RANDOM MOVE2 ****");
        executeOrder(u, u.newRandomMoveOrder());
      }

    }
    return true;
  }

  
  public void beginTurn() {


    attemptCounter = 0;
    pathFinder = null;
    this.unit.life().resetForTurn();

    this.unit.repairAndRefuel();

    
    Order order = this.unit.getOrder();
    if (order != null && order.getType() == OrderType.SKIPTURN) {
      this.unit.clearOrders();
    }
    
    this.unit.autoLoad();
    
    updateOrderState();
    
  }

  public void completeTurn() {
    
    this.unit.life().burnMoves();
    if (this.unit.life().hasDied()) {
      this.game.killUnit(this.unit);
    }
    orderState = OrderState.DONE;
  }
  
  
  public void clearOrderAndCompleteTurn() {
    this.unit.clearOrders();
    completeTurn();
  }

 
  public String toString() {
    if (orderState == null) {
      return "UNKNOWN";
    }
    return this.orderState.toString();
  }
  
  
  public Path getPath(Location dest) {
    if (this.pathFinder != null && this.pathFinder.getDest().equals(dest)) {
      ;
    } else {
      pathFinder = new PathFinder(this.unit, dest);
    }
    return this.pathFinder.getPath();
  }
  
  public void addObstruction(Location dest) {
    if (this.pathFinder != null) {
      this.pathFinder.addObstruction(dest);
    }
  }
  
  private void updateOrderState() {
    if (this.unit.hasOrders()) {
      orderState = OrderState.READY;
    } else {
      orderState = OrderState.AWAITING_ORDERS;
    }
  }
  
  public boolean isDone() {
    return orderState == OrderState.DONE;
  }

  public boolean isReady() {
    return orderState == OrderState.READY;
  }

  public void setReady() {
    orderState = OrderState.READY;
  }

  public boolean isKnownObstruction(Location dest) {
    if (this.pathFinder != null) {
      return this.pathFinder.isKnownObstruction(dest);
    }
    return false;
  }
}
