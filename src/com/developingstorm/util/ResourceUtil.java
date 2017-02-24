package com.developingstorm.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;

/**
 * 
 *
 */
public class ResourceUtil {

  private static byte[] s_work = new byte[10000];

  public static InputStream openResourceStream(ClassLoader cl, String name)
      throws IOException {
    if (cl == null) {
      cl = ClassLoader.getSystemClassLoader();
    }
    InputStream is = cl.getResourceAsStream(name);
    if (is == null) {
      is = new FileInputStream(name);
    }
    return new BufferedInputStream(is);
  }

  public static byte[] loadResource(String name) {
    return loadResource(null, name);
  }

  public static synchronized byte[] loadResource(ClassLoader cl, String name) {
    InputStream is = null;
    try {
      is = openResourceStream(cl, name);
      int bytesRead = is.read(s_work, 0, s_work.length);
      byte[] rd = new byte[bytesRead];
      System.arraycopy(s_work, 0, rd, 0, bytesRead);
      return rd;
    } catch (IOException io) {
      return null;
    } finally {
      try {
        if (is != null) {
          is.close();
        }
      } catch (Exception e) {
      }
    }
  }

  public static ImageIcon loadImageIcon(ClassLoader cl, String name) {
    byte[] bytes = loadResource(cl, name);
    return new ImageIcon(bytes);
  }

  public static ImageIcon loadImageIcon(String name) {
    return loadImageIcon(null, name);
  }

}
