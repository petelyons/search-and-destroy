package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.ui.UIMode;

/**
 * Acts as a splitter to send UI Listeners to the mode appropriate handler
 */
public class UIController {

  

  static class Mode {
    public BaseCommander commander;
    public BaseController controller;
  }
  
  static class GameMode extends Mode {
    GameMode(SaDFrame frame, Game game) {
      commander =  new GameCommander(frame, game);
      controller = new GameModeController(frame, (GameCommander) commander);
    }
  }

  static class PathsMode extends Mode {
    PathsMode(SaDFrame frame, Game game) {
      commander =  new PathsCommander(frame, game);
      controller = new PathsModeController(frame, (PathsCommander) commander);
    }
  }

  static class ExploreMode extends Mode {
    ExploreMode(SaDFrame frame, Game game) {
      commander =  new ExploreCommander(frame, game);
      controller = new ExploreModeController(frame, (ExploreCommander) commander);
    }
  }

  
  private UIMode _modeMode;
  private Mode _mode;
  private SaDFrame _frame;
  private Game _game;
  private BoardCanvas _canvas;
  private MouseListener _mouseListener;
  private MouseMotionListener _mouseMotionListener;
  private KeyListener _keyboardListener;
  private Map<UIMode, Mode> _modes;
  
  
  public UIController(SaDFrame frame, Game game) {
    
    _modes = new HashMap<UIMode, Mode>();
    
    _frame = frame;
    _game = game;
    _canvas = _frame.getCanvas();
    _modes.put(UIMode.GAME, new GameMode(_frame, _game));
    _modes.put(UIMode.PATHS, new PathsMode(_frame, _game));
    _modes.put(UIMode.EXPLORE, new ExploreMode(_frame, _game));
   
    switchMode(UIMode.GAME);
   
    _mouseListener = new MouseListener() {

      @Override
      public void mouseClicked(MouseEvent e) {
        _mode.controller.mouseListener().mouseClicked(e);
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        _mode.controller.mouseListener().mouseEntered(e);  
      }

      @Override
      public void mouseExited(MouseEvent e) {
        _mode.controller.mouseListener().mouseExited(e);
      }

      @Override
      public void mousePressed(MouseEvent e) {
        _mode.controller.mouseListener().mousePressed(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        _mode.controller.mouseListener().mouseReleased(e);
      }
      
    };
    
    _mouseMotionListener = new MouseMotionListener() {

      @Override
      public void mouseDragged(MouseEvent e) {
        _mode.controller.mouseMotionListener().mouseDragged(e);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        _mode.controller.mouseMotionListener().mouseMoved(e);
      }
      
    };
    
    _keyboardListener = new KeyListener() {

      @Override
      public void keyPressed(KeyEvent e) {
        _mode.controller.keyListener().keyPressed(e);    
      }

      @Override
      public void keyReleased(KeyEvent e) {
        _mode.controller.keyListener().keyReleased(e); 
      }

      @Override
      public void keyTyped(KeyEvent e) {
        _mode.controller.keyListener().keyTyped(e); 
      }
      
    };
  }
  
  
  public void switchMode(UIMode modeMode) {
    
    if (modeMode == _modeMode) {
      return;
    }  
    _modeMode = modeMode;  
    _mode = _modes.get(_modeMode);
    _canvas.setUIMode(_modeMode);
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

  public UIMode getUIMode() {
    return _modeMode;  
  }

  public PathsCommander getPathsCommander() {
    Mode mode = _modes.get(UIMode.PATHS);
    return (PathsCommander) mode.commander;
  }
}
