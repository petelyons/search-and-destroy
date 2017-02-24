package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.TurnState;
import com.developingstorm.games.sad.util.Log;

/**
 * 
 */
public class HeadHome extends Move {

  public OrderResponse executeInternal(TurnState turnState) {
    Player p = _unit.getOwner();

    City c = p.getClosestHome(_unit);
    if (c != null) {
      Log.debug(c, "chosen as landing point for " + _unit);
      Location loc = c.getLocation();
      if (loc.equals(_loc)) {
        return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
      }
      
      _loc = loc;
      _lastPath = null;
      
      return super.executeInternal(turnState);
    }
    return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
  }

}
