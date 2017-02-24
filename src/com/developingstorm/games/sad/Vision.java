package com.developingstorm.games.sad;

import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

public class Vision extends Enum {
  private static final EnumClass _class = new EnumClass("Visibility");

  public static final Vision NONE = new Vision("none");
  public static final Vision SURFACE = new Vision("surface");
  public static final Vision COMPLETE = new Vision("complete");
  public static final Vision WATER = new Vision("water");

  private Vision(String desc) {
    super(_class, desc);
  }
}
