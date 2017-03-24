package com.developingstorm.games.sad.brain;

import java.util.Set;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.types.Battleship;
import com.developingstorm.util.CollectionUtil;

public class BattleshipCaptain extends UnitCaptain<Battleship> {
  
  
  static final Type[] PrimaryTargetTypes = new Type[] {Type.TRANSPORT, Type.CARRIER, Type.BATTLESHIP};
  static final Set<Type> PrimaryTargets = CollectionUtil.create(PrimaryTargetTypes);
    
  
  static final Type[] SecondaryTargetTypes = new Type[] {Type.DESTROYER, Type.CRUISER};
  static final Set<Type> SecondaryTargets = CollectionUtil.create(SecondaryTargetTypes);
  
  
  
  public BattleshipCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }

  @Override
  public Order plan(Battleship u) {
    return attackShipStrategy(u, PrimaryTargets, SecondaryTargets);
  }

}
