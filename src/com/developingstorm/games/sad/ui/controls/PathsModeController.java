package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;

/**
 * 
 */
public class PathsModeController  extends BaseController {

  private final PathsCommander _commander;
  private final SaDFrame _frame;
  private final KeyListener _keyListener;
  private final HexMouseListenerAdapter _hexMouseListenerAdapter;
  private final HexMouseMotionListenerAdapter _hexMouseMotionListenerAdapter;
  private BoardHex _mouseDown;
  
  public PathsModeController(SaDFrame frame,  PathsCommander commander) {
    _frame = frame;
    _commander = commander;
   
    _keyListener = new KeyListener() {
      private boolean _controlSet = false;
      
      @Override
      public void keyPressed(KeyEvent ke) {
        
         OrderType ot;

        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
          _controlSet = true;
        }

        switch (ke.getKeyCode()) {
        case KeyEvent.VK_LEFT:
          break;
        case KeyEvent.VK_RIGHT:
          break;
        case KeyEvent.VK_PAGE_UP:
          break;
        case KeyEvent.VK_PAGE_DOWN:
          break;
        case KeyEvent.VK_HOME:
          break;
        case KeyEvent.VK_END:
          break;
        }
      }

      @Override
      public void keyReleased(KeyEvent ke) {
     
        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
          _controlSet = false;
        }

       }
      @Override
      public void keyTyped(KeyEvent ke) {
      }

    };
    
    _hexMouseListenerAdapter = new HexMouseListenerAdapter(commander, new IHexMouseListener() {
            
      @Override
      public void hexMousePressed(MouseEvent e, BoardHex hex) {
        int button = e.getButton();
        Location loc = hex.getLocation();
        if (button == MouseEvent.BUTTON1) {
          if (_commander.isValidDestination(hex)) {
            Log.info("Setting destination");
            _commander.setDestination(hex);
            _commander.endPathsMode();
          }
        }
      }

      @Override
      public void hexMouseReleased(MouseEvent e, BoardHex hex) {
      }

      @Override
      public void hexMouseClicked(MouseEvent e, BoardHex hex) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void hexMouseEntered(MouseEvent e, BoardHex hex) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void hexMouseExited(MouseEvent e, BoardHex hex) {
        // TODO Auto-generated method stub
        
      }
    });
    
    _hexMouseMotionListenerAdapter = new HexMouseMotionListenerAdapter(commander, new IHexMouseMotionListener() {
            
      @Override
      public void hexMouseDragged(MouseEvent e, BoardHex hex) {
    
      }

      @Override
      public void hexMouseMoved(MouseEvent e, BoardHex hex) {
        
        Location start = _commander.autoDraggingLocation();
        if (start != null) {
          if (hex != null && !hex.equals(start)) {
            _commander.showLine(start, hex.getLocation());
          }
        }
        
      }

    });
  }
  
 
  @Override
  public MouseListener mouseListener() {
    return _hexMouseListenerAdapter;
  }

  @Override
  public MouseMotionListener mouseMotionListener() {
    return _hexMouseMotionListenerAdapter;
  }

  @Override
  public KeyListener keyListener() {
    return _keyListener;
  }

  @Override
  public void clearAction() {
  }
}
