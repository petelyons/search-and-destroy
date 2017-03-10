package com.developingstorm.games.sad.edicts;

import java.util.List;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Edict;
import com.developingstorm.games.sad.EdictType;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.util.RandomUtil;

public class AirPatrol extends Edict {
  
  Travel _travel;
  City _dest;

  public AirPatrol(Player p, City c) {
    super(p, c, EdictType.AIR_PATROL);
    _travel = Travel.AIR;
  }
  

  @Override
  public void onTurnStart(Game game) {
    
    List<Unit> units = unitsMatchingTravel(_travel);
    if (!units.isEmpty()) {
      
      for (Unit u : units) {
        List<Location> locs = _city.getLocation().getCircle(u.turnAroundDist());
        Location loc = RandomUtil.randomValue(locs);
        u.orderMove(loc);
      }
    }
  }

}
