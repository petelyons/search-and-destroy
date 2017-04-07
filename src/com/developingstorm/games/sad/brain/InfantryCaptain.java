package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.types.Infantry;

public class InfantryCaptain extends UnitCaptain<Infantry> {
  
  public InfantryCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }

  @Override
  public Order plan(Infantry u) {
    return occupyLandStrategy(u);
  }
}
