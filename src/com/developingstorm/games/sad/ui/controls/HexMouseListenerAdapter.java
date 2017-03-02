package com.developingstorm.games.sad.ui.controls;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.hexboard.BoardHex;

/**
 * Adapts typical java mouse events to Hex board related mouse events
 */
public class HexMouseListenerAdapter  implements MouseListener {

  protected BaseCommander _commander;
  private IHexMouseListener _extListener;
  
  HexMouseListenerAdapter(BaseCommander c, IHexMouseListener extListener) {
    _commander = c;
    _extListener = extListener;
  }
  

  @Override
  public void mousePressed(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    if (isChooseEvent(e)) {
      _commander.choose(hex);
      return;
    }
    _extListener.hexMousePressed(e, hex);
  }
  @Override
  public void mouseReleased(final MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    if (isChooseEvent(e)) {
      _commander.choose(hex);
      return;
    }
    _extListener.hexMouseReleased(e, hex);
  }
  @Override
  public void mouseClicked(final MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    _extListener.hexMouseClicked(e, hex);
  }
  @Override
  public void mouseEntered(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    _commander.refocus();
    _extListener.hexMouseEntered(e, hex);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    _extListener.hexMouseExited(e, hex);
  }
  
  public static boolean isChooseEvent(MouseEvent e) {
    return (e.isPopupTrigger());
  }

 

}
