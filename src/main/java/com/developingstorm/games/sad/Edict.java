package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.developingstorm.games.sad.util.json.JsonObj;

/**
 * Edicts are orders assigned to Cities instead of Units.
 */
public abstract class Edict {
  protected Player _player;
  protected City _city;
  protected EdictType _edictType;
  private static HashMap<EdictType, Class<? extends Edict>> s_map = new HashMap<EdictType, Class<? extends Edict>>();
  
  protected Edict(Player p, City c, EdictType t) {
    _player = p;
    _city = c;
    _edictType = t;
  }
  
  
  public Edict(Player p, EdictType t, JsonObj json) {
    _player = p;
    _edictType = t;
    _city = p.getGame().getCity(json.getString("city"));
  }


  public JsonObj toJson() {
    JsonObj obj = new JsonObj();
    obj.put("type", _edictType.getName());
    obj.put("city", _city.toJsonLink());
    return obj;
  }
  
 
  protected List<Unit> unitsMatchingTravel(Travel t) {
    List<Unit> matching = new ArrayList<Unit>();
    List<Unit> units = _city.getUnits();
    for (Unit u : units) {
      if (u.getTravel().equals(t)) {
        matching.add(u);
      }
    }
    return matching;
  }


  public abstract void execute(Game game);
  

}
