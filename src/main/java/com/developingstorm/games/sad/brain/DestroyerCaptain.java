package com.developingstorm.games.sad.brain;

import java.util.Set;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.types.Destroyer;
import com.developingstorm.util.CollectionUtil;

public class DestroyerCaptain extends UnitCaptain<Destroyer> {
  
  
  
  static final Type[] PrimaryTargetTypes = new Type[] {Type.TRANSPORT, Type.SUBMARINE};
  static final Set<Type> PrimaryTargets = CollectionUtil.create(PrimaryTargetTypes);
    
  
  static final Type[] SecondaryTargetTypes = new Type[] {Type.DESTROYER, Type.BOMBER, Type.FIGHTER};
  static final Set<Type> SecondaryTargets = CollectionUtil.create(SecondaryTargetTypes);
  
  public DestroyerCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }

  @Override
  public Order plan(Destroyer u) {
    return attackShipStrategy(u, PrimaryTargets, SecondaryTargets);
  }


}
