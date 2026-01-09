package com.developingstorm.games.sad.ui.controls;

import java.awt.Point;
import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;

/**
 * BaseCommander is a common foundation class for objects that act as glue between the UI and the game model. 
 */
public abstract class BaseCommander {
  protected SaDFrame _frame;
  protected BoardCanvas _canvas;
  protected Game _game;

  public BaseCommander(SaDFrame frame, Game game) {
    _frame = frame;
    _canvas = frame.getCanvas();
    _game = game;
  }

  public boolean onBoard(Location loc) {
    return _game.getBoard().onBoard(loc);
  }

  public void showLine(Location start, Location end) {
    _canvas.setLine(start, end);
  }

  public void showLocation(Location loc) {
    _frame.showLocation(loc);
  }
  
  public int boardWidth() {
    return _game.getBoard().getWidth();
  }

  public int boardHeight() {
    return _game.getBoard().getHeight();
  }

  public BoardHex trans(Point p) {
    return _game.getBoard().get(p);
  }

  public void setFocus(BoardHex hex) {
    Log.debug(this, "Setting focus");
    _game.getBoard().setFocus(hex);
  }

  public void refocus() {
    if (!_canvas.hasFocus()) {
      _canvas.requestFocus();
    }
  }
  
  public BoardHex locationToHex(Location loc) {
    return _game.getBoard().get(loc);
  }

  public abstract Location getCurrentLocation();
  public abstract void choose(BoardHex hex);
  public abstract boolean isDraggable(BoardHex hex);

}
