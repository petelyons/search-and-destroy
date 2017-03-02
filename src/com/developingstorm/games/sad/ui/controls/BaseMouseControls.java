package com.developingstorm.games.sad.ui.controls;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.hexboard.BoardHex;

/**

 * 
 */
abstract public class BaseMouseControls {

  protected BaseCommander _commander;

  private MouseListener _mouseListener;
  private MouseMotionListener _mouseMotionListener;

  
  BaseMouseControls(BaseCommander c) {
    _commander = c;
    
    _mouseListener = new MouseListener() {

      @Override
      public void mousePressed(MouseEvent e) {
        BoardHex hex = _commander.trans(e.getPoint());
        if (isChooseEvent(e)) {
          _commander.choose(hex);
          return;
        }
        extMousePressed(e, hex);
      }
      @Override
      public void mouseReleased(final MouseEvent e) {
        BoardHex hex = _commander.trans(e.getPoint());
        if (isChooseEvent(e)) {
          _commander.choose(hex);
          return;
        }
        extMouseReleased(e, hex);
      }
      @Override
      public void mouseClicked(final MouseEvent e) {
        BoardHex hex = _commander.trans(e.getPoint());
        extMouseClicked(e, hex);
      }
      @Override
      public void mouseEntered(MouseEvent e) {
        BoardHex hex = _commander.trans(e.getPoint());
        _commander.refocus();
        extMouseEntered(e, hex);
      }

      @Override
      public void mouseExited(MouseEvent e) {
        BoardHex hex = _commander.trans(e.getPoint());
        extMouseExited(e, hex);
      }

      
    };
    
    _mouseMotionListener = new MouseMotionListener() {

      @Override
      public void mouseDragged(MouseEvent e) {
        BoardHex hex = _commander.trans(e.getPoint());
        extMouseDragged(e, hex);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        BoardHex hex = _commander.trans(e.getPoint());
        extMouseMoved(e, hex);
      }
      
    };
  }
  
  
  public MouseListener mouseListener() {
    return _mouseListener;
  }

  public MouseMotionListener mouseMotionListener() {
    return _mouseMotionListener;
  }

  protected static boolean isChooseEvent(MouseEvent e) {
    return (e.isPopupTrigger());
  }

  public abstract void extMousePressed(MouseEvent event, BoardHex hex);
  public abstract void extMouseReleased(MouseEvent event, BoardHex hex);
  public abstract void extMouseClicked(MouseEvent e, BoardHex hex);
  public abstract void extMouseEntered(MouseEvent e, BoardHex hex);
  public abstract void extMouseDragged(MouseEvent e, BoardHex hex);
  public abstract void extMouseMoved(MouseEvent e, BoardHex hex);
  public abstract void extMouseExited(MouseEvent e, BoardHex hex);

}
