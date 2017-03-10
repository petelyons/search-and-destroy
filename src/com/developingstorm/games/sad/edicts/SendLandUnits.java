package com.developingstorm.games.sad.edicts;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.EdictType;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;

public class SendLandUnits extends SendUnits {
  

  public SendLandUnits(Player p, City c, City dest) {
    super(p, c, EdictType.SEND_LAND_UNITS, Travel.LAND, dest);
  }
}
