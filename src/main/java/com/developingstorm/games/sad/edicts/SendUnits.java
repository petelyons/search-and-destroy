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
  
  Travel travel;
  City dest;

  protected SendUnits(Player p, City c, EdictType t, Travel travel, City dest) {
    super(p, c, t);
    this.travel = travel;
    this.dest = dest;
  }
  
  protected SendUnits(Player p, EdictType t, Travel travel, JsonObj json) {
    super(p, t, json);
    this.travel = travel;
    dest = p.getGame().getCity(json.getString("dest"));
  }

  public City destination() {
   return dest;
  }
  
  @Override
  public void execute(Game game) {
    
    List<Unit> units = unitsMatchingTravel(this.travel);
    if (!units.isEmpty()) {
      for (Unit u : units) {
        Log.debug(u, "Applying send edict. " + this.travel);
        u.orderMove(this.dest.getLocation());
      }
    }
  }
  
  public JsonObj toJson() {
    JsonObj obj = super.toJson();
    obj.put("dest", this.dest.toJsonLink());
    return obj;
  }

}
