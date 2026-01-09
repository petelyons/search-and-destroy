package com.developingstorm.games.sad.edicts;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.EdictType;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.util.json.JsonObj;

public class SendSeaUnits extends SendUnits {
  

  public SendSeaUnits(Player p, City c, City dest) {
    super(p, c, EdictType.SEND_SEA_UNITS, Travel.SEA, dest);
  }
  
  public SendSeaUnits(Player p, EdictType t, JsonObj json) {
    super(p, t, Travel.SEA, json);
    
  }
}
