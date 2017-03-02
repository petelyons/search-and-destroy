package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.KeyboardControls;

/**

 * 
 */
public class BaseController {

  private BoardCanvas _canvas;
  private BaseMouseControls _mc;
  private KeyboardControls _kc;

  protected BaseController(BoardCanvas c) {
    _canvas = c;
  }
  
  
  protected void init(BaseMouseControls m, KeyboardControls k) {
    _mc = m;
    _kc = k;
  }

  public void enable() {
    _canvas.addMouseListener(_mc.mouseListener());
    _canvas.addMouseMotionListener(_mc.mouseMotionListener());
    _canvas.addKeyListener(_kc);
  }

  public void disable() {
    _canvas.removeMouseListener(_mc.mouseListener());
    _canvas.removeMouseMotionListener(_mc.mouseMotionListener());
    _canvas.removeKeyListener(_kc);
  }

}
