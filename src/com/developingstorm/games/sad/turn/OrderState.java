package com.developingstorm.games.sad.turn;

import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

public class OrderState extends Enum {
  private static final EnumClass _class = new EnumClass("OrderState");
  
  public static final OrderState AWAITING_ORDERS = new OrderState("awaiting-orders");
  public static final OrderState READY = new OrderState("ready");
  public static final OrderState YIELDING = new OrderState("yielding");
  public static final OrderState SLEEPING = new OrderState("sleeping");
  public static final OrderState DONE = new OrderState("done");

  private OrderState(String desc) {
    super(_class, desc);
  }
}
