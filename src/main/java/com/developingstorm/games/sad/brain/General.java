package com.developingstorm.games.sad.brain;

import java.util.Set;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.types.Armor;
import com.developingstorm.games.sad.types.Battleship;
import com.developingstorm.games.sad.types.Bomber;
import com.developingstorm.games.sad.types.Cargo;
import com.developingstorm.games.sad.types.Carrier;
import com.developingstorm.games.sad.types.Cruiser;
import com.developingstorm.games.sad.types.Destroyer;
import com.developingstorm.games.sad.types.Fighter;
import com.developingstorm.games.sad.types.Infantry;
import com.developingstorm.games.sad.types.Submarine;
import com.developingstorm.games.sad.types.Transport;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.RandomUtil;

public class General {

  private final ArmorCaptain _armor;
  private final BattleshipCaptain _battleship;
  private final BomberCaptain _bomber;
  private final CargoCaptain _cargo;
  private final CruiserCaptain _cruiser;
  private final DestroyerCaptain _destroyer;
  private final InfantryCaptain _infantry;
  private final SubmarineCaptain _submarine;
  private final TransportCaptain _transport;
  private final FighterCaptain _fighter;
  private final CarrierCaptain _carrier;
  
  
  private int _defendVsExplore = 50;
  
  private Battleplan _plan;
  
  public General(Battleplan plan) {
    _plan = plan;
    
    _armor = new ArmorCaptain(this, plan);
    _battleship = new BattleshipCaptain(this, plan);
    _bomber = new BomberCaptain(this, plan);
    _cargo = new CargoCaptain(this, plan);
    _cruiser = new CruiserCaptain(this, plan);
    _destroyer = new DestroyerCaptain(this, plan);
    _fighter = new FighterCaptain(this, plan);
    _infantry = new InfantryCaptain(this, plan);
    _submarine = new SubmarineCaptain(this, plan);
    _transport = new TransportCaptain(this, plan);
    _carrier = new CarrierCaptain(this, plan);
  }
  
  private Order plan(Fighter u) {
    return _fighter.plan(u);
  }

  private Order plan(Submarine u) {
    return _submarine.plan(u);
  }

  private Order plan(Transport u) {
    return _transport.plan(u);
  }
  private Order plan(Armor u) {
    return _armor.plan(u);
  }
  
  private Order plan(Infantry u) {
    return _infantry.plan(u);
  }

  private Order plan(Destroyer u) {
    return _destroyer.plan(u);
  }
  
  private Order plan(Cruiser u) {
    return _cruiser.plan(u);
  }
  
  private Order plan(Cargo u) {
    return _cargo.plan(u);
  }

  private Order plan(Bomber u) {
    return _bomber.plan(u);
  }
  
  private Order plan(Battleship u) {
    return _battleship.plan(u);
  }
  

  private Order plan(Carrier u) {
    return _carrier.plan(u);
  }

  public Order getOrders(Unit u) {
    
    
    Order order = null;
    switch(u.getType().getId()) {
    case Type.ARMOR_ID:
      order = plan((Armor) u);
      break;
      
    case Type.INFANTRY_ID:
      order = plan((Infantry) u);
      break;
      
    case Type.FIGHTER_ID:
      order = plan((Fighter) u);
      break;
      
    case Type.BOMBER_ID:
      order = plan((Bomber) u);
      break;
      
    case Type.CARGO_ID:
      order = plan((Cargo) u);
      break;
      
    case Type.DESTROYER_ID:
      order = plan((Destroyer) u);
      break;
      
    case Type.TRANSPORT_ID:
      order = plan((Transport) u);
      break;
      
    case Type.SUBMARINE_ID:
      order = plan((Submarine) u);
      break;
      
    case Type.CRUISER_ID:
      order = plan((Cruiser) u);
      break;
      
    case Type.CARRIER_ID:
      order = plan((Carrier) u);
      break;
      
    case Type.BATTLESHIP_ID:
      order = plan((Battleship) u);
      break;
    }
    
    if (order == null) {
      Log.warn("COULD NOT FIND VALID ORDER!");
      return u.newSkipTurn();
    }
    
    return order; 
  }


  boolean shouldDefend() {
    return (RandomUtil.getInt(100) <= _defendVsExplore);
  }
  
  


  Set<Location> getUnloadingZone() {
    Set<Location> def = _plan.getDefenseUnloadingPoints();
    Set<Location> exp = _plan.getExpandUnloadingPoints();
    
    if (def.isEmpty() && exp.isEmpty()) {
      return def;
    } else if (def.isEmpty() && !exp.isEmpty()) {
      return exp;
    } else if (!def.isEmpty() && exp.isEmpty()) {
      return def;
    } else {
      if (shouldDefend()) {
        return def;
      } else {
        return exp;
      }
    }
  }
}
