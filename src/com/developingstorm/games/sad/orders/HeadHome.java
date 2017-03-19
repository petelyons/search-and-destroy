package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

/**
 * 
 */
public class HeadHome extends Move {

  public HeadHome(Game g, Unit u) {
    super(g, u, OrderType.HEAD_HOME, u.getLocation());
  }

  public OrderResponse executeInternal() {
    Player p = _unit.getOwner();

    City c = p.getClosestHome(_unit);
    if (c != null) {
      Log.debug(c, "chosen as landing point for " + _unit);
      Location loc = c.getLocation();
      if (loc.equals(_unit.getLocation())) {
        return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
      }
      
      _loc = loc;
      _lastPath = null;
      
      return super.executeInternal();
    }
    return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
  }

}
