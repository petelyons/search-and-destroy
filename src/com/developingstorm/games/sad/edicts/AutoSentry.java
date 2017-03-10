package com.developingstorm.games.sad.edicts;

import java.util.List;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Edict;
import com.developingstorm.games.sad.EdictType;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;

public class AutoSentry extends Edict {
  

  public AutoSentry(Player p, City c) {
    super(p, c, EdictType.AUTO_SENTRY);
  }
  

  @Override
  public void onTurnStart(Game game) {
    
    List<Unit> units = _city.getUnits();
    if (!units.isEmpty()) {
      
      for (Unit u : units) {
        if (u.getTravel() == Travel.LAND || u.getTravel() == Travel.AIR) {
          u.orderSentry();
        }
      }
    }
  }

}
