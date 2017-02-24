package com.developingstorm.games.sad.orders;

import java.util.List;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.TurnState;
import com.developingstorm.games.sad.Unit;

/**

 * 
 */
public class Sentry extends Order {

  public OrderResponse executeInternal(TurnState turnState) {
    if (_unit.canCarry()) {
      return executeTransport(turnState);
    }
    else {
      return executeNonTransport(turnState);
    }
  }
  
  
  private OrderResponse executeTransport(TurnState turnState) {
    
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
  
  
  private OrderResponse executeNonTransport(TurnState turnState) {
    return new OrderResponse(ResponseCode.YIELD_PASS, this, null);
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