package com.developingstorm.games.sad;

import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

public class TurnState extends Enum {
  private static final EnumClass _class = new EnumClass("TurnState");
  
  public static final TurnState START = new TurnState("start");
  public static final TurnState LOOP = new TurnState("loop");
  public static final TurnState END = new TurnState("end");

  private TurnState(String desc) {
    super(_class, desc);
  }
}
