package com.developingstorm.games.sad;

/**
 * Class information
 */
public class OrderResponse {
  ResponseCode code;
  Object context;
  Order orig;

  public OrderResponse(ResponseCode code, Order orig, Object ctx) {
    this.code = code;
    this.orig = orig;
    context = ctx;
  }

  public ResponseCode getCode() {
    return code;
  }

}
