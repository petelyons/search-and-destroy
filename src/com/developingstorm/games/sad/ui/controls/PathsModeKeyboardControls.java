package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;

import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ui.KeyboardControls;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.ui.UserAction;

/**

 * 
 */
public class PathsModeKeyboardControls implements KeyboardControls {

  private boolean _controlSet = false;
  private UserAction _currentAction;
  private PathsCommander _commander;

  public PathsModeKeyboardControls(SaDFrame frame, PathsCommander commander) {
    _currentAction = null;
    _commander = commander;
  }

  /**
     *  
     */
  public void keyPressed(KeyEvent ke) {
 
    if (_currentAction != null) {
      _currentAction.keyPressed(ke);
      return;
    }

    OrderType ot;

    if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
      _controlSet = true;
    }

    switch (ke.getKeyCode()) {
    case KeyEvent.VK_LEFT:
      break;
    case KeyEvent.VK_RIGHT:
      break;
    case KeyEvent.VK_PAGE_UP:
      break;
    case KeyEvent.VK_PAGE_DOWN:
      break;
    case KeyEvent.VK_HOME:
      break;
    case KeyEvent.VK_END:
      break;
    }
  }

  /**
     *  
     */
  public void keyReleased(KeyEvent ke) {
 
    if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
      _controlSet = false;
    }

    if (_currentAction != null) {
      _currentAction.keyReleased(ke);
      return;
    }

  }

  /**
     *  
     */
  public void keyTyped(KeyEvent ke) {

    if (_currentAction != null) {
      _currentAction.keyTyped(ke);
      return;
    }

  }

  public void clearAction() {
    _currentAction = null;
  }

}
