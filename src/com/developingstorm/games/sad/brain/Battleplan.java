package com.developingstorm.games.sad.brain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.UnitStats;
import com.developingstorm.util.CollectionUtil;

public class Battleplan {
  
  // We have explored the entire continent and control all the cities 
  private final Set<Continent> _secureContinents;
  
  // Colonized but either not fully explored or fully controlled
  private final Set<Continent> _unsecureContinents;
  
  // Known continents where we don't own a city
  private final Set<Continent> _targetContinents;

  private final Board _board;

  private final Robot _player;

  private final Game _game;

  private final Set<Location> _loadingPoints;

  private final Set<Continent> _battlezoneContinents;

  private final Set<Continent> _defenseContinents;

  private Set<Location> _unloadingPoints;

  private UnitStats _us;
  
  
  public Battleplan(final Game game, final Robot p) {
    
    _game = game;
    _board = game.getBoard();
    _player = p;
    
    _us = _player.getStats();
    
    Set<Continent> discovered = _player.getDiscoveredContinents();
    HashSet<Continent> colonized = (HashSet<Continent>) _player.getColonizedContinents();
    Set<Unit> enemies = _player.getKnownEnemies();
    
    _battlezoneContinents = calcBattlezones(enemies);
    _secureContinents = calcSecureContinents(colonized);
    _unsecureContinents = CollectionUtil.subtract(colonized, _secureContinents);
    _targetContinents = CollectionUtil.subtract(discovered, colonized);
    _defenseContinents = CollectionUtil.intersect(colonized, _battlezoneContinents);
    
    _loadingPoints = calcLoadingLocations();
    _unloadingPoints = calcUnloadingLocations();
    
  }

  private HashSet<Continent> calcSecureContinents(Set<Continent> colonized) {
    HashSet<Continent> set = new HashSet<Continent>();
    for (Continent cont : colonized) {
      int totalCities = cont.getCityCount();
      int ownedCities = 0;
      for (City city : _player.getCities()) {
        if (city.getContinent().equals(cont)) {
          ownedCities++;
        }
      }
      if (ownedCities == totalCities) {
        set.add(cont);
      }
    }
    return set;
  }
  
  
  /**
   * Provide the Set of continents where enemy land units have been spotted
   * @param enemies
   * @return
   */
  private Set<Continent> calcBattlezones(Set<Unit> enemies) {
    
    Set<Continent> contested = new HashSet<Continent>();
    for (Unit u : enemies) {
      if (u.getTravel().equals(Travel.LAND)) {
        contested.add(_board.getContinent(u.getLocation()));
      }
    }
    return contested;
  }
  
  
  private Set<Location> calcLoadingLocations() {
    Set<Location> loadingPoints = new HashSet<Location>();
    for (City c : _player.getCities()) {
      if (c.isCoastal() && c.getProduction().equals(Type.TRANSPORT)) {
        loadingPoints.add(c.getLocation());
      }
    }
    return loadingPoints;
  }
  
  private static Set<Location> coastline(Set<Continent> continents) {
    Set<Location> points = new HashSet<Location>();
    for (Continent con : continents) {
      points.addAll(con.getCoastalWaters());
    }
    return points;
  }
  
  private Set<Location> calcUnloadingLocations() {
    Set<Location> unloadingPoints = new HashSet<Location>();
    if (!_defenseContinents.isEmpty()) {
      return coastline(_defenseContinents);
    } else if (_targetContinents.isEmpty()) {
      return coastline(_targetContinents);
    }
    return unloadingPoints;
  }

    
  /**
   * Provide the Set of locations loading cargo and transports
   * @param enemies
   * @return
   */
  public Set<Location> getLoadingPoints() {
    
    return _loadingPoints;
  }
  
  /**
   * Provide the Set of locations for unloading transports
   * @param enemies
   * @return
   */
  public Set<Location> getUnloadingPoints() {
    
    return _unloadingPoints;
  }
  
  

  private Type percentageProductionChoice(City c) {
    if (_us.percent_infantry < 0.15)
      return Type.INFANTRY;
    if (_us.percent_fighters < 0.15)
      return Type.FIGHTER;
    if (_us.percent_armor < 0.15)
      return Type.ARMOR;

    if (c.isCoastal()) {
      if (_us.percent_transports < 0.10)
        return Type.TRANSPORT;
      if (_us.percent_destroyers < 0.075)
        return Type.DESTROYER;
      if (_us.percent_submarines < 0.075)
        return Type.SUBMARINE;
      if (_us.percent_cruisers < 0.05)
        return Type.CRUISER;
      if (_us.percent_carriers < 0.05)
        return Type.CARRIER;
      if (_us.percent_battleships < 0.05)
        return Type.BATTLESHIP;
    } else {
      if (_us.percent_bombers < 0.10)
        return Type.BOMBER;
      if (_us.percent_transports < 0.05)
        return Type.CARGO;
      if (_us.percent_armor < _us.percent_infantry)
        return Type.ARMOR;
    }
    return Type.BOMBER;
  }

  public Type productionChoice(City city) {
    
    Continent cont = city.getContinent();
    if (_defenseContinents.contains(cont)) {
      return Type.INFANTRY;
    }
    
    if (_secureContinents.contains(cont)) {
      return percentageProductionChoice(city);
    }

    return Type.INFANTRY;
  }
  
      
}
