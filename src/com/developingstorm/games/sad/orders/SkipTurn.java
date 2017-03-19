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
public class SkipTurn extends Order {
  public SkipTurn(Game g, Unit u) {
    super(g, u, OrderType.SKIPTURN);
  }
  public OrderResponse executeInternal() {
    return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
  }
}
