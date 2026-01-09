package com.developingstorm.games.sad.brain;

import java.util.HashSet;
import java.util.Set;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.types.Fighter;
import com.developingstorm.util.CollectionUtil;

public class FighterCaptain  extends UnitCaptain<Fighter> {
  
  
  static final Type[] PrimaryTargetTypes = new Type[] {Type.TRANSPORT, Type.ARMOR, Type.INFANTRY, Type.BOMBER, Type.CARGO, Type.FIGHTER};
  static final Set<Type> PrimaryTargets = CollectionUtil.create(PrimaryTargetTypes);
  
  static final Type[] SecondaryTargetTypes = new Type[] {Type.DESTROYER, Type.CRUISER, Type.BATTLESHIP, Type.CARRIER};
  static final Set<Type> SecondaryTargets = CollectionUtil.create(SecondaryTargetTypes);
  
  public FighterCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }

  @Override
  public Order plan(Fighter u) {
    
    Order order = planAttack(u, PrimaryTargets);
    if (order == null) {
      order = planAttack(u, SecondaryTargets);
    }
    if (order != null) {
      return order;
    }
    
    if (!u.isCarried()) {
      order = gotoCarrier(u);
      if (order != null) {
        return order;
      }
    }
    
    return explore(u);
  }

  private Order gotoCarrier(Unit u) {
    Set<Unit> carriers = new HashSet<Unit>();
    u.getOwner().forEachUnit((Unit u2)->{if (u2.isCarrier()) {carriers.add(u2);}});
    
    
    Location loc = u.getLocation();
    for(Unit u2 : carriers) {
      int dist = loc.distance(u2.getLocation());
      if (dist <= u.life().remainingFuel()) {
        return u.newMoveOrder(u2.getLocation());
      }
    }
    return null;
  }

}
