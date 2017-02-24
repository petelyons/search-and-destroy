package com.developingstorm.games.sad;

/**
 * Class information
 */
public class OrderResponse {
  ResponseCode _code;
  Object _context;
  Order _orig;

  public OrderResponse(ResponseCode code, Order orig, Object ctx) {
    _code = code;
    _orig = orig;
    _context = ctx;
  }

  public ResponseCode getCode() {
    return _code;
  }

}
