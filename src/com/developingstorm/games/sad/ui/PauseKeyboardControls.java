package com.developingstorm.games.sad.ui;

import java.awt.event.KeyEvent;

import com.developingstorm.games.sad.OrderType;

/**

 * 
 */
public class PauseKeyboardControls implements KeyboardControls {

  private boolean _controlSet = false;
  private UserAction _currentAction;
  private UserCommands _commander;

  public PauseKeyboardControls(UserCommands commander) {
    _currentAction = null;
    _commander = commander;
  }

  /**
     *  
     */
  public void keyPressed(KeyEvent ke) {
    if (_commander.isWaiting() == false) {
      return;
    }

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
    if (_commander.isWaiting() == false) {
      return;
    }

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
    if (_commander.isWaiting() == false) {
      return;
    }

    if (_currentAction != null) {
      _currentAction.keyTyped(ke);
      return;
    }

  }

  public void clearAction() {
    _currentAction = null;
  }

}
