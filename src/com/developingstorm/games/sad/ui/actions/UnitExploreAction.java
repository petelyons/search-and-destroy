package com.developingstorm.games.sad.ui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;


public class UnitExploreAction extends AbstractAction {
  
  public UnitExploreAction(String text,
      String desc, Integer mnemonic) {
    super("Explore");
    putValue(SHORT_DESCRIPTION, "Explore nearby territory");
    putValue(MNEMONIC_KEY, KeyEvent.VK_X);
    putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK));

  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
   
  }
}
