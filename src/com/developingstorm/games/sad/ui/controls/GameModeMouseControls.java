package com.developingstorm.games.sad.ui.controls;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.ui.SaDFrame;

/**

 * 
 */
public class GameModeMouseControls extends BaseMouseControls {

  private BoardHex _mouseDown;
  private SaDFrame _frame;
  private GameCommander _commander;
  
  public GameModeMouseControls(SaDFrame frame, GameCommander commander) {
    super(commander);
    _frame = frame;
    _mouseDown = null;
    _commander = commander;
  }

  @Override
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

  @Override
  public void extMousePressed(MouseEvent e, BoardHex hex) {
    if (_commander.isWaiting() == false) {
      return;
    }
    int button = e.getButton();
    if (button == MouseEvent.BUTTON1) {
      if (_commander.isDraggable(hex)) {
        _mouseDown = hex;
      }
    }
  }

  @Override
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

  @Override
  public void extMouseClicked(MouseEvent e, BoardHex hex) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void extMouseEntered(MouseEvent e, BoardHex hex) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void extMouseMoved(MouseEvent e, BoardHex hex) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void extMouseExited(MouseEvent e, BoardHex hex) {
    // TODO Auto-generated method stub
    
  }
}
