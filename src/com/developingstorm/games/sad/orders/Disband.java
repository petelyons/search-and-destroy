package com.developingstorm.games.sad.orders;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.TurnState;

/**

 * 
 */
public class Disband extends Order {
  public OrderResponse executeInternal(TurnState turnState) {
    _game.killUnit(_unit);
    return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
  }
}
