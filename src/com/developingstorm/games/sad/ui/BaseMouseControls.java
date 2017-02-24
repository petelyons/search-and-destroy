package com.developingstorm.games.sad.ui;

import java.awt.event.MouseEvent;

import com.developingstorm.games.hexboard.BoardHex;

/**

 * 
 */
abstract public class BaseMouseControls implements MouseControls {

  protected UserCommands _commander;

  BaseMouseControls(UserCommands c) {
    _commander = c;
  }

  protected boolean isChooseEvent(MouseEvent e) {
    return (e.isPopupTrigger());
  }

  public void mousePressed(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());

    if (isChooseEvent(e)) {
      _commander.choose(hex);
      return;
    }

    extMousePressed(e, hex);
  }

  public void mouseReleased(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());

    if (isChooseEvent(e)) {
      _commander.choose(hex);
      return;
    }

    extMouseReleased(e, hex);
  }

  public void mouseClicked(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    extMouseClicked(e, hex);
  }

  public void mouseEntered(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());

    _commander.refocus();

    extMouseEntered(e, hex);
  }

  public void mouseDragged(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    extMouseDragged(e, hex);
  }

  public void mouseMoved(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    extMouseMoved(e, hex);
  }

  public void mouseExited(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    extMouseExited(e, hex);
  }

  public void extMousePressed(MouseEvent event, BoardHex hex) {

  }

  public void extMouseReleased(MouseEvent event, BoardHex hex) {

  }

  public void extMouseClicked(MouseEvent e, BoardHex hex) {

  }

  public void extMouseEntered(MouseEvent e, BoardHex hex) {

  }

  public void extMouseDragged(MouseEvent e, BoardHex hex) {

  }

  public void extMouseMoved(MouseEvent e, BoardHex hex) {

  }

  public void extMouseExited(MouseEvent e, BoardHex hex) {

  }

}
