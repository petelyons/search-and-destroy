package com.developingstorm.games.sad;

import com.developingstorm.games.sad.edicts.AirPatrol;
import com.developingstorm.games.sad.edicts.AutoSentry;
import com.developingstorm.games.sad.edicts.SendAirUnits;
import com.developingstorm.games.sad.edicts.SendLandUnits;
import com.developingstorm.games.sad.edicts.SendSeaUnits;

public class EdictFactory {
  private final Player _player;
  
  
  EdictFactory(Player p) {
    _player = p;
  }
  
  
  public SendAirUnits sendAirUnits(City from, City to) {
    return new SendAirUnits(_player, from, to);
  }
  
  public SendLandUnits sendLandUnits(City from, City to) {
    return new SendLandUnits(_player, from, to);
  }
  
  public SendSeaUnits sendSeaUnits(City from, City to) {
    return new SendSeaUnits(_player, from, to);
  }
  
  public AirPatrol sendSeaUnits(City from) {
    return new AirPatrol(_player, from);
  }


  public AirPatrol airPatrol(City from) {
    return new AirPatrol(_player, from);
  }


  public AutoSentry autoSentry(City from) {
    return new AutoSentry(_player, from);
  }
  
}
