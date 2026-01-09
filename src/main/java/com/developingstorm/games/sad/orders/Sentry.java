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
    if (this.unit.canCarry()) {
      return executeTransport();
    }
    else {
      return executeNonTransport();
    }
  }
  
  
  private OrderResponse executeTransport() {
    this.unit.life().sleep();
    if (this.unit.carriableWeight() > 0
        && this.unit.carriedWeight() < this.unit.carriableWeight()) {

      Location loc = this.unit.getLocation();
      List<Unit> ul = this.game.unitsBorderingLocation(loc);
      for (Unit u : ul) {
        if (u.isCarried() == false) {
          if (this.unit.canCarry(u)) {
            this.unit.addCarried(u);
          }
        }
      }
    }
    if (this.unit.carriedWeight() < this.unit.carriableWeight()) {
      return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
    } else {
      return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
    }
  }
  
  
  private OrderResponse executeNonTransport() {
    this.unit.life().sleep();
    return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
  }

//  public boolean complete() {
//    return !(this.unit.carriableWeight() == 0 || this.unit.carriedWeight() < this.unit
//        .carriableWeight());
//  }
//
//  public boolean isYielding() {
//    return (this.unit.carriableWeight() == 0 || this.unit.carriedWeight() < this.unit
//        .carriableWeight());
//
//  }

}