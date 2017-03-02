package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ui.KeyboardControls;
import com.developingstorm.games.sad.ui.MoveAction;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.ui.UserAction;

/**

 * 
 */
public class GameModeKeyboardControls implements KeyboardControls {

  private boolean _controlSet = false;
  private UserAction _currentAction;
  private GameCommander _commander;
  private int _boardMiddle;
  private SaDFrame _frame;

  public GameModeKeyboardControls(SaDFrame frame, GameCommander commander) {
    _frame = frame;
    _currentAction = null;
    _commander = commander;
    _boardMiddle = (_commander.boardWidth() / 2);
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

    Location loc = _commander.getCurrentLocation();

    
    switch (ke.getKeyCode()) {
    case KeyEvent.VK_UP:
      if (loc.x >= _boardMiddle) {
        _commander.moveNorthWest();
      } else {
        _commander.moveNorthEast();
      }
      break;
    case KeyEvent.VK_DOWN:
      if (loc.x >= _boardMiddle) {
        _commander.moveSouthWest();
      } else { 
        _commander.moveSouthWest();
      }
      break;
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
