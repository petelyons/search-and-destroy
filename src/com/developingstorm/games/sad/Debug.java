package com.developingstorm.games.sad;

import java.util.List;

/**

 * 
 */
public class Debug {

  static boolean s_debugExplore = false;
  static List s_locationList;

  public static void setDebugExplore(boolean b) {
    s_debugExplore = b;
  }

  public static boolean getDebugExplore() {
    return s_debugExplore;
  }

  public static void setDebugLocations(List list) {
    s_locationList = list;
  }

  public static List getDebugLocations() {
    return s_locationList;
  }

}
