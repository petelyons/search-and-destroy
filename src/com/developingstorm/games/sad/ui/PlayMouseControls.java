package com.developingstorm.games.sad.ui;

import java.awt.event.MouseEvent;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Unit;

/**

 * 
 */
public class PlayMouseControls extends BaseMouseControls {

  private boolean _controlSet = false;
  private UserAction _currentAction;
  private BoardHex _mouseDown;
  private Unit _actionUnit;

  public PlayMouseControls(UserCommands commander) {
    super(commander);
    _currentAction = null;
    _mouseDown = null;
    _actionUnit = null;
  }

  public void extMouseDragged(MouseEvent e, BoardHex hex) {
    if (_commander.isWaiting() == false) {
      return;
    }
    if (_mouseDown != null) {
      if (hex != null && !hex.equals(_mouseDown)) {
        _commander.showLine(_mouseDown.getLocation(), hex.getLocation());
      }
    }
  }

  public void extMousePressed(MouseEvent e, BoardHex hex) {
    if (_commander.isWaiting() == false) {
      return;
    }
    int button = e.getButton();
    Location loc = hex.getLocation();
    if (button == MouseEvent.BUTTON1) {
      if (_commander.isDraggable(hex)) {
        _mouseDown = hex;
      }
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

    if (button == MouseEvent.BUTTON1 && !e.isPopupTrigger()) {
      if (_mouseDown != null) {
        _commander.showLine(null, null);
        if (!_mouseDown.getLocation().equals(loc)) {
          _commander.move(_mouseDown.getLocation(), loc);
        } else {
          _commander.activate(hex);
        }
        _mouseDown = null;
      }
    }
    _mouseDown = null;
  }

}
