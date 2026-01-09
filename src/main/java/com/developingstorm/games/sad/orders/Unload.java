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
public class Unload extends Order {
  
  public Unload(Game g, Unit u) {
    super(g, u, OrderType.UNLOAD);
  }

  public OrderResponse executeInternal() {
    this.unit.unload();
    return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
  }

}
