package com.developingstorm.games.sad;

import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

/**
 * Class information
 */
public class ResponseCode extends Enum {
  private static final EnumClass _class = new EnumClass("OrderResponse");

  
  public static final ResponseCode STEP_COMPLETE = new ResponseCode("Step-Complete");
  
  public static final ResponseCode TURN_COMPLETE = new ResponseCode("Turn-Complete");
  public static final ResponseCode ORDER_AND_TURN_COMPLETE = new ResponseCode("Order-And-Turn-Complete");
  public static final ResponseCode YIELD_PASS = new ResponseCode("Yield-Pass");
  //public static final ResponseCode INVALID_ORDER = new ResponseCode("Invalid");
 // public static final ResponseCode NOWORK = new ResponseCode("No Work");
  public static final ResponseCode DIED = new ResponseCode("Died");
  public static final ResponseCode BLOCKED = new ResponseCode("Blocked");
  public static final ResponseCode CANCEL_ORDER = new ResponseCode("Cancel-Order");
//  public static final ResponseCode ENEMY_DETECTED = new ResponseCode(
 //     "Enemy Detected");

  public static final ResponseCode ORDER_COMPLETE = new ResponseCode("Order-Complete(Needs-New-Order)");

  private ResponseCode(String name) {
    super(_class, name);
  }

}
