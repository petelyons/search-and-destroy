package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Unit;

/**
 * 
 */
public class MoveSouthWest extends DirectionalMove {
  public MoveSouthWest(Game g, Unit u) {
    super(g, u, OrderType.MOVE_SOUTH_WEST, Direction.SOUTH_WEST);
  }
}
