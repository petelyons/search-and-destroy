package com.developingstorm.util;

import java.awt.event.KeyEvent;

import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

/**

 * 
 */
public class NoKeyScrollPane extends JScrollPane {

  public boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition,
      boolean pressed) {
    return false;
  }

  public boolean isFocusable() {
    return false;
  }

}
