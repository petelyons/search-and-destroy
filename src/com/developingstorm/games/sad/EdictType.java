package com.developingstorm.games.sad;

import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

/**
 * 
 */
public class EdictType extends Enum {
  private static final EnumClass _class = new EnumClass("EdictType");

  public static final EdictType NONE = new EdictType("None");
  public static final EdictType SEND_LAND_UNITS = new EdictType("SendLandUnits");
  public static final EdictType SEND_SEA_UNITS = new EdictType("SendSeaUnits");
  public static final EdictType SEND_AIR_UNITS = new EdictType("SendAirUnits");
  public static final EdictType AUTO_SENTRY = new EdictType("AutomaticSentry");
  public static final EdictType AIR_PATROL = new EdictType("AirPatrol");
  


  private EdictType(String name) {
    super(_class, name);
  }

}
