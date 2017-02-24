package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.TurnState;
import com.developingstorm.games.sad.util.Log;

/**

 * 
 */
public class MoveWest extends Order {
  public OrderResponse executeInternal(TurnState turnState) {
    Log.debug(_unit, "Moving West");
    ResponseCode resp;
    Location ul = _unit.getLocation();
    Location dest = ul.relative(Direction.WEST);
    resp = _game.resolveMove(_unit, dest);
    return new OrderResponse(resp, this, null);
  }
}
