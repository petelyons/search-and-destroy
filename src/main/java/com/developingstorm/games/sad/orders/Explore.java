package com.developingstorm.games.sad.orders;

import java.util.ArrayList;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

/**
 * 
 */
public class Explore extends Order {
  
  public Explore(Game g, Unit u) {
    super(g, u, OrderType.EXPLORE);
  }

  public OrderResponse executeInternal() {
    Player owner = this.unit.getOwner();

    Order headHome = new HeadHome(this.game, this.unit);

    ArrayList<Location> frontierLocations = owner.getFrontier(this.unit);
    ArrayList<Location> blockedLocations = new ArrayList<>();

    ResponseCode resp;
    do {
      if (this.unit.life().mustLand() &&  !this.unit.hasLanded()) {
        Log.debug(this.unit, "requires landing!");
        OrderResponse response = headHome.execute();
        if (response.getCode() == ResponseCode.ORDER_COMPLETE) {
          return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }
        resp = response.getCode();
        continue;
      }
      if (frontierLocations.isEmpty()) {
        Log.debug(this.unit, "no reachable frontier to explore!");
        if (this.unit.getTravel() == Travel.AIR) {
          if (!this.unit.hasLanded()) {
            return headHome.execute();
          }
          else {
            City currentCity = this.game.cityAtLocation(this.unit.getLocation());
            City hop = this.unit.getOwner().findHopCity(currentCity, this.unit.getMaxTravel());
            if (hop != null) {
              Order altMove = new Move(this.game, this.unit, hop.getLocation());
              return altMove.execute();
            }
          }
        }
        resp = ResponseCode.CANCEL_ORDER;
        break;
      }
      Location ul = this.unit.getLocation();
      Location loc = ul.closest(frontierLocations);

      if (loc == null) {
        Log.debug(this.unit, "no close frontier to explore!");
        resp = ResponseCode.CANCEL_ORDER;
        break;
      }
      Location dest;
      if (ul.distance(loc) > 1) {
        Path path = this.unit.getPath(loc);
        if (path != null && !path.isEmpty()) {
          dest = path.next(this.unit.getLocation());
        } else {
          frontierLocations.remove(loc);
          blockedLocations.add(loc);
          Log.debug(this.unit, "Blocked from exploring choosen location! " + loc);
          resp = ResponseCode.BLOCKED;
          continue;
        }
      } else {
        dest = loc;
      }

      resp = this.game.resolveMove(this.unit, dest);
      if (resp == ResponseCode.DIED || resp == ResponseCode.TURN_COMPLETE) {
        return new OrderResponse(resp, this, null);
      }

      if (resp == ResponseCode.YIELD_PASS && blockedLocations.contains(dest)) {
        return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
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
      
 
    } while (this.unit.life().movesLeft() > 0  && !this.unit.isDead());

    if (resp == ResponseCode.ORDER_AND_TURN_COMPLETE || resp == ResponseCode.STEP_COMPLETE) {
      resp = ResponseCode.TURN_COMPLETE;
    }
    
    return new OrderResponse(resp, this, null);
  }
}
