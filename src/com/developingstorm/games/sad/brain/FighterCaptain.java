package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.types.Fighter;

public class FighterCaptain  extends UnitCaptain<Fighter> {
  
  public FighterCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }

  @Override
  public Order plan(Fighter u) {
    return explore(u);
  }

}
