package com.developingstorm.games.sad.orders;

import java.util.ArrayList;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.TurnState;
import com.developingstorm.games.sad.util.Log;

/**
 * 
 */
public class Explore extends Move {

  public OrderResponse executeInternal(TurnState turnState) {
    Player owner = _unit.getOwner();

    Order headHome = alternateOrder(OrderType.HEAD_HOME, null, null);

    ArrayList<Location> frontierLocations = owner.getFrontier(_unit);
    ArrayList<Location> blockedLocations = new ArrayList<>();

    ResponseCode resp;
    do {
      if (_unit.mustLand() &&  !_unit.hasLanded()) {
        Log.debug(_unit, "requires landing!");
        _lastPath = null;
        OrderResponse response = headHome.execute(turnState);
        if (response.getCode() == ResponseCode.ORDER_COMPLETE) {
          return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }
      }
      if (frontierLocations.isEmpty()) {
        Log.debug(_unit, "no reachable frontier to explore!");
        if (_unit.getTravel() == Travel.AIR) {
          if (!_unit.hasLanded()) {
            return headHome.execute(turnState);
          }
          else {
            City currentCity = _game.cityAtLocation(_unit.getLocation());
            City hop = _unit.getOwner().findHopCity(currentCity, _unit.getMaxTravel());
            if (hop != null) {
              Order altMove = alternateOrder(OrderType.MOVE, hop.getLocation(), null);
              return altMove.execute(turnState);
            }
          }
        }
        resp = ResponseCode.CANCEL_ORDER;
        break;
      }
      Location ul = _unit.getLocation();
      Location loc = ul.closest(frontierLocations);

      if (loc == null) {
        Log.debug(_unit, "no close frontier to explore!");
        resp = ResponseCode.CANCEL_ORDER;
        break;
      }
      Location dest;
      if (ul.distance(loc) > 1) {
        Path path = _unit.getPath(loc);
        if (path != null && !path.isEmpty()) {
          dest = path.next(_unit.getLocation());
        } else {
          frontierLocations.remove(loc);
          blockedLocations.add(loc);
          Log.debug(_unit, "Blocked from exploring choosen location! " + loc);
          resp = ResponseCode.BLOCKED;
          continue;
        }
      } else {
        dest = loc;
      }

      resp = _game.resolveMove(_unit, dest);
      if (resp == ResponseCode.YIELD_PASS && blockedLocations.contains(dest)) {
        return new OrderResponse(resp, this, null);
      }
      else if (resp == ResponseCode.YIELD_PASS) {
        blockedLocations.add(dest);
      }
      
      if (resp == ResponseCode.CANCEL_ORDER  && blockedLocations.contains(dest)) {
        return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
      } else if (resp == ResponseCode.CANCEL_ORDER) {
        frontierLocations.remove(dest);
        blockedLocations.add(dest);
      }
 
    } while (_unit.movesLeft() > 0);

    if (resp == ResponseCode.ORDER_AND_TURN_COMPLETE || resp == ResponseCode.STEP_COMPLETE) {
      resp = ResponseCode.TURN_COMPLETE;
    }
    
    return new OrderResponse(resp, this, null);
  }
}
