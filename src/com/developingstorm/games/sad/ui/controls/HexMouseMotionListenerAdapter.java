package com.developingstorm.games.sad.ui.controls;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.hexboard.BoardHex;

/**
 * Adapts typical java mouse events to Hex board related mouse events
 */
public class HexMouseMotionListenerAdapter  implements MouseMotionListener {

  protected BaseCommander _commander;
  private IHexMouseMotionListener _extListener;
  
  HexMouseMotionListenerAdapter(BaseCommander c, IHexMouseMotionListener extListener) {
    _commander = c;
    _extListener = extListener;
  }
  
  @Override
  public void mouseDragged(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    _extListener.hexMouseDragged(e, hex);
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    _extListener.hexMouseMoved(e, hex);
  }
 

}
