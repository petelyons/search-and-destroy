package com.developingstorm.games.sad.brain;

import java.util.Set;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.types.Cruiser;
import com.developingstorm.util.CollectionUtil;

public class CruiserCaptain  extends UnitCaptain<Cruiser> {
  
  
  static final Type[] PrimaryTargetTypes = new Type[] {Type.TRANSPORT, Type.DESTROYER, Type.SUBMARINE};
  static final Set<Type> PrimaryTargets = CollectionUtil.create(PrimaryTargetTypes);
    
  
  static final Type[] SecondaryTargetTypes = new Type[] {Type.CRUISER, Type.BOMBER, Type.FIGHTER};
  static final Set<Type> SecondaryTargets = CollectionUtil.create(SecondaryTargetTypes);
  
  
  
  public CruiserCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }
  

  @Override
  public Order plan(Cruiser u) {
    return attackShipStrategy(u, PrimaryTargets, SecondaryTargets);
  }

}
