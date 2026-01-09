package com.developingstorm.games.sad.edicts;

import java.util.List;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Edict;
import com.developingstorm.games.sad.EdictType;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.games.sad.util.json.JsonObj;

public class SendUnits extends Edict {
  
  Travel _travel;
  City _dest;

  protected SendUnits(Player p, City c, EdictType t, Travel travel, City dest) {
    super(p, c, t);
    _travel = travel;
    _dest = dest;
  }
  
  protected SendUnits(Player p, EdictType t, Travel travel, JsonObj json) {
    super(p, t, json);
    _travel = travel;
    _dest = p.getGame().getCity(json.getString("dest"));
  }

  public City destination() {
   return _dest;
  }
  
  @Override
  public void execute(Game game) {
    
    List<Unit> units = unitsMatchingTravel(_travel);
    if (!units.isEmpty()) {
      for (Unit u : units) {
        Log.debug(u, "Applying send edict. " + _travel);
        u.orderMove(_dest.getLocation());
      }
    }
  }
  
  public JsonObj toJson() {
    JsonObj obj = super.toJson();
    obj.put("dest", _dest.toJsonLink());
    return obj;
  }

}
