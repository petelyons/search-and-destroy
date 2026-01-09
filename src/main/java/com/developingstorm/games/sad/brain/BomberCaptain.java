package com.developingstorm.games.sad.brain;

import java.util.Set;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.types.Bomber;
import com.developingstorm.util.CollectionUtil;

public class BomberCaptain extends UnitCaptain<Bomber> {
  
  
  static final Type[] PrimaryTargetTypes = new Type[] {Type.TRANSPORT, Type.ARMOR, Type.INFANTRY};
  static final Set<Type> PrimaryTargets = CollectionUtil.create(PrimaryTargetTypes);
  
  static final Type[] SecondaryTargetTypes = new Type[] {Type.DESTROYER, Type.CRUISER, Type.BATTLESHIP, Type.CARRIER};
  static final Set<Type> SecondaryTargets = CollectionUtil.create(SecondaryTargetTypes);
  
  public BomberCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }



  @Override
  public Order plan(Bomber u) {
    Order order = planAttack(u, PrimaryTargets);
    if (order == null) {
      order = planAttack(u, SecondaryTargets);
    }
    if (order != null) {
      return order;
    }
    
    return explore(u);
  }

}
