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
  protected SaDFrame frame;
  protected BoardCanvas canvas;
  protected Game game;

  public BaseCommander(SaDFrame frame, Game game) {
    this.frame = frame;
    canvas = frame.getCanvas();
    this.game = game;
  }

  public boolean onBoard(Location loc) {
    return this.game.getBoard().onBoard(loc);
  }

  public void showLine(Location start, Location end) {
    this.canvas.setLine(start, end);
  }

  public void showLocation(Location loc) {
    this.frame.showLocation(loc);
  }
  
  public int boardWidth() {
    return this.game.getBoard().getWidth();
  }

  public int boardHeight() {
    return this.game.getBoard().getHeight();
  }

  public BoardHex trans(Point p) {
    return this.game.getBoard().get(p);
  }

  public void setFocus(BoardHex hex) {
    Log.debug(this, "Setting focus");
    this.game.getBoard().setFocus(hex);
  }

  public void refocus() {
    if (!this.canvas.hasFocus()) {
      this.canvas.requestFocus();
    }
  }
  
  public BoardHex locationToHex(Location loc) {
    return this.game.getBoard().get(loc);
  }

  public abstract Location getCurrentLocation();
  public abstract void choose(BoardHex hex);
  public abstract boolean isDraggable(BoardHex hex);

}
