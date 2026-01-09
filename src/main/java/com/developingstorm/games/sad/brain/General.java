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

  private final ArmorCaptain armor;
  private final BattleshipCaptain battleship;
  private final BomberCaptain bomber;
  private final CargoCaptain cargo;
  private final CruiserCaptain cruiser;
  private final DestroyerCaptain destroyer;
  private final InfantryCaptain infantry;
  private final SubmarineCaptain submarine;
  private final TransportCaptain transport;
  private final FighterCaptain fighter;
  private final CarrierCaptain carrier;
  
  
  private int defendVsExplore = 50;
  
  private Battleplan plan;
  
  public General(Battleplan plan) {
    this.plan = plan;
    
    armor = new ArmorCaptain(this, plan);
    battleship = new BattleshipCaptain(this, plan);
    bomber = new BomberCaptain(this, plan);
    cargo = new CargoCaptain(this, plan);
    cruiser = new CruiserCaptain(this, plan);
    destroyer = new DestroyerCaptain(this, plan);
    fighter = new FighterCaptain(this, plan);
    infantry = new InfantryCaptain(this, plan);
    submarine = new SubmarineCaptain(this, plan);
    transport = new TransportCaptain(this, plan);
    carrier = new CarrierCaptain(this, plan);
  }
  
  private Order plan(Fighter u) {
    return this.fighter.plan(u);
  }

  private Order plan(Submarine u) {
    return this.submarine.plan(u);
  }

  private Order plan(Transport u) {
    return this.transport.plan(u);
  }
  private Order plan(Armor u) {
    return this.armor.plan(u);
  }
  
  private Order plan(Infantry u) {
    return this.infantry.plan(u);
  }

  private Order plan(Destroyer u) {
    return this.destroyer.plan(u);
  }
  
  private Order plan(Cruiser u) {
    return this.cruiser.plan(u);
  }
  
  private Order plan(Cargo u) {
    return this.cargo.plan(u);
  }

  private Order plan(Bomber u) {
    return this.bomber.plan(u);
  }
  
  private Order plan(Battleship u) {
    return this.battleship.plan(u);
  }
  

  private Order plan(Carrier u) {
    return this.carrier.plan(u);
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
    return (RandomUtil.getInt(100) <= this.defendVsExplore);
  }
  
  


  Set<Location> getUnloadingZone() {
    Set<Location> def = this.plan.getDefenseUnloadingPoints();
    Set<Location> exp = this.plan.getExpandUnloadingPoints();
    
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
