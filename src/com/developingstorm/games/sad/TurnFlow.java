package com.developingstorm.games.sad;

public class TurnFlow {
  private OrderState _orderState;
  private int _yields;
  private Unit _unit;
  private TurnState _turnState;
  
  public String toString() {
    if (_orderState == null) {
      return "UNKNOWN";
    }
    return _orderState.toString();
  }
  
  
  public void startPass(TurnState state, Unit u) {
    _turnState = state;
    _unit = u;
    if (state == TurnState.START) {
      updateOrderState();
      _yields = 0;
    }
    else if (state == TurnState.LOOP ) {
      if (_orderState == OrderState.DONE) {
        return;
      }
      updateOrderState();
    }
    else if (state == TurnState.END) {
      if (_orderState == OrderState.DONE) {
        return;
      }
      updateOrderState();     
    }
  }

  
  private void updateOrderState() {
    if (_unit.hasOrders()) {
      _orderState = OrderState.READY;
    } else {
      _orderState = OrderState.AWAITING_ORDERS;
    }
  }
  
  
  public void completeTurn() {
    _orderState = OrderState.DONE;
  }

  public void yieldTurn() {
    if (_orderState == OrderState.DONE) {
      throw new SaDException("A DONE unit cannot yield!");
    }
    
    if (_turnState == TurnState.END) {
      throw new SaDException("A unit may not yield during END state");
    }
    _orderState = OrderState.YIELDING;
    _yields++;
  }

  public boolean isDone() {
    return _orderState == OrderState.DONE;
  }
  
  public boolean awaitingOrders() {
    return _orderState == OrderState.AWAITING_ORDERS;
  }
  
    
  public boolean isReady() {
    return _orderState == OrderState.READY;
  }

  
  public boolean isYielding() {
    return _orderState == OrderState.YIELDING;
  }

  
  public void setReady() {
    _orderState = OrderState.READY;
  }


  public void setWaitingOrders() {
    _orderState = OrderState.AWAITING_ORDERS;
    
  }

}
