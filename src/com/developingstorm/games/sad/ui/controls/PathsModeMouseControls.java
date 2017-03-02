package com.developingstorm.games.sad.ui.controls;

import java.awt.event.MouseEvent;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.ui.SaDFrame;

/**

 * 
 */
public class PathsModeMouseControls extends BaseMouseControls {

  private BoardHex _mouseDown;
  private PathsCommander _commander;

  public PathsModeMouseControls(SaDFrame frame, PathsCommander commander) {
    super(commander);
    _commander = commander;
    _mouseDown = null;
  }
  
  @Override
  public void extMouseDragged(MouseEvent e, BoardHex hex) {
    if (_mouseDown != null) {
      if (hex != null && !hex.equals(_mouseDown)) {
        _commander.showLine(_mouseDown.getLocation(), hex.getLocation());
      }
    }
  }

  @Override
  public void extMousePressed(MouseEvent e, BoardHex hex) {
    int button = e.getButton();
    Location loc = hex.getLocation();
    if (button == MouseEvent.BUTTON1) {
      if (_commander.isDraggable(hex)) {
        _mouseDown = hex;
      }
    }
  }

  @Override
  public void extMouseReleased(MouseEvent e, BoardHex hex) {
    int button = e.getButton();
    if (hex == null)
      return;
    if (_mouseDown != null) {

    } else if (button == MouseEvent.BUTTON1) {
      _commander.setFocus(hex);
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
