package com.developingstorm.games.sad;

import com.developingstorm.exceptions.ProductRuntimeException;

/**

 * 
 */
public class SaDException extends ProductRuntimeException {

  public SaDException(String s) {
    super(s);
  }
  
  public SaDException(Unit u, String s) {
    super(u.toString() + ":" + s);
  }


}
