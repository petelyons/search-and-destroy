package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.types.Carrier;

public class CarrierCaptain extends UnitCaptain<Carrier> {
  
  public CarrierCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }
 
  @Override
  public Order plan(Carrier u) {
    Order order = null;
    if (order == null) {
      order = patrolUnloadingZones(u);
    }
    
    if (order == null) {
      order = explore(u);
    }
    return order;
  }

}
