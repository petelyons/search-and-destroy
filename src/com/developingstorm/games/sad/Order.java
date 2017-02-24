package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.orders.Disband;
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

/**
 * Class information
 */
abstract public class Order {

  protected Game _game;
  protected Unit _unit;
  protected Type _unitType;
  protected OrderType _orderType;
  protected Location _loc;
  protected Unit _target;
  private static HashMap<OrderType, Class<? extends Order>> s_map = new HashMap<OrderType, Class<? extends Order>>();
  private List<OrderResponse> _results;

  protected void init(Game g, Unit u, OrderType type, Location loc, Unit target) {
    _game = g;
    _unit = u;
    _orderType = type;
    _unitType = _unit.getType();
    _loc = loc;
    _target = target;
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
  
  
  public Order alternateOrder(OrderType type, Location location, Unit target) {
    return factory(_game, _unit, type, location, target);
  }

  public OrderResponse execute(TurnState turnState) {
    OrderResponse resp = executeInternal(turnState);
    _results.add(resp);
    return resp;
  }
  
  abstract protected OrderResponse executeInternal(TurnState turnState);

  public static Order factory(Game g, Unit u, OrderType type) {
    return factory(g, u, type, null, null);
  }

  public static Order factory(Game g, Unit u, OrderType type,
      Location location, Unit target) {
    Class<? extends Order> clazz = (Class<? extends Order>) s_map.get(type);
    if (clazz == null) {
      throw new SaDException("Order type not implemented:" + type);
    }
    Order order = null;
    try {
      order = (Order) clazz.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
      throw new SaDException(e.toString());
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      throw new SaDException(e.toString());
    }

    order.init(g, u, type, location, target);
    return order;
  }

  static {
    s_map.put(OrderType.MOVE, Move.class);
    s_map.put(OrderType.MOVE_EAST, MoveEast.class);
    s_map.put(OrderType.MOVE_WEST, MoveWest.class);
    s_map.put(OrderType.MOVE_SOUTH_WEST, MoveSouthWest.class);
    s_map.put(OrderType.MOVE_SOUTH_EAST, MoveSouthEast.class);
    s_map.put(OrderType.MOVE_NORTH_WEST, MoveNorthWest.class);
    s_map.put(OrderType.MOVE_NORTH_EAST, MoveNorthEast.class);
    s_map.put(OrderType.EXPLORE, Explore.class);
    s_map.put(OrderType.SENTRY, Sentry.class);
    s_map.put(OrderType.UNLOAD, Unload.class);
    s_map.put(OrderType.DISBAND, Disband.class);
    s_map.put(OrderType.HEAD_HOME, HeadHome.class);
    s_map.put(OrderType.SKIPTURN, SkipTurn.class);
  }
}
