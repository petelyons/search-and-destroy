package com.developingstorm.games.sad.orders;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.Unit;

/**

 * 
 */
public class Disband extends Order {
  
  protected Disband(Game g, Unit u) {
    super(g, u, OrderType.DISBAND);
  }

  public OrderResponse executeInternal() {
    this.game.killUnit(this.unit);
    return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
  }
}
