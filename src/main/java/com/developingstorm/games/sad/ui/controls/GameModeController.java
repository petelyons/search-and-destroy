package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;

/**
 * 
 */
public class GameModeController extends BaseController {
  
  

  private final int boardMiddle;
  private final GameCommander commander;
  private final SaDFrame frame;
  private UserAction currentAction;
  private final KeyListener keyListener;
  private final HexMouseListenerAdapter hexMouseListenerAdapter;
  private final HexMouseMotionListenerAdapter hexMouseMotionListenerAdapter;
  private BoardHex mouseDown;
  
  public GameModeController(SaDFrame frame,  GameCommander commander) {
   
    currentAction = null;
    this.frame = frame;
    this.commander = commander;
    boardMiddle = (this.commander.boardWidth() / 2);
  
    keyListener = new KeyListener() {
      private boolean controlSet = false;
      
      @Override
      public void keyPressed(KeyEvent ke) {
        int code = ke.getKeyCode();
        if (code == KeyEvent.VK_F5) {
          SaDFrame.DEBUG_PATH_TOGGLE = !SaDFrame.DEBUG_PATH_TOGGLE; 
        } else if (code == KeyEvent.VK_F8) {
          if (GameModeController.this.commander.isPaused()) {
            GameModeController.this.commander.resume();
          } else {
            GameModeController.this.commander.pause();
          }
        }
        
        
        if (GameModeController.this.commander.isPaused() == false) {
          return;
        }

        if (GameModeController.this.currentAction != null) {
          GameModeController.this.currentAction.keyPressed(ke);
          return;
        }

        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
          controlSet = true;
        }

        Location loc = GameModeController.this.commander.getCurrentLocation();

        
        switch (ke.getKeyCode()) {
        case KeyEvent.VK_UP:
          if (loc.x >= GameModeController.this.boardMiddle) {
            GameModeController.this.commander.moveNorthWest();
          } else {
            GameModeController.this.commander.moveNorthEast();
          }
          break;
        case KeyEvent.VK_DOWN:
          if (loc.x >= GameModeController.this.boardMiddle) {
            GameModeController.this.commander.moveSouthWest();
          } else { 
            GameModeController.this.commander.moveSouthWest();
          }
          break;
        case KeyEvent.VK_LEFT:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.moveWest();
          }
          break;
        case KeyEvent.VK_RIGHT:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.moveEast();
          }
          break;
        case KeyEvent.VK_PAGE_UP:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.moveNorthEast();
          }
          break;
        case KeyEvent.VK_PAGE_DOWN:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.moveSouthEast();
          }
          break;
        case KeyEvent.VK_HOME:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.moveNorthWest();
          }

          break;
        case KeyEvent.VK_END:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.moveSouthWest();
          }
          break;
        case KeyEvent.VK_X:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.explore();
          }
          break;
        case KeyEvent.VK_SPACE:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.skipTurn();
          }
          break;

        case KeyEvent.VK_S:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.sentry();
          }
          break;
        case KeyEvent.VK_U:
          if (this.controlSet) {
          } else {
            GameModeController.this.commander.unload();
          }
          break;
        case KeyEvent.VK_T:
          currentAction = new MoveController(GameModeController.this.commander, GameModeController.this);
          break;

        case KeyEvent.VK_H:
          GameModeController.this.commander.headHome();
          break;

        case KeyEvent.VK_K:
          GameModeController.this.commander.disband();
          break;

        case KeyEvent.VK_C:
          GameModeController.this.commander.center();
        }
      }

      @Override
      public void keyReleased(KeyEvent ke) {
        if (GameModeController.this.commander.isPaused() == false) {
          return;
        }

        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
          controlSet = false;
        }

        if (GameModeController.this.currentAction != null) {
          GameModeController.this.currentAction.keyReleased(ke);
          return;
        }

      }

      @Override
      public void keyTyped(KeyEvent ke) {
        if (GameModeController.this.commander.isPaused() == false) {
          return;
        }

        if (GameModeController.this.currentAction != null) {
          GameModeController.this.currentAction.keyTyped(ke);
          return;
        }

      }
    };
  
    hexMouseListenerAdapter = new HexMouseListenerAdapter(commander, new IHexMouseListener() {
      
  
      @Override
      public void hexMousePressed(MouseEvent e, BoardHex hex) {
        //Log.info("GAME MOUSE DOWN *******************************************");
        if (GameModeController.this.commander.isPaused() == false) {
          return;
        }
        int button = e.getButton();
        if (button == MouseEvent.BUTTON1) {
          if (GameModeController.this.commander.isDraggable(hex)) {
            mouseDown = hex;
          }
        }
      }

      @Override
      public void hexMouseReleased(MouseEvent e, BoardHex hex) {
        //Log.info("GAME MOUSE UP ***********************************************");

        if (GameModeController.this.commander.isPaused() == false) {
          return;
        }
        int button = e.getButton();
        if (hex == null)
          return;

        Location loc = hex.getLocation();

        if (button == MouseEvent.BUTTON1 && !e.isPopupTrigger()) {
          if (GameModeController.this.mouseDown != null) {
            GameModeController.this.commander.showLine(null, null);
            if (!GameModeController.this.mouseDown.getLocation().equals(loc)) {
              GameModeController.this.commander.move(GameModeController.this.mouseDown.getLocation(), loc);
            } else {
              GameModeController.this.commander.activate(hex);
            }
            mouseDown = null;
          }
        }
        mouseDown = null;
      }

      @Override
      public void hexMouseClicked(MouseEvent e, BoardHex hex) {
      }

      @Override
      public void hexMouseEntered(MouseEvent e, BoardHex hex) {
        
      }

      @Override
      public void hexMouseExited(MouseEvent e, BoardHex hex) {
     
      }
    });
    
    
    
    hexMouseMotionListenerAdapter = new HexMouseMotionListenerAdapter(commander, new IHexMouseMotionListener() {
    
      @Override
      public void hexMouseDragged(MouseEvent e, BoardHex hex) {
        if (GameModeController.this.commander.isPaused() == false) {
          return;
        }
        if (GameModeController.this.mouseDown != null) {
          if (hex != null && !hex.equals(GameModeController.this.mouseDown)) {
            GameModeController.this.commander.showLine(GameModeController.this.mouseDown.getLocation(), hex.getLocation());
          }
        }
      }

      @Override
      public void hexMouseMoved(MouseEvent e, BoardHex hex) {

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
    currentAction = null;
    
  }
  
  
}
