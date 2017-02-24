package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.MapState;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.TurnState;
import com.developingstorm.games.sad.util.Log;

/**

 * 
 */
public class Move extends Order {

  protected Path _lastPath = null;

  public OrderResponse executeInternal(TurnState turnState) {
    ResponseCode resp = ResponseCode.CANCEL_ORDER;
   
    if (_loc == null) {
      throw new SaDException("No Move Order");
    }
   
    Location dest = _loc;
    
    if (_unit.getLocation().equals(dest)) {
      return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
    }

    if (_unit.getLocation().distance(_loc) > 1) {
      Path path;
      if (_lastPath == null) {
        _lastPath = _unit.getPath(_loc);
        if (_lastPath == null || _lastPath.isEmpty()) {
          Log.error("No path available!!!");
          
          if (MapState.isBlocked(_loc)) {
            Log.error("The destination is blocked. Invalid move!");
          }
          else if (MapState.isBlocked(_unit.getLocation())) {
            Log.error("The starting location is blocked. Invalid move!");
          }
          resp = ResponseCode.CANCEL_ORDER;
          return new OrderResponse(resp, this, null);
        }
      }
     
      while (_unit.movesLeft() > 0){
        dest = _lastPath.next(_unit.getLocation());
        if (dest == null) {
          return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }
        Log.info(this, " Attempting move from " + _unit.getLocation() + " to " + dest + " along path " + _lastPath + " to " + _loc);
        resp = _game.resolveMove(_unit, dest);
        if (resp == ResponseCode.STEP_COMPLETE) {
          continue;
        } else if (resp == ResponseCode.TURN_COMPLETE) {
          return new OrderResponse(resp, this, null);
        } else {
          Log.debug("Converting response to CANCEL_ORDER:" + resp);
          resp = ResponseCode.CANCEL_ORDER;
          return new OrderResponse(resp, this, null);
        }
      } 
      
      if (dest == _loc) {
        resp = ResponseCode.ORDER_AND_TURN_COMPLETE;
      }
      else {
        resp = ResponseCode.TURN_COMPLETE;
      }
      return new OrderResponse(resp, this, null);
    } 
    else {
      

      Log.info(_unit, "Attempting move from " + _unit.getLocation() + " to " + dest);
      resp = _game.resolveMove(_unit, dest);
      if (resp != ResponseCode.STEP_COMPLETE) {
        Log.info(_unit, "Bad move:" + resp);
      }
      if (_unit.movesLeft() > 0) {
        resp = ResponseCode.ORDER_COMPLETE;
      } else {
        resp = ResponseCode.ORDER_AND_TURN_COMPLETE;
      }
     
      return new OrderResponse(resp, this, null);
    }

    
  }

//  public boolean complete() {
//    return _unit.getLocation().equals(_loc);
//  }

}
