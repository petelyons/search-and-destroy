package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;

/**
 * 
 */
public class PathsCommander extends BaseCommander {
 
  private City _selectedCity;
  private Travel _selectedTravel;
  
  public PathsCommander(SaDFrame frame, BoardCanvas canvas, Game game) {
    super(frame, canvas, game);
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
    City city = _game.cityAtLocation(hex.getLocation());
    if (city == null) {
      return false;
    }
    if (_selectedTravel.equals(Travel.SEA)) {
      return city.isCoastal();
    }
    
    if (_selectedTravel.equals(Travel.LAND)) {
      return city.shareContinent(_selectedCity);
    }
    
    
    return true;
  }

  public void setDestination(BoardHex hex) {
    City city = _game.cityAtLocation(hex.getLocation());
    if (_selectedTravel.equals(Travel.AIR)) {
      _selectedCity.getGovernor().setAirPathDest(city);
      
    } else if (_selectedTravel.equals(Travel.SEA)) {
      _selectedCity.getGovernor().setSeaPathDest(city);
    } else if (_selectedTravel.equals(Travel.LAND)) {
      _selectedCity.getGovernor().setLandPathDest(city);
      
    }
   
    
  }

  public void endPathsMode() {
    _canvas.clearArrow();
    _frame.endPathsMode();
    
  }

}
