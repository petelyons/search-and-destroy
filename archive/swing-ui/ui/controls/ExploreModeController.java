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
public class ExploreModeController  extends BaseController {

  private final ExploreCommander commander;
  private final SaDFrame frame;
  private final KeyListener keyListener;
  private final HexMouseListenerAdapter hexMouseListenerAdapter;
  private final HexMouseMotionListenerAdapter hexMouseMotionListenerAdapter;
  private BoardHex mouseDown;
  
  public ExploreModeController(SaDFrame frame, ExploreCommander commander) {
    this.frame = frame;
    this.commander = commander;
   
    keyListener = new KeyListener() {
      private boolean controlSet = false;
      
      @Override
      public void keyPressed(KeyEvent ke) {
        
         OrderType ot;

        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
          controlSet = true;
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
          controlSet = false;
        }

       }
      @Override
      public void keyTyped(KeyEvent ke) {
      }

    };
    
    hexMouseListenerAdapter = new HexMouseListenerAdapter(commander, new IHexMouseListener() {
            
      @Override
      public void hexMousePressed(MouseEvent e, BoardHex hex) {
        //Log.info("EXPLORE MOUSE DOWN *******************************************");
        int button = e.getButton();
        Location loc = hex.getLocation();
        if (button == MouseEvent.BUTTON1) {
          if (ExploreModeController.this.commander.isValidDestination(hex)) {
            Log.info("Setting destination");
            ExploreModeController.this.commander.setDestination(hex);
          }
        }
      }

      @Override
      public void hexMouseReleased(MouseEvent e, BoardHex hex) {
        //Log.info("EXPLORE MOUSE UP *******************************************");

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
    
    hexMouseMotionListenerAdapter = new HexMouseMotionListenerAdapter(commander, new IHexMouseMotionListener() {
            
      @Override
      public void hexMouseDragged(MouseEvent e, BoardHex hex) {
    
      }

      @Override
      public void hexMouseMoved(MouseEvent e, BoardHex hex) {
        
        Location start = ExploreModeController.this.commander.autoDraggingLocation();
        if (start != null) {
          if (hex != null && !hex.equals(start)) {
            ExploreModeController.this.commander.showLine(start, hex.getLocation());
          }
        }
        
      }

    });
  }
  
 
  @Override
  public MouseListener mouseListener() {
    return hexMouseListenerAdapter;
  }

  @Override
  public MouseMotionListener mouseMotionListener() {
    return hexMouseMotionListenerAdapter;
  }

  @Override
  public KeyListener keyListener() {
    return keyListener;
  }

  @Override
  public void clearAction() {
  }
}
