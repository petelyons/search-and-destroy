package com.developingstorm.games.sad;

import java.util.List;

import com.developingstorm.games.hexboard.Location;

/**

 * 
 */
public class Debug {

  static boolean s_debugExplore = false;
  static List<Location> s_locationList;

  public static void setDebugExplore(boolean b) {
    s_debugExplore = b;
  }

  public static boolean getDebugExplore() {
    return s_debugExplore;
  }

  public static void setDebugLocations(List<Location> list) {
    s_locationList = list;
  }

  public static List<Location> getDebugLocations() {
    return s_locationList;
  }

}
