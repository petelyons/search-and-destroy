package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.HexBoard;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;



/**
 * Encapsulate SaD board behaviors
 */
public class Board extends HexBoard {

  private HexBoardMap _map;
  private int[][] _mapData;
  private int[][] _contData;
  private HashMap<Location, City> _ctoks;
  private Game _game;
  private Set<Continent> _continents;
  private List<City> _cities;
  private boolean isInitialized = false;

  public Board(Game game, HexBoardMap grid, HexBoardContext ctx) {
    super(ctx);
    _map = grid;
    _mapData = _map.getData();
    _ctoks = new HashMap<Location, City>();
    _game = game;
    _continents = new HashSet<Continent>();
    _cities = new ArrayList<City>();

    calcContinents();
  }

  public Location randomLand() {
    int count = 0;
    while (true) {
      Location loc = _map.random();
      if (isLand(loc))
        return loc;
      count++;
      if (count > 1000)
        throw new Error("No Land on Map");
    }
  }

  public synchronized void init() {
 
    sprinkleCities((_map.getHeight() + _map.getWidth()) / 2);

    // this must follow the sprinkling of the cities
    for (Continent cont : _continents) {
      cont.init();
    }
    isInitialized = true;
  }
  
  private synchronized boolean isInitialized() {
    return isInitialized;
  }

  public List<City> getCities() {
    return _cities;
  }

  private City getCity(int x) {
    return (City) _cities.get(x);
  }

  private void sprinkleCities(int count) {
    if (_continents.size() == 0) {
      throw new SaDException("Map must have some land");
    }

    boolean[] usedConts = new boolean[_continents.size() + 1];
    int usedCount = 0;
    for (int x = 0; x < count; x++) {
      boolean again = false;
      int againCount = 0;
      Location loc;
      do {
        loc = randomLand();
        again = false;
        for (int z = 0; z < x; z++) {
          Location locz = getCity(z).getLocation();
          if (loc.isNear(locz, 2)
              && _contData[locz.x][locz.y] == _contData[loc.x][loc.y]) {
            again = true;
            againCount++;
            if (againCount == 100)
              again = false;
            break;
          }
        }

        if (again == false) {
          int continent = _contData[loc.x][loc.y];
          if (usedConts[continent] == false) {
            usedConts[continent] = true;
            usedCount++;
          } else {
            // sprinkle cities to 90% of the continents before
            // putting cities on
            // the same continent
            if (usedCount < _continents.size() - (_continents.size() / 10)) {
              again = true;
            }
          }
        }

      } while (again);

      City nc = new City(loc, _game);
      _cities.add(nc);
      _ctoks.put(loc, nc);
    }
  }

  static int wrap(int val, int max) {
    int val2 = val;
    if (val2 < 0) {
      val2 += max;
    }
    if (val2 >= max) {
      val2 -= max;
    }
    return val2;
  }

  public boolean isSame(int x, int y, Board other) {
    return (_mapData[x][y] == other._mapData[x][y]);
  }

  public boolean isCoast(Location loc) {
    if (!isLand(loc)) {
      return false;
    }

    List<BoardHex> ring = getRing(loc, 1);
    for (BoardHex hex : ring) {
      if (isWater(hex.getLocation())) {
        return true;
      }
    }
    return false;
  }

  public boolean isLand(int x, int y) {
    return isLand(Location.get(x, y));
  }

  public City getCity(Location loc) {
    return (City) _ctoks.get(loc);
  }

  public boolean isCity(Location loc) {
    return (getCity(loc) != null);
  }

  public boolean isWater(Location loc) {
    return (_mapData[loc.x][loc.y] == 0);
  }

  public boolean isLand(Location loc) {
    return (_mapData[loc.x][loc.y] == 1);
  }

  public int getTerrain(Location loc) {
    return (_mapData[loc.x][loc.y]);
  }

  public boolean isTravelable(Unit u, Location loc) {
    Type t = u.getType();
    Travel trav = t.getTravel();
    return isTravelable(trav, loc);
  }

  public void makeWater(Location loc) {
    _mapData[loc.x][loc.y] = 0;
  }

  public void makeLand(Location loc) {
    _mapData[loc.x][loc.y] = 1;
  }

  private void markContinent(Location loc, int c, Continent cont) {

    List<BoardHex> ring = getRing(loc, 1);
   
    for (BoardHex hex : ring) {
      Location l2 = hex.getLocation();
      if (l2.x < 0 || l2.y < 0) {
        continue;
      }
      if (_contData[l2.x][l2.y] == 0 && _mapData[l2.x][l2.y] > 0) {
        _contData[l2.x][l2.y] = c;
        cont.add(Location.get(l2.x, l2.y));
        markContinent(l2, c, cont); // recursive!
      }
    }
  }

  private void calcContinents() {
    int count = 0;
    _contData = new int[_map.getWidth()][_map.getHeight()];
    for (int x = 0; x < _map.getWidth(); x++) {
      for (int y = 0; y < _map.getHeight(); y++) {
        int md = _mapData[x][y];
        int contd = _contData[x][y];

        if (md > 0 && contd == 0) {
          count++;
          Continent cont = new Continent(this, count);
          _continents.add(cont);
          Location loc = Location.get(x, y);
          cont.add(loc);
          markContinent(loc, count, cont);
        }
      }
    }
  }

  public boolean isWaterPath(Path p) {
    for (Location loc : p) {
      if (isLand(loc) && !isCity(loc)) {
        return false;
      }
    }
    return true;
  }

  public boolean isLandPath(Path p) {
    for (Location loc : p) {
      if (!isLand(loc)) {
        return false;
      }
    }
    return true;
  }

  
  public Set<Continent> getContinents() {
    return _continents;
  }
  
  public Continent getContinent(Location loc) {
    if (!isInitialized()) {
      throw new SaDException("Board not ready");
    }
    int id = getContinentId(loc);
    if (id == 0) {
      return null;
    }
    return getContinentById(id);
  }

  
  public Continent getContinentById(int id) {
    for (Continent cont : _continents) {
      if (cont.getID() == id) {
        return cont;
      }
    }
    throw new SaDException("Invalid continent ID");
  }
  
  public int getContinentId(Location loc) {
    return _contData[loc.x][loc.y];
  }

  public int getContinentCount() {
    return _continents.size();
  }

   public boolean isTravelable(Travel trav, Location loc) {
    if (!onBoard(loc)) {
      return false;
    }
    if (trav == Travel.LAND) {
      return isLand(loc);
    }
    if (trav == Travel.SEA) {
      return (isWater(loc) || isCity(loc));
    }
    return true;
  }


}