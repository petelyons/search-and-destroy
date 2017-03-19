package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Unit;

/**
 * 
 */
public class MoveNorthEast extends DirectionalMove {
  public MoveNorthEast(Game g, Unit u) {
    super(g, u, OrderType.MOVE_NORTH_EAST, Direction.NORTH_EAST);
  }
}
