package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;

/**

 * 
 */
public class PathsCommander extends BaseCommander {
 
  public PathsCommander(SaDFrame frame, BoardCanvas canvas, Game game) {
    super(frame, canvas, game);
  }

  public Location getCurrentLocation() {
    if (_game.selectedUnit() != null) {
      return _game.selectedUnit().getLocation();
    } else {
      return null;
    }
  }

  public void choose(BoardHex hex) {
  }

  public boolean isDraggable(BoardHex hex) {
    City city = _game.cityAtLocation(hex.getLocation());
    return (city != null);
  }

}
