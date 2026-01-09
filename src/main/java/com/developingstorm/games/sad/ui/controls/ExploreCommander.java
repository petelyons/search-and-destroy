package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;

/**
 * 
 */
public class ExploreCommander extends BaseCommander {
 
  private City _selectedCity;
  private Travel _selectedTravel;
  
  public ExploreCommander(SaDFrame frame, Game game) {
    super(frame, game);
  }
  
  public void setPathOrigin(City c, Travel travel) {
    _selectedCity = c;
    _selectedTravel = travel;
  }
  
  public Location getCurrentLocation() {
    if (_selectedCity != null) {
      return _selectedCity.getLocation();
    } else {
      return null;
    }
  }

  public void choose(BoardHex hex) {
  }

  public boolean isDraggable(BoardHex hex) {
    return false;
  }

  public Location autoDraggingLocation() {
    if (_selectedCity == null) {
      return null;
    }
    return _selectedCity.getLocation();
  }

  public boolean isValidDestination(BoardHex hex) {
    return true;
  }

  public void setDestination(BoardHex hex) {
    Log.error("SET DESINATION NOT DEFINED");
    
  }

  public void endExploreMode() {
    _canvas.clearArrow();
    _frame.returnGameMode();
    
  }

}
