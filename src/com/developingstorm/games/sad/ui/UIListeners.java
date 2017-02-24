package com.developingstorm.games.sad.ui;

/**

 * 
 */
public class UIListeners {

  private BoardCanvas _canvas;
  private MouseControls _mc;
  private KeyboardControls _kc;

  public UIListeners(BoardCanvas c, MouseControls m, KeyboardControls k) {
    _canvas = c;
    _mc = m;
    _kc = k;
  }

  public void enable() {
    _canvas.addMouseListener(_mc);
    _canvas.addMouseMotionListener(_mc);
    _canvas.addKeyListener(_kc);
  }

  public void disable() {
    _canvas.removeMouseListener(_mc);
    _canvas.removeMouseMotionListener(_mc);
    _canvas.removeKeyListener(_kc);
  }

}
