package com.developingstorm.games.sad.ui;

import java.awt.event.KeyEvent;

import com.developingstorm.games.sad.OrderType;
import com.developingstorm.util.Tracer;

/**

 * 
 */
public class PlayKeyboardControls implements KeyboardControls {

  private boolean _controlSet = false;
  private UserAction _currentAction;
  private UserCommands _commander;

  public PlayKeyboardControls(UserCommands commander) {
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
      if (_controlSet) {
      } else {
        _commander.moveWest();
      }
      break;
    case KeyEvent.VK_RIGHT:
      if (_controlSet) {
      } else {
        _commander.moveEast();
      }
      break;
    case KeyEvent.VK_PAGE_UP:
      if (_controlSet) {
      } else {
        _commander.moveNorthEast();
      }
      break;
    case KeyEvent.VK_PAGE_DOWN:
      if (_controlSet) {
      } else {
        _commander.moveSouthEast();
      }
      break;
    case KeyEvent.VK_HOME:
      if (_controlSet) {
      } else {
        _commander.moveNorthWest();
      }

      break;
    case KeyEvent.VK_END:
      if (_controlSet) {
      } else {
        _commander.moveSouthWest();
      }
      break;
    case KeyEvent.VK_X:
      if (_controlSet) {
      } else {
        _commander.explore();
      }
      break;
    case KeyEvent.VK_SPACE:
      if (_controlSet) {
      } else {
        _commander.skipTurn();
      }
      break;

    case KeyEvent.VK_S:
      if (_controlSet) {
      } else {
        _commander.sentry();
      }
      break;
    case KeyEvent.VK_U:
      if (_controlSet) {
      } else {
        _commander.unload();
      }
      break;
    case KeyEvent.VK_T:
      _currentAction = new MoveAction(_commander, this);
      break;

    case KeyEvent.VK_H:
      _commander.headHome();
      break;

    case KeyEvent.VK_K:
      _commander.disband();
      break;

    case KeyEvent.VK_C:
      _commander.center();
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
