package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Unit;

/**
 * 
 */
public class MoveSouthEast  extends DirectionalMove {
  public MoveSouthEast(Game g, Unit u) {
    super(g, u, OrderType.MOVE_SOUTH_EAST, Direction.SOUTH_EAST);
  }
}
