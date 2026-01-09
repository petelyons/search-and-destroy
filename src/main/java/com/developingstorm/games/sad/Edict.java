package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.developingstorm.games.sad.util.json.JsonObj;

/**
 * Edicts are orders assigned to Cities instead of Units.
 */
public abstract class Edict {
  protected Player player;
  protected City city;
  protected EdictType edictType;
  private static HashMap<EdictType, Class<? extends Edict>> s_map = new HashMap<EdictType, Class<? extends Edict>>();
  
  protected Edict(Player p, City c, EdictType t) {
    player = p;
    city = c;
    edictType = t;
  }
  
  
  public Edict(Player p, EdictType t, JsonObj json) {
    player = p;
    edictType = t;
    city = p.getGame().getCity(json.getString("city"));
  }


  public JsonObj toJson() {
    JsonObj obj = new JsonObj();
    obj.put("type", this.edictType.getName());
    obj.put("city", this.city.toJsonLink());
    return obj;
  }
  
 
  protected List<Unit> unitsMatchingTravel(Travel t) {
    List<Unit> matching = new ArrayList<Unit>();
    List<Unit> units = this.city.getUnits();
    for (Unit u : units) {
      if (u.getTravel().equals(t)) {
        matching.add(u);
      }
    }
    return matching;
  }


  public abstract void execute(Game game);
  

}
