package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.IBrain;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

public class RobotBrain implements IBrain {
  
  private final Robot _owner;
  private Battleplan _battleplan;
  private General _general;
 
  
  public RobotBrain(Robot owner) {
    _owner = owner;   
    _battleplan = new Battleplan(_owner.getGame(), _owner);
  }
  
  @Override
  public void startNewTurn() {
   
    _battleplan = new Battleplan(_owner.getGame(), _owner);
    _general = new General(_battleplan);
    _owner.forEachUnit((u) -> {_general.getOrders(u);});
    
    for (City c : _owner.getCities()) {
      if (c.productionCompleted()) {
        Type t = _battleplan.productionChoice(c);
        Log.debug(this, "Resetting production of " + c + " to: " + t);
        c.produce(t);
      }
    }
  }
  
  @Override
  public Order getOrders(Unit u) {
    return _general.getOrders(u);    
  }

  @Override
  public Type getProduction(City c) {
    return _battleplan.productionChoice(c);
  }
  
}
