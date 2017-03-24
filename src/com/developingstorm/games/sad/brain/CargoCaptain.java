package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.types.Cargo;

public class CargoCaptain extends UnitCaptain<Cargo> {
  
  public CargoCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }

  @Override
  public Order plan(Cargo u) {
    if (u.hasCargo() && atUnloadPoint(u)) {
      return unload(u);
    } else if (u.hasCargo()) {
      return goToUnloadingPoint(u);
    } else {
      return u.newHeadHomeOrder();
    } 

  }

}
