package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.sad.ui.SaDFrame;

/**
 * 
 */
public class PathsModeController  extends BaseController {

  private PathsCommander _commander;
  private SaDFrame _frame;

  public PathsModeController(SaDFrame frame,  PathsCommander commander) {
    super(frame.getCanvas());
    
    _frame = frame;
    _commander = commander;
    init(new PathsModeMouseControls(_frame, _commander),  new PathsModeKeyboardControls(_frame, _commander));
  }
}
