package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

/**

 * 
 */
public class DirectionalMove extends Order {
  private Direction dir;
  public DirectionalMove(Game g, Unit u, OrderType type, Direction dir) {
    super(g, u, type);
    this.dir = dir;
  }
  public OrderResponse executeInternal() {
    Log.info(this.unit, "Moving " + this.dir.toString());
    ResponseCode resp;
    Location ul = this.unit.getLocation();
    Location dest = ul.relative(this.dir);
    resp = this.game.resolveMove(this.unit, dest);
    if (resp == ResponseCode.TURN_COMPLETE) {
      resp = ResponseCode.ORDER_AND_TURN_COMPLETE;
    }
    return new OrderResponse(resp, this, null);

  }
}
