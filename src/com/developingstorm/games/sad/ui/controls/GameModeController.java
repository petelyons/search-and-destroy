package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.ui.MoveAction;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.ui.UserAction;
import com.developingstorm.games.sad.util.Log;

/**

 * 
 */
public class GameModeController extends BaseController {

  private final int _boardMiddle;
  private final GameCommander _commander;
  private final SaDFrame _frame;
  private UserAction _currentAction;
  private final KeyListener _keyListener;
  private final HexMouseListenerAdapter _hexMouseListenerAdapter;
  private final HexMouseMotionListenerAdapter _hexMouseMotionListenerAdapter;
  private BoardHex _mouseDown;
  
  public GameModeController(SaDFrame frame,  GameCommander commander) {
   
    _currentAction = null;
    _frame = frame;
    _commander = commander;
    _boardMiddle = (_commander.boardWidth() / 2);
  
    _keyListener = new KeyListener() {
      private boolean _controlSet = false;
      
      @Override
      public void keyPressed(KeyEvent ke) {
        if (_commander.isWaiting() == false) {
          return;
        }

        if (_currentAction != null) {
          _currentAction.keyPressed(ke);
          return;
        }

        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
          _controlSet = true;
        }

        Location loc = _commander.getCurrentLocation();

        
        switch (ke.getKeyCode()) {
        case KeyEvent.VK_UP:
          if (loc.x >= _boardMiddle) {
            _commander.moveNorthWest();
          } else {
            _commander.moveNorthEast();
          }
          break;
        case KeyEvent.VK_DOWN:
          if (loc.x >= _boardMiddle) {
            _commander.moveSouthWest();
          } else { 
            _commander.moveSouthWest();
          }
          break;
        case KeyEvent.VK_LEFT:
          if (_controlSet) {
          } else {
            _commander.moveWest();
          }
          break;
        case KeyEvent.VK_RIGHT:
          if (_controlSet) {
          } else {
            _commander.moveEast();
          }
          break;
        case KeyEvent.VK_PAGE_UP:
          if (_controlSet) {
          } else {
            _commander.moveNorthEast();
          }
          break;
        case KeyEvent.VK_PAGE_DOWN:
          if (_controlSet) {
          } else {
            _commander.moveSouthEast();
          }
          break;
        case KeyEvent.VK_HOME:
          if (_controlSet) {
          } else {
            _commander.moveNorthWest();
          }

          break;
        case KeyEvent.VK_END:
          if (_controlSet) {
          } else {
            _commander.moveSouthWest();
          }
          break;
        case KeyEvent.VK_X:
          if (_controlSet) {
          } else {
            _commander.explore();
          }
          break;
        case KeyEvent.VK_SPACE:
          if (_controlSet) {
          } else {
            _commander.skipTurn();
          }
          break;

        case KeyEvent.VK_S:
          if (_controlSet) {
          } else {
            _commander.sentry();
          }
          break;
        case KeyEvent.VK_U:
          if (_controlSet) {
          } else {
            _commander.unload();
          }
          break;
        case KeyEvent.VK_T:
          _currentAction = new MoveAction(_commander, GameModeController.this);
          break;

        case KeyEvent.VK_H:
          _commander.headHome();
          break;

        case KeyEvent.VK_K:
          _commander.disband();
          break;

        case KeyEvent.VK_C:
          _commander.center();
        }
      }

      @Override
      public void keyReleased(KeyEvent ke) {
        if (_commander.isWaiting() == false) {
          return;
        }

        if (ke.getKeyCode() == KeyEvent.VK_CONTROL) {
          _controlSet = false;
        }

        if (_currentAction != null) {
          _currentAction.keyReleased(ke);
          return;
        }

      }

      @Override
      public void keyTyped(KeyEvent ke) {
        if (_commander.isWaiting() == false) {
          return;
        }

        if (_currentAction != null) {
          _currentAction.keyTyped(ke);
          return;
        }

      }
    };
  
    _hexMouseListenerAdapter = new HexMouseListenerAdapter(commander, new IHexMouseListener() {
      
  
      @Override
      public void hexMousePressed(MouseEvent e, BoardHex hex) {
        Log.debug("hexMousePressed");
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
      public void hexMouseReleased(MouseEvent e, BoardHex hex) {
        Log.debug("hexMouseReleased");
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
      public void hexMouseClicked(MouseEvent e, BoardHex hex) {
      }

      @Override
      public void hexMouseEntered(MouseEvent e, BoardHex hex) {
        
      }

      @Override
      public void hexMouseExited(MouseEvent e, BoardHex hex) {
     
      }
    });
    
    
    
    _hexMouseMotionListenerAdapter = new HexMouseMotionListenerAdapter(commander, new IHexMouseMotionListener() {
    
      @Override
      public void hexMouseDragged(MouseEvent e, BoardHex hex) {
        Log.debug("hexMouseDragged");
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
      public void hexMouseMoved(MouseEvent e, BoardHex hex) {

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
    // TODO Auto-generated method stub
    
  }
  
  
}
