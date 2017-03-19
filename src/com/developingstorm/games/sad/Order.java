package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.List;

/**
 * Class information
 */
abstract public class Order {

  protected Game _game;
  protected Unit _unit;
  protected Type _unitType;
  protected OrderType _orderType;
  private List<OrderResponse> _results;

  protected Order(Game g, Unit u, OrderType type) {
    _game = g;
    _unit = u;
    _orderType = type;
    _unitType = _unit.getType();
    _results = new ArrayList<OrderResponse>();
  }
  
  public String toString() {
    return _orderType.toString();
  }

  public Unit getAssignee() {
    return _unit;
  }

  public OrderType getType() {
    return _orderType;
  }
  
  public OrderResponse execute() {
    OrderResponse resp = executeInternal();
    _results.add(resp);
    return resp;
  }
  
  abstract protected OrderResponse executeInternal();



}
