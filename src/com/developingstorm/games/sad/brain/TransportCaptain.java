package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.types.Transport;

public class TransportCaptain extends UnitCaptain<Transport> {
  
  public TransportCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }
 
  @Override
  public Order plan(Transport u) {
    if (u.hasCargo() && atUnloadPoint(u)) {
      return unload(u);
    } else if (u.hasCargo()) {
      return goToUnloadingPoint(u);
    } else if (atLoadingPoint(u)) {
      return sentry(u);
    } 
    Order order = goToLoadingPoint(u);
    if (order == null) {
      order = explore(u);
    }
    return order;
  }

 



}
