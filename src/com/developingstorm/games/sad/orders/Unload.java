package com.developingstorm.games.sad.orders;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.TurnState;

/**

 * 
 */
public class Unload extends Order {

  public OrderResponse executeInternal(TurnState turnState) {
    _unit.unload();
    return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
  }

}
