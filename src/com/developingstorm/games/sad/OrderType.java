package com.developingstorm.games.sad;

import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

/**
 * Class information
 */
public class OrderType extends Enum {
  private static final EnumClass _class = new EnumClass("UnitFunction");

  public static final OrderType NONE = new OrderType("None");
 /*
  public static final OrderType USER = new OrderType("User");

  public static final OrderType KAMIKAZE_DIRECTION = new OrderType(
      "Kamikaze Direction");
  public static final OrderType KAMIKAZE_TARGET = new OrderType(
      "Kamikaze Target");
  public static final OrderType GO_DIRECTION = new OrderType("Go Direction");
  public static final OrderType ATTACK_TARGET = new OrderType("Attack Target");
  public static final OrderType MOVE_TO_CARRIER = new OrderType(
      "Move to Carrier");
  public static final OrderType LOITER = new OrderType("Loiter");
  public static final OrderType MOVE_TO_STATION = new OrderType(
      "Move to Station");
  public static final OrderType ESCORT = new OrderType("Escort");
  public static final OrderType FOLLOW_SHORE = new OrderType("Follow shore");
  */
  public static final OrderType EXPLORE = new OrderType("Explore");
  public static final OrderType BOARD_TRANSPORT = new OrderType(
      "Board Transport");
  public static final OrderType SENTRY = new OrderType("Sentry");
  public static final OrderType MOVE = new OrderType("Move");
  public static final OrderType HEAD_HOME = new OrderType("Head Home");
  public static final OrderType SKIPTURN = new OrderType("Skip Turn");
  public static final OrderType UNLOAD = new OrderType("UnLoad");
  public static final OrderType MOVE_NORTH = new OrderType("North");
  public static final OrderType MOVE_SOUTH = new OrderType("South");
  public static final OrderType MOVE_EAST = new OrderType("East");
  public static final OrderType MOVE_WEST = new OrderType("West");
  public static final OrderType MOVE_NORTH_EAST = new OrderType("NorthEast");
  public static final OrderType MOVE_NORTH_WEST = new OrderType("NorthWest");
  public static final OrderType MOVE_SOUTH_EAST = new OrderType("SouthEast");
  public static final OrderType MOVE_SOUTH_WEST = new OrderType("SouthWest");
  public static final OrderType YIELD = new OrderType("Yield");
  public static final OrderType DISBAND = new OrderType("Disband");

  private OrderType(String name) {
    super(_class, name);
  }

}
