package com.developingstorm.games.sad.ui;

import java.awt.event.MouseEvent;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;

/**

 * 
 */
public class PauseMouseControls extends BaseMouseControls {

  private boolean _controlSet = false;
  private UserAction _currentAction;
  private BoardHex _mouseDown;

  public PauseMouseControls(UserCommands commander) {
    super(commander);
    _currentAction = null;
    _mouseDown = null;
  }

  public void extMousePressed(MouseEvent e, BoardHex hex) {
    if (_commander.isWaiting() == false) {
      return;
    }
  }

  public void extMouseReleased(MouseEvent e, BoardHex hex) {
    if (_commander.isWaiting() == false) {
      return;
    }
    int button = e.getButton();
    if (hex == null)
      return;
    Location loc = hex.getLocation();
    if (_mouseDown != null) {

    } else if (button == MouseEvent.BUTTON1) {
      _commander.setFocus(hex);
    }
    _mouseDown = null;
  }

}
