package com.developingstorm.util;

import java.util.List;
import java.util.Random;

/**

 * 
 */
public class RandomUtil {

  private static Random s_rand;

  static {
    s_rand = new Random(System.currentTimeMillis());
  }

  private RandomUtil() {
  }

  /**
   * Reseed the shared generator so a run can be repeated.
   */
  public static void setSeed(long seed) {
    s_rand = new Random(seed);
  }

  public static int getInt(int max) {
    return s_rand.nextInt(max);
  }

  public static boolean nextBoolean() {
    return s_rand.nextBoolean();
  }
  
  
  public static <T> T randomValue(List<T> vals) {
    return vals.get(getInt(vals.size()));
  }

}
