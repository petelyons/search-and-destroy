package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.ui.UIMode;

public class UIController {

  private UIMode _mode;
  private GameCommander _gameCommander;
  private PathsCommander _pathsCommander;

  private GameModeController _gameControls;
  private PathsModeController _pathsControls;
  private SaDFrame _frame;
  private Game _game;
  private BoardCanvas _canvas;
  
  public UIController(SaDFrame frame, Game game) {
    
    
    _frame = frame;
    _game = game;
    _canvas = _frame.getCanvas();
    
    _gameCommander = new GameCommander(_frame, _canvas, _game);
    _pathsCommander = new PathsCommander(_frame, _canvas, _game);

    _gameControls = new GameModeController(_frame, _gameCommander);
    _pathsControls = new PathsModeController(_frame, _pathsCommander);
  
    switchMode(UIMode.GAME);
  }
  
  public void switchMode(UIMode mode) {
    
    if (mode == _mode) {
      return;
    } 
    
    _mode = mode;
    
    _canvas.setUIMode(mode);
    
    switch(mode) {
    case GAME:
      
      _pathsControls.disable();
      _gameControls.enable();
      

      break;
    case PATHS:
      _gameControls.disable();
      _pathsControls.enable();
      
      break;
    }
      
    
  }
}
