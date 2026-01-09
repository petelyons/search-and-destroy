package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Unit;

/**
 * 
 */
public class MoveNorthWest extends DirectionalMove {
  public MoveNorthWest(Game g, Unit u) {
    super(g, u, OrderType.MOVE_NORTH_WEST, Direction.NORTH_WEST);
  }
}
