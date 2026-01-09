package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.List;

/**
 * Class information
 */
abstract public class Order {

  protected Game game;
  protected Unit unit;
  protected Type unitType;
  protected OrderType orderType;
  private List<OrderResponse> results;

  protected Order(Game g, Unit u, OrderType type) {
    game = g;
    unit = u;
    orderType = type;
    unitType = this.unit.getType();
    results = new ArrayList<OrderResponse>();
  }
  
  public String toString() {
    return this.orderType.toString();
  }

  public Unit getAssignee() {
    return unit;
  }

  public OrderType getType() {
    return orderType;
  }
  
  public OrderResponse execute() {
    OrderResponse resp = executeInternal();
    this.results.add(resp);
    return resp;
  }
  
  abstract protected OrderResponse executeInternal();



}
