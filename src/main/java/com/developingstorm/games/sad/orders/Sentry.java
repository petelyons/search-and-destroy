package com.developingstorm.games.sad.orders;

import java.util.List;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.Unit;

/**

 * 
 */
public class Sentry extends Order {
  
  public Sentry(Game g, Unit u) {
    super(g, u, OrderType.SENTRY);
  }

  public OrderResponse executeInternal() {
    if (_unit.canCarry()) {
      return executeTransport();
    }
    else {
      return executeNonTransport();
    }
  }
  
  
  private OrderResponse executeTransport() {
    _unit.life().sleep();
    if (_unit.carriableWeight() > 0
        && _unit.carriedWeight() < _unit.carriableWeight()) {

      Location loc = _unit.getLocation();
      List<Unit> ul = _game.unitsBorderingLocation(loc);
      for (Unit u : ul) {
        if (u.isCarried() == false) {
          if (_unit.canCarry(u)) {
            _unit.addCarried(u);
          }
        }
      }
    }
    if (_unit.carriedWeight() < _unit.carriableWeight()) {
      return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
    } else {
      return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
    }
  }
  
  
  private OrderResponse executeNonTransport() {
    _unit.life().sleep();
    return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
  }

//  public boolean complete() {
//    return !(_unit.carriableWeight() == 0 || _unit.carriedWeight() < _unit
//        .carriableWeight());
//  }
//
//  public boolean isYielding() {
//    return (_unit.carriableWeight() == 0 || _unit.carriedWeight() < _unit
//        .carriableWeight());
//
//  }

}