package com.developingstorm.games.sad.edicts;

import java.util.List;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Edict;
import com.developingstorm.games.sad.EdictType;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;

public class SendUnits extends Edict {
  
  Travel _travel;
  City _dest;

  protected SendUnits(Player p, City c, EdictType t, Travel travel, City dest) {
    super(p, c, t);
    _travel = travel;
    _dest = dest;
  }

  public City destination() {
   return _dest;
  }
  
  @Override
  public void onTurnStart(Game game) {
    
    List<Unit> units = unitsMatchingTravel(_travel);
    if (!units.isEmpty()) {
      for (Unit u : units) {
        u.orderMove(_dest.getLocation());
      }
    }
  }

}
