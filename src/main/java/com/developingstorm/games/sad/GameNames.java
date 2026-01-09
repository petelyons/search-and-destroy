package com.developingstorm.games.sad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.developingstorm.util.RandomUtil;
import com.developingstorm.util.ResourceUtil;

/**

 * 
 */
public class GameNames {

  static final String PATH = "names.lst";

  private static GameNames s_names = new GameNames();

  private List<String> unused;

  private GameNames() {

    InputStream in = null;
    HashSet<String> names = new HashSet<String>(2000);
    try {
      in = ResourceUtil.openResourceStream(getClass().getClassLoader(), PATH);
      InputStreamReader isr = new InputStreamReader(in);
      BufferedReader br = new BufferedReader(isr);
      String name;
      while ((name = br.readLine()) != null) {
        names.add(name);
      }
    } catch (IOException io) {
      throw new SaDException("Could not load names");
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (Exception e) {
        }
      }
    }

    unused = new ArrayList<String>(names);

  }

  private String alloc() {
    int i = RandomUtil.getInt(this.unused.size());
    String s = (String) this.unused.remove(i);
    return s;
  }

  private void free(String s) {
    this.unused.add(s);
  }

  public static String getName() {
    return s_names.alloc();
  }

  public static void releaseName(String s) {
    s_names.free(s);

  }

}
