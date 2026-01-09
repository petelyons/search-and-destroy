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
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.UnitStats;
import com.developingstorm.util.CollectionUtil;

public class Battleplan {
  
  // We have explored the entire continent and control all the cities 
  private final Set<Continent> _secureContinents;
  
  // Known continents where we don't own a city
  private final Set<Continent> _targetContinents;

  private final Board _board;

  private final Robot _player;

  private final Game _game;

  private final Set<Location> _loadingPoints;

  private final Set<Continent> _battlezoneContinents;

  private final Set<Continent> _defenseContinents;

  private Set<Location> _defenseUnloadingPoints;

  private UnitStats _us;

  private Set<Location> _expandUnloadingPoints;
  
  
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
    _targetContinents = CollectionUtil.subtract(discovered, colonized);
    _defenseContinents = CollectionUtil.intersect(colonized, _battlezoneContinents);
    
    _loadingPoints = calcLoadingLocations();
    _defenseUnloadingPoints = calcDefenseUnloadingLocations();
    _expandUnloadingPoints = calcExpandUnloadingLocations();
    
  }
  
  private static final String CRLF = "\r\n";
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(CRLF);
    sb.append("BATTLEPLAN:");
    sb.append(CRLF);
    sb.append("----------------------------------------");
    sb.append(CRLF);
    sb.append("Secure continents:");
    listContinents(sb, _secureContinents);  
    sb.append("Target continents:");
    listContinents(sb, _targetContinents);  
    sb.append("Defense continents:");
    listContinents(sb, _defenseContinents);  
    sb.append("BZone continents:");
    listContinents(sb, _battlezoneContinents);  
    sb.append("Loading Points");
    listLocations(sb, _loadingPoints);
    sb.append("Defense Unloading Points");
    listLocations(sb, _defenseUnloadingPoints);
    sb.append("Expland Unloading Points");
    listLocations(sb, _expandUnloadingPoints);
    return sb.toString();
  }

  private void listContinents(StringBuilder sb, Set<Continent> continents) {
    sb.append(CRLF);
    int counter = 0;
    if (continents != null) {
      for (Continent cont : continents) {
        if (counter > 0) {
          sb.append(", ");
        }
        sb.append(cont);
        counter++;
      }
    }
    sb.append(CRLF);

  }

  private void listLocations(StringBuilder sb, Set<Location> locations) {
    sb.append(CRLF);
    int counter = 0;
    if (locations != null) {
      for (Location loc : locations) {
        if (counter > 0) {
          sb.append(", ");
        }
        sb.append(loc);
        counter++;
      }
    }
    sb.append(CRLF);

  }
  
  
  private HashSet<Continent> calcSecureContinents(Set<Continent> colonized) {
    HashSet<Continent> set = new HashSet<Continent>();
    for (Continent cont : colonized) {
      if (cont == null) {
        throw new SaDException("Null values not allowed in continent sets");
      }
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
        Location loc = c.getLocation();
        loadingPoints.add(loc);
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
  
  private Set<Location> calcDefenseUnloadingLocations() {
    Set<Location> unloadingPoints = new HashSet<Location>();
    if (!_defenseContinents.isEmpty()) {
      return coastline(_defenseContinents);
    } 
    return unloadingPoints;
  }
  
  private Set<Location> calcExpandUnloadingLocations() {
    Set<Location> unloadingPoints = new HashSet<Location>();
    if (!_targetContinents.isEmpty()) {
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
  public Set<Location> getDefenseUnloadingPoints() {
    
    return _defenseUnloadingPoints;
  }
  
  
  public Set<Location> getExpandUnloadingPoints() {
    
    return _expandUnloadingPoints;
  }
  
  

  private Type supplyBasedProductionChoice(City c) {
    _us.recalc();
    if (c.isCoastal()) {
      return coastalProductionChoice(c);
    } else {
      return inlandProductionChoice(c);
    }
  }


  private Type inlandProductionChoice(City c) {
   
    Continent cont = c.getContinent();
    List<City> coastal = cont.coastalCities();
    UnitStats stats = c.getContinentStats();
  
    Type currrentProduction = c.getProduction();
    stats.decrementProduction(currrentProduction);

    int infantry = stats.getProduction(Type.INFANTRY);
    int armor = stats.getProduction(Type.ARMOR);
    int bomber = stats.getProduction(Type.BOMBER);
    int fighter = stats.getProduction(Type.FIGHTER);

    
    if (infantry + armor > bomber + fighter) {
      if (bomber > fighter) {
        return Type.FIGHTER;
      } else {
        return Type.BOMBER;
      }
      
    } else {
      if (infantry > armor) {
        return Type.INFANTRY;
      } else {
        return Type.ARMOR;
      }      
    }
    
  }

  private Type coastalProductionChoice(City c) {

    UnitStats stats = c.getContinentStats();
    Type currrentProduction = c.getProduction();
    stats.decrementProduction(currrentProduction);

    int infantry = stats.getProduction(Type.INFANTRY);
    int armor = stats.getProduction(Type.ARMOR);
    int transports = stats.getProduction(Type.TRANSPORT);

    if (_us.getCount(Type.INFANTRY) < 6) {
      return Type.INFANTRY;
    }
    
    if (_us.getCount(Type.TRANSPORT) == 0) {
      return Type.TRANSPORT;
    }
    
    if (infantry + armor > 0 && transports == 0) {
      return Type.TRANSPORT;
    }
    
    Type t = percentageCoastalChoice(c);
    _us.incrementProduction(t);
    return t;
  }

  private Type percentageCoastalChoice(City c) {
    if (_us.getPercentage(Type.DESTROYER) < 0.075)
      return Type.DESTROYER;
    if (_us.getPercentage(Type.SUBMARINE) < 0.075)
      return Type.SUBMARINE;
    if (_us.getPercentage(Type.CRUISER) < 0.05)
      return Type.CRUISER;
    if (_us.getPercentage(Type.CARRIER) < 0.05)
      return Type.CARRIER;
    if (_us.getPercentage(Type.BATTLESHIP) < 0.05)
      return Type.BATTLESHIP;
    return Type.SUBMARINE;
  }

  public Type productionChoice(City city) {
    
    Continent cont = city.getContinent();
    if (_secureContinents.contains(cont)) {
      Type t = supplyBasedProductionChoice(city);
      return t;
    }

    return Type.INFANTRY;
  }
  
  
  Game getGame() {
    return _game;
  }
  
  Board getBoard() {
    return _board;
  }
  
  Robot getPlayer() {
    return _player;
  }
      
}
