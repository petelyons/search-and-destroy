package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.types.Bomber;

public class BomberCaptain extends UnitCaptain<Bomber> {
  
  public BomberCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }



  @Override
  public Order plan(Bomber u) {
  
    return explore(u);
  }

}
