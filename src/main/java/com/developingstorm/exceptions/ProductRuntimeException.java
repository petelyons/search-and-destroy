package com.developingstorm.exceptions;

import com.developingstorm.global.GlobalConstants;
import com.developingstorm.global.Globals;

/**
 * 
 *
 */
public class ProductRuntimeException extends RuntimeException {

  private static final String LINE_SEPERATOR = System
      .getProperty("line.separator");

  public ProductRuntimeException(String desc) {
    super(desc);
  }

  private void append(StringBuffer sb, String global) {
    String s = Globals.getString(global);
    if (s != null) {
      sb.append(s);
      sb.append(LINE_SEPERATOR);
    }
  }

  public String toString() {

    StringBuffer sb = new StringBuffer(256);

    append(sb, GlobalConstants.NAME);
    append(sb, GlobalConstants.COPYRIGHT);
    append(sb, GlobalConstants.VERSION);
    append(sb, GlobalConstants.EMAIL);
    sb.append(super.toString());

    return sb.toString();
  }

}
