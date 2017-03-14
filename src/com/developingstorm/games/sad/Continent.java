package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;

/**

 * 
 */
public class Continent {

  private Set<Location> _locations;
  private int _id;
  private Board _board;
  private HashSet<Location> _coastWater;
  private ArrayList<City> _cities;

  Continent(Board b, int id) {
    _board = b;
    _id = id;
    _locations = new HashSet<Location>();
    _cities = new ArrayList<City>();
    _coastWater = null;
  }

  public void add(Location loc) {
    _locations.add(loc);
  }

  void init() {
    _coastWater = buildCoastalWaters();
    calcCities();
  }

  private HashSet<Location> getCoastalWaters(int continent) {
    return _coastWater;
  }

  public int getCityCount() {
    return _cities.size();
  }

  public void calcCities() {
    Iterator<Location> itr = _locations.iterator();
    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      City c = _board.getCity(loc);
      if (c != null) {
        _cities.add(c);
      }
    }
  }

  private HashSet<Location> buildCoastalWaters() {
    HashSet<Location> coast = new HashSet<Location>();
    Iterator<Location> itr = _locations.iterator();
    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      List<BoardHex> ring = _board.getRing(loc, 1);
      Iterator<BoardHex> itr2 = ring.iterator();
      while (itr2.hasNext()) {
        Location l2 = ((BoardHex) itr2.next()).getLocation();
        if (_board.isWater(l2)) {
          coast.add(l2);
        }
      }
    }
    return coast;
  }

  public Set<Location> getLocations() {
    return _locations;
  }

}
