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
   // _battleplan = new Battleplan(_owner.getGame(), _owner);
  }
  
  @Override
  public void startNewTurn() {
   
    _battleplan = new Battleplan(_owner.getGame(), _owner);
    
    Log.info(_battleplan.toString());
    
    _general = new General(_battleplan);
    
    
    
    _owner.forEachUnit((u) -> {u.assignOrder(_general.getOrders(u));});
    
    for (City c : _owner.getCities()) {
      if (c.productionCompleted()) {
        Type t = _battleplan.productionChoice(c);
        Log.debug(_owner, "Resetting production of " + c + " to: " + t);
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
    if (_battleplan == null) {
      return Type.INFANTRY;
    }
    return _battleplan.productionChoice(c);
  }
  
}
