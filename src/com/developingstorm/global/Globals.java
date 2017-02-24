package com.developingstorm.global;

import java.util.Properties;

/**

 * 
 */
public class Globals extends Properties {

  public static final Globals INSTANCE = new Globals();

  private Globals() {

  }

  public static String getString(String name) {
    return INSTANCE.getProperty(name);
  }

  public static void setString(String name, String val) {
    INSTANCE.setProperty(name, val);
  }

}
