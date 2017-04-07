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
  
  private Game _game;
  
  private OrderState _orderState;
  private final Unit _unit;
  private int _attemptCounter;
  private PathFinder _pathFinder;

  
  public UnitTurnState(Game g, Unit u) {
    _game = g;
    _unit = u;
    _attemptCounter = 0;
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
    Unit u = _unit;
    Order alternate = null;
    _attemptCounter++;
    
    if (_attemptCounter > 2) {
      alternate = u.newRandomMoveOrder();
    }
    
    if (_attemptCounter > 5) {
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
      if (executeOrder(u, alternate)) {
        executeOrder(u, u.newRandomMoveOrder());
      }

    }
    return true;
  }

  
  public void beginTurn() {
    _attemptCounter = 0;
    _pathFinder = null;
    _unit.life().resetForTurn();
  
    Order order = _unit.getOrder();
    if (order != null && order.getType() == OrderType.SKIPTURN) {
      _unit.clearOrders();
    }
    
    _unit.autoLoad();
    
    updateOrderState();
    
  }

  public void completeTurn() {
    
    _unit.life().burnMoves();
    if (_unit.life().hasDied()) {
      _game.killUnit(_unit);
    }
    _orderState = OrderState.DONE;
  }
  
  
  public void clearOrderAndCompleteTurn() {
    _unit.clearOrders();
    completeTurn();
  }

 
  public String toString() {
    if (_orderState == null) {
      return "UNKNOWN";
    }
    return _orderState.toString();
  }
  
  
  public Path getPath(Location dest) {
    if (_pathFinder != null && _pathFinder.getDest().equals(dest)) {
      ;
    } else {
      _pathFinder = new PathFinder(_unit, dest);
    }
    return _pathFinder.getPath();
  }
  
  public void addObstruction(Location dest) {
    if (_pathFinder != null) {
      _pathFinder.addObstruction(dest);
    }
  }
  
  private void updateOrderState() {
    if (_unit.hasOrders()) {
      _orderState = OrderState.READY;
    } else {
      _orderState = OrderState.AWAITING_ORDERS;
    }
  }
  
  public boolean isDone() {
    return _orderState == OrderState.DONE;
  }

  public boolean isReady() {
    return _orderState == OrderState.READY;
  }

  public void setReady() {
    _orderState = OrderState.READY;
  }

  public boolean isKnownObstruction(Location dest) {
    if (_pathFinder != null) {
      return _pathFinder.isKnownObstruction(dest);
    }
    return false;
  }
}
