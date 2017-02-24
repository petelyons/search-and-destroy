package com.developingstorm.games.sad.orders;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.TurnState;

/**

 * 
 */
public class SkipTurn extends Order {
  public OrderResponse executeInternal(TurnState turnState) {
    return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
  }
}
