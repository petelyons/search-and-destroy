package com.developingstorm.games.sad;

import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

public class Travel extends Enum {
  private static final EnumClass _class = new EnumClass("Travel");

  public static final Travel LAND = new Travel("land");
  public static final Travel SEA = new Travel("sea");
  public static final Travel AIR = new Travel("air");

  private Travel(String desc) {
    super(_class, desc);
  }
}
