package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Unit;

/**
 * 
 */
public class MoveWest extends DirectionalMove {
  public MoveWest(Game g, Unit u) {
    super(g, u, OrderType.MOVE_WEST, Direction.WEST);
  }
}
