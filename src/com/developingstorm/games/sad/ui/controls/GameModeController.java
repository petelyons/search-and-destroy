package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.sad.ui.SaDFrame;

/**

 * 
 */
public class GameModeController extends BaseController {

  private GameCommander _commander;
  private SaDFrame _frame;

  public GameModeController(SaDFrame frame,  GameCommander commander) {
    super(frame.getCanvas());
    
    _frame = frame;
    _commander = commander;
    init(new GameModeMouseControls(_frame, _commander),  new GameModeKeyboardControls(_frame, _commander));
  }
  
  
}
