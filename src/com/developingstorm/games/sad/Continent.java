package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;

/**

 * 
 */
public class Continent {



  private final Set<Location> _locations;
  private final int _id;
  private final Board _board;
  private final Set<Location> _coastWater;
  private final List<City> _cities;
  private final List<City> _coastalCities;
  private final List<City> _inlandCities;
 

  Continent(Board b, int id) {
    _board = b;
    _id = id;
    _locations = new HashSet<Location>();
    _cities = new ArrayList<City>();
    _coastWater = new HashSet<Location>();
    _coastalCities = new ArrayList<City>();
    _inlandCities = new ArrayList<City>();
  }
  
  
  public String toString() {
    return "Ct-" + _id;
  }

  public void add(Location loc) {
    _locations.add(loc);
  }

  void init() {
    calcCoastalWaters();
    calcCities();
 
  }

  public Set<Location> getCoastalWaters(int continent) {
    return _coastWater;
  }

  public int getCityCount() {
    return _cities.size();
  }

  private void calcCities() {
    for (Location loc : _locations) {
      City c = _board.getCity(loc);
      if (c != null) {
        _cities.add(c);
        if (c.isCoastal()) {
          _coastalCities.add(c);
        } else {
          _inlandCities.add(c);
        }
      }
    }
  }

  private void calcCoastalWaters() {
    for (Location loc : _locations) {
      List<BoardHex> ring = _board.getRing(loc, 1);
      for (BoardHex hex : ring) {
        Location loc2 = hex.getLocation();
        if (_board.isWater(loc2)) {
          _coastWater.add(loc2);
        }
      }
    }
  }

  public Set<Location> getLocations() {
    return _locations;
  }

  public int getID() {
    return _id;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _id;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Continent other = (Continent) obj;
    if (_id != other._id)
      return false;
    return true;
  }

  public Set<Location> getCoastalWaters() {
    return _coastWater;
    
  }

  public List<City> getCities() {
    return _cities;
  }
  
  public List<City> getOtherCities(City except) {
    List<City> cities = new ArrayList<City>(_cities);
    cities.remove(except);
    return cities;
  }

  public List<City> coastalCities() {

    return _coastalCities;
  }
  
  public List<City> inlandCities() {

    return _inlandCities;
  }

  
}
