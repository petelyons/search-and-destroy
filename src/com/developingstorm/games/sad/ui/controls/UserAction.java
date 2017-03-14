package com.developingstorm.games.sad.ui.controls;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import com.developingstorm.games.hexboard.BoardHex;

/**
 *
 *
 */
public class UserAction implements UserActionListener {

  protected UserActionOwner _owner;
  protected GameCommander _commander;

  public UserAction(GameCommander commander, UserActionOwner owner) {
    _owner = owner;
    _commander = commander;
  }

  public void keyPressed(KeyEvent ke) {
  }

  public void keyReleased(KeyEvent ke) {
  }

  public void mousePressed(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    extMousePressed(e, hex);
  }

  public void mouseReleased(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    extMouseReleased(e, hex);
  }

  public void mouseClicked(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
    extMouseClicked(e, hex);
  }

  public void mouseEntered(MouseEvent e) {
    BoardHex hex = _commander.trans(e.getPoint());
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

  public void actionPerformed(ActionEvent arg0) {
  }

  public void keyTyped(KeyEvent arg0) {
  }

}
