package com.developingstorm.games.sad;

import com.developingstorm.games.sad.edicts.AirPatrol;
import com.developingstorm.games.sad.edicts.AutoSentry;
import com.developingstorm.games.sad.edicts.SendAirUnits;
import com.developingstorm.games.sad.edicts.SendLandUnits;
import com.developingstorm.games.sad.edicts.SendSeaUnits;

public class EdictFactory {
  private final Player player;
  
  
  EdictFactory(Player p) {
    player = p;
  }
  
  
  public SendAirUnits sendAirUnits(City from, City to) {
    return new SendAirUnits(this.player, from, to);
  }
  
  public SendLandUnits sendLandUnits(City from, City to) {
    return new SendLandUnits(this.player, from, to);
  }
  
  public SendSeaUnits sendSeaUnits(City from, City to) {
    return new SendSeaUnits(this.player, from, to);
  }
  
  public AirPatrol sendSeaUnits(City from) {
    return new AirPatrol(this.player, from);
  }


  public AirPatrol airPatrol(City from) {
    return new AirPatrol(this.player, from);
  }


  public AutoSentry autoSentry(City from) {
    return new AutoSentry(this.player, from);
  }
  
}
