package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.ui.UIMode;

/**
 * Acts as a splitter to send UI Listeners to the mode appropriate handler
 */
public class UIController {

  private UIMode _mode;
  private GameCommander _gameCommander;
  private PathsCommander _pathsCommander;

  private GameModeController _gameControls;
  private PathsModeController _pathsControls;
  private SaDFrame _frame;
  private Game _game;
  private BoardCanvas _canvas;
  private MouseListener _mouseListener;
  private MouseMotionListener _mouseMotionListener;
  private KeyListener _keyboardListener;
  
  private BaseController[] _controlers;
  
  
  public UIController(SaDFrame frame, Game game) {
    
    
    _frame = frame;
    _game = game;
    _canvas = _frame.getCanvas();
    
    _gameCommander = new GameCommander(_frame, _canvas, _game);
    _pathsCommander = new PathsCommander(_frame, _canvas, _game);

    _gameControls = new GameModeController(_frame, _gameCommander);
    _pathsControls = new PathsModeController(_frame, _pathsCommander);
   
    int max = 0;
    for (UIMode mode : UIMode.values()) {
      if (mode.ordinal() > max) {
        max = mode.ordinal();
      }
    }
    
    _controlers = new BaseController[max + 1];
    _controlers[UIMode.GAME.ordinal()] = _gameControls;
    _controlers[UIMode.PATHS.ordinal()] = _pathsControls;
  
    switchMode(UIMode.GAME);
   
    _mouseListener = new MouseListener() {

      @Override
      public void mouseClicked(MouseEvent e) {
        _controlers[_mode.ordinal()].mouseListener().mouseClicked(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        _controlers[_mode.ordinal()].mouseListener().mouseEntered(e);  
      }

      @Override
      public void mouseExited(MouseEvent e) {
        _controlers[_mode.ordinal()].mouseListener().mouseExited(e);
      }

      @Override
      public void mousePressed(MouseEvent e) {
        _controlers[_mode.ordinal()].mouseListener().mousePressed(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        _controlers[_mode.ordinal()].mouseListener().mouseReleased(e);
      }
      
    };
    
    _mouseMotionListener = new MouseMotionListener() {

      @Override
      public void mouseDragged(MouseEvent e) {
        _controlers[_mode.ordinal()].mouseMotionListener().mouseDragged(e);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        _controlers[_mode.ordinal()].mouseMotionListener().mouseMoved(e);
      }
      
    };
    
    _keyboardListener = new KeyListener() {

      @Override
      public void keyPressed(KeyEvent e) {
        _controlers[_mode.ordinal()].keyListener().keyPressed(e);    
      }

      @Override
      public void keyReleased(KeyEvent e) {
        _controlers[_mode.ordinal()].keyListener().keyReleased(e); 
      }

      @Override
      public void keyTyped(KeyEvent e) {
        _controlers[_mode.ordinal()].keyListener().keyTyped(e); 
      }
      
    };
  }
  
  public void switchMode(UIMode mode) {
    
    if (mode == _mode) {
      return;
    }  
    _mode = mode;  
    _canvas.setUIMode(mode);
  }
  

  
  public MouseListener mouseListener() {
    return _mouseListener;
  }

  public MouseMotionListener mouseMotionListener() {
    return _mouseMotionListener;
  }

  public KeyListener keyListener() {
    return _keyboardListener;
  }
}
