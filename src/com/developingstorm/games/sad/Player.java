package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.Graph;
import com.developingstorm.util.GraphNode;



public class Player implements BoardLens {

  protected int _id;
  protected Board _board;
  protected ArrayList<City> _cities;
  protected ArrayList<Unit> _units;
  protected Game _game;
  protected HashSet<City> _unownedCities;
  protected HashSet<City> _enemyCities;
  protected HashSet<Unit> _enemyUnits;
  protected Vision[][] _visible;
  protected boolean[][] _explored;
  protected int _enemyActivity[][];
  protected String _name;
  private volatile LinkedList<Unit> _pendingPlay;
  private volatile LinkedList<Unit> _pendingOrders;
  
  
  protected UnitStats _unitStats;

  public Player(String name, int id) {

    _id = id;
    _name = name;
    _cities = new ArrayList<City>();
    _units = new ArrayList<Unit>();
    _unownedCities = new HashSet<City>();
    _enemyCities = new HashSet<City>();
    _enemyUnits = new HashSet<Unit>();
    _unitStats = new UnitStats();
    _pendingPlay = new LinkedList<>();
    _pendingOrders = new LinkedList<>();
  }
  
  public Unit popPendingPlay() {
    if (_pendingPlay.isEmpty()) {
      return null;
    }
    return _pendingPlay.pop();
  }

  public void pushPendingPlay(Unit u) {
    _pendingPlay.push(u);
  }
  
  public Unit popPendingOrders() {
    if (_pendingOrders.isEmpty()) {
      return null;
    }
    return _pendingOrders.pop();
  }

  public void pushPendingOrders(Unit u) {
    _pendingOrders.push(u);
  }

  public String toString() {
    return "Human: N=" + _name + " I=" + _id;
  }

  public int getId() {

    return _id;
  }

  public void setGame(Game g) {

    _game = g;
    _board = _game.getBoard();

    int width = _board.getWidth();
    int height = _board.getHeight();

    _explored = new boolean[width][height];
    _visible = new Vision[width][height];
  }

  public Board getBoard() {

    return _board;
  }

  public void loseCity(City c) {

    _cities.remove(c);
  }

  public void captureCity(City c) {

    adjustVisibility(c);
    _cities.add(c);
    initProduction(c);
  }

  public boolean ownsCity(City c) {

    return _cities.contains(c);
  }

  protected void initProduction(City c) {

    Type t = Type.INFANTRY;
    Log.debug(c, "Setting production to: " + t);
    c.produce(t);
    if (c.getOwner() != this) {
      throw new SaDException("City not owned by us");
    }

  }

  private Path pathToCity(Unit u, City c) {

    Location start;
    Location end;

    start = u.getLocation();
    end = c.getLocation();

    return _game.calcAbsolutePath(this, start, end, u.getTravel());
  }

  public City getClosestHome(Unit u) {

    Travel travel = u.getTravel();

    List<City> list;
    if (travel == Travel.SEA) {
      list = getCoastalCities();
    } else {
      list = _cities;
    }

    Path shortest = null;
    City choosen = null;
    Iterator<City> itr = list.iterator();
    while (itr.hasNext()) {
      City c = (City) itr.next();
      Path p = pathToCity(u, c);
      if (p != null) {
        if (shortest == null || p.length() < shortest.length()) {
          shortest = p;
          choosen = c;
        }
      }
    }

    return choosen;
  }

  public void addUnit(Unit u) {

    _units.add(u);
  }

  public void killUnit(Unit u) {

    _units.remove(u);
  }
  
  public List<Unit> reachableEnemies(Unit u) {

    ArrayList<Unit> list = new ArrayList<Unit>();
    ArrayList<Location> reachable = getReachable(u);
    Iterator<Location> itr = reachable.iterator();
    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      if (isExplored(loc)) {
        Unit u2 = visibleUnit(loc);
        if (u2 != null && u2.getOwner() != this)
          list.add(u2);
      }
    }
    return list;
  }

  public List<City> getCities() {
    return _cities;
  }

  public List<City> getCoastalCities() {
    List<City> list = new ArrayList<City>();
    for (City c : _cities) {
      if (c.isCoastal()) {
        list.add(c);
      }
    }
    return list;
  }

  public List<City> reachableCities(Unit u) {

    ArrayList<City> list = new ArrayList<City>();
    ArrayList<Location> reachable = getReachable(u);
    Iterator<Location> itr = reachable.iterator();
    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      if (isExplored(loc)) {
        City c = _board.getCity(loc);
        list.add(c);
      }
    }
    return list;
  }
  
  public List<City> reachableCities(Location origin, Travel t, int dist) {

    ArrayList<City> list = new ArrayList<City>();
    ArrayList<Location> reachable = getReachable(origin, t, dist);
    Iterator<Location> itr = reachable.iterator();
    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      if (isExplored(loc)) {
        City c = _board.getCity(loc);
        list.add(c);
      }
    }
    return list;
  }

  public int unitCount() {

    return _units.size();
  }

  public int cityCount() {

    return _cities.size();
  }

  public boolean isTurnDone() {

    for(Unit u : _units) {
      if (u.turn().isDone() == false) {
        return false;
      }
    }
    return true;
  }


  private void buildCityLists() {

    _unownedCities.clear();
    _enemyCities.clear();

    int width = _board.getWidth();
    int height = _board.getHeight();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Location loc = Location.get(x, y);
        if (isExplored(loc)) {
          City c = _board.getCity(loc);
          if (c != null) {
            Player p = c.getOwner();
            if (p == null) {
              _unownedCities.add(c);
            } else if (p != this) {
              _enemyCities.add(c);
            }
          }
        }
      }
    }
  }

  private void buildEnemyUnitList() {

    _enemyUnits.clear();

    int width = _board.getWidth();
    int height = _board.getHeight();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Location loc = Location.get(x, y);
        if (isExplored(loc)) {
          Unit u = visibleUnit(loc);
          if (u != null) {
            Player p = u.getOwner();
            if (p != this) {
              _enemyUnits.add(u);
            }
          }
        }
      }
    }
  }

  private HashSet<City> getCitiesBuildingArmies() {

    HashSet<City> set = new HashSet<City>();
    Iterator<City> itr = _cities.iterator();
    while (itr.hasNext()) {
      City c = (City) itr.next();
      if (c.getProduction() == Type.INFANTRY) {
        set.add(c);
      }
    }
    return set;
  }

  private int[] getContinentsContainingCities(Iterator<City> cities) {

    int cc = _board.getContinentCount();
    int[] continents = new int[cc + 1];
    Iterator<City> itr = cities;
    while (itr.hasNext()) {
      City c = (City) itr.next();
      int id = c.getContinent();
      if (id > cc) {
        throw new SaDException(
            "Continent id exceeds specified continent count:" + id + ">" + cc);
      }
      continents[id]++;
    }
    return continents;
  }

  private void calcEnemyActivity() {

    int width = _board.getWidth();
    int height = _board.getHeight();

    _enemyActivity = new int[width][height];

    Iterator<Unit> itr = _enemyUnits.iterator();
    while (itr.hasNext()) {
      Unit u = (Unit) itr.next();
      ArrayList<?> influence = u.getAreaOfInfluence();
      Iterator<?> i2 = influence.iterator();
      while (i2.hasNext()) {
        Location loc = (Location) i2.next();
        _enemyActivity[loc.x][loc.y]++;
      }
    }
  }

  private void calcContinentsLoadingPositions() {

    HashSet<City> cities = getCitiesBuildingArmies();
    int[] continents = getContinentsContainingCities(cities.iterator());

    for (int x = 0; x < continents.length; x++) {
      // chooseLoadingPosition(continents[x]);
    }
    // ArrayList _board.getCoast(
  }

  private void calcTargetContinents() {

    int[] ucont = getContinentsContainingCities(_unownedCities.iterator());
    int[] ocont = getContinentsContainingCities(_cities.iterator());

    // for (int x = 0; x < continents.length; x++) {
    // chooseLoadingPosition(continents[x]);
    // }
    // ArrayList _board.getCoast(
  }

  protected void startTurnPass(long turn, TurnState state) {
    Log.debug(this, "Starting new turn pass");
    for (City c: _cities) {
      c.startTurnPass(state);
    }

    buildCityLists();
    buildEnemyUnitList();
    calcEnemyActivity();
    calcContinentsLoadingPositions();

    for (Unit u : _units) {
      u.startTurnPass(state);
    }

    if (state == TurnState.START) {
      _unitStats.recalc(_units, _cities);
      Log.info("===========================================================================\r\nStarting new turn with:\n" + _unitStats);
    }
    else {
      Log.info("---------------------------------------------------------------------------\r\n");
    }
 }

  public Path getTravelPath(Travel travel, Location from, Location to) {

    if (travel == Travel.SEA) {
      return getWaterPath(from, to);
    } else if (travel == Travel.LAND) {
      return getLandPath(from, to);
    } else {
      return getAirPath(from, to);
    }
  }

  public Path getAirPath(Location from, Location to) {

    return calcPath(from, to, Travel.AIR);
  }

  public Path getWaterPath(Location from, Location to) {

    if (isExplored(to) && !(_board.isCity(to) || _board.isWater(to))) {
      return null;
    }
    return calcPath(from, to, Travel.SEA);
  }

  public Path getLandPath(Location from, Location to) {

    if (isExplored(to) && !_board.isLand(to)) {
      return null;
    }
    return calcPath(from, to, Travel.LAND);
  }

  private Path calcPath(Location from, Location to, Travel travel) {

    return _game.calcPath(this, from, to, travel);
  }

  public static Path getDirectPath(Location from, Location to) {

    Path path = new Path(from);

    int x0 = from.x;
    int x1 = to.x;
    int y0 = from.y;
    int y1 = to.y;

    int dy = y1 - y0;
    int dx = x1 - x0;
    int stepx, stepy;

    if (dy < 0) {
      dy = -dy;
      stepy = -1;
    } else {
      stepy = 1;
    }
    if (dx < 0) {
      dx = -dx;
      stepx = -1;
    } else {
      stepx = 1;
    }
    dy <<= 1;
    dx <<= 1;

    if (dx > dy) {
      int fraction = dy - (dx >> 1);
      while (x0 != x1) {
        if (fraction >= 0) {
          y0 += stepy;
          fraction -= dx;
        }
        x0 += stepx;
        fraction += dy;

        Location loc = Location.get(x0, y0);
        path.addLocation(loc);

      }
    } else {
      int fraction = dx - (dy >> 1);
      while (y0 != y1) {
        if (fraction >= 0) {
          x0 += stepx;
          fraction -= dy;
        }
        y0 += stepy;
        fraction += dx;

        Location loc = Location.get(x0, y0);
        path.addLocation(loc);
      }
    }

    return path;
  }

  static final byte NOTTESTED = 0;
  static final byte REACHABLE = 1;
  static final byte UNREACHABLE = 2;

  private void expandReachable(Unit u, int max, Location orig, Location loc,
      byte[][] reachable) {

    List<BoardHex> ring = _board.getRing(loc, 1);
    for (BoardHex hex : ring) {
      Location l2 = hex.getLocation();
      if (reachable[l2.x][l2.y] == NOTTESTED) {
        boolean b = _board.isTravelable(u, l2);
        int dist = l2.distance(orig);
        if (max > 0 && b == true) {
          b = (dist <= max);
        }
        if (b == true) {
          reachable[l2.x][l2.y] = REACHABLE;
          expandReachable(u, max, orig, l2, reachable);
        } else {
          reachable[l2.x][l2.y] = UNREACHABLE;
        }
      }
    }
  }
  
  
  private void expandReachable(Travel t, int max, Location orig, Location loc,
      byte[][] reachable) {

    List<BoardHex> ring = _board.getRing(loc, 1);
    for (BoardHex hex : ring) {
      Location l2 = hex.getLocation();
      if (reachable[l2.x][l2.y] == NOTTESTED) {
        boolean b = _board.isTravelable(t, l2);
        int dist = l2.distance(orig);
        if (max > 0 && b == true) {
          b = (dist <= max);
        }
        if (b == true) {
          reachable[l2.x][l2.y] = REACHABLE;
          expandReachable(t, max, orig, l2, reachable);
        } else {
          reachable[l2.x][l2.y] = UNREACHABLE;
        }
      }
    }
  }


  public ArrayList<Location> getReachable(Unit unit) {

    ArrayList<Location> dest = new ArrayList<Location>();

    // if (u.getTravel() == Travel.LAND) {
    // return _board.getContinent(u.getLocation());
    // }

    byte[][] reachable = new byte[_board.getWidth()][_board.getHeight()];
    Location loc = unit.getLocation();
    reachable[loc.x][loc.y] = REACHABLE;
    // Only look out as far as the unit can travel or
    int max = unit.getMaxTravel();
    if (max <= 0) {
      max = 20; // we don't want to send units half-way round the world
    } 
    expandReachable(unit, max, loc, loc, reachable);

    for (int x = 0; x < _board.getWidth(); x++) {
      for (int y = 0; y < _board.getHeight(); y++) {
        if (reachable[x][y] == REACHABLE) {
          dest.add(Location.get(x, y));
        }
      }
    }
    return dest;
  }
  
  
  public ArrayList<Location> getReachable(Location loc, Travel trav, int dist) {

    ArrayList<Location> dest = new ArrayList<Location>();

    byte[][] reachable = new byte[_board.getWidth()][_board.getHeight()];
    reachable[loc.x][loc.y] = REACHABLE;
    // Only look out as far as the unit can travel or
    int max = dist;
    expandReachable(trav, max, loc, loc, reachable);

    for (int x = 0; x < _board.getWidth(); x++) {
      for (int y = 0; y < _board.getHeight(); y++) {
        if (reachable[x][y] == REACHABLE) {
          dest.add(Location.get(x, y));
        }
      }
    }
    return dest;
  }


  public boolean isFrontier(Location loc) {

    if (isExplored(loc)) {
      List<?> ring = _board.getRing(loc, 1);
      Iterator<?> itr = ring.iterator();
      while (itr.hasNext()) {
        BoardHex hex = (BoardHex) itr.next();
        Location l2 = hex.getLocation();
        if (!isExplored(l2))
          return true;
      }
    }
    return false;
  }

  public ArrayList<Location> getFrontier(Unit u) {

    ArrayList<Location> list = getReachable(u);

    if (Debug.getDebugExplore()) {
      Debug.setDebugLocations(list);
    }

    ArrayList<Location> dest = new ArrayList<Location>();
    Iterator<Location> itr = list.iterator();
    while (itr.hasNext()) {
      Location loc = (Location) itr.next();
      if (isFrontier(loc)) {
        dest.add(loc);
      }
    }
    return dest;
  }
  
  
  public ArrayList<Location> getFrontier(Location loc, Travel t, int dist) {

    ArrayList<Location> list = getReachable(loc, t, dist);

    if (Debug.getDebugExplore()) {
      Debug.setDebugLocations(list);
    }

    ArrayList<Location> dest = new ArrayList<Location>();
    Iterator<Location> itr = list.iterator();
    while (itr.hasNext()) {
      Location loc2 = (Location) itr.next();
      if (isFrontier(loc2)) {
        dest.add(loc2);
      }
    }
    return dest;
  }
  
  public void forEachUnit(Consumer<Unit> consumer) {
    _units.forEach(consumer);
  }

  private void markVisible(Location loc, Vision v) {

    Vision ov = _visible[loc.x][loc.y];
    if ((ov == Vision.SURFACE && v == Vision.WATER)
        || (ov == Vision.WATER && v == Vision.SURFACE) || ov == Vision.COMPLETE) {
      v = Vision.COMPLETE;
    }
    _visible[loc.x][loc.y] = v;
  }

  private void markExplored(Location loc) {

    _explored[loc.x][loc.y] = true;
  }

  private void markRegion(Location p, Vision v, int dist) {

    for (int x = p.x - dist; x <= p.x + dist; x++) {
      for (int y = p.y - dist; y <= p.y + dist; y++) {
        Location loc = Location.get(x, y);
        if (loc != null) {
          markVisible(loc, v);
          markExplored(loc);
        }
      }
    }
  }

  private void clearVis() {

    int width = _board.getWidth();
    int height = _board.getHeight();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        _visible[x][y] = Vision.NONE;
      }
    }
  }

  public void adjustVisibility(Unit u) {

    Location loc = u.getLocation();
    Type t = u.getType();
    Vision v = t.getVision();
    int dist = t.getVisionDistance();
    markRegion(loc, v, dist);
  }

  public void adjustVisibility(City c) {

    Location loc = c.getLocation();
    int dist = 3;
    markRegion(loc, Vision.COMPLETE, dist);
  }

  public void calcVisibility(Iterator<?> unitItr) {

    clearVis();
    while (unitItr.hasNext()) {
      Unit u = (Unit) unitItr.next();
      adjustVisibility(u);
    }
  }

  public Vision getVisibility(Location loc) {

    return _visible[loc.x][loc.y];
  }

  public boolean isExplored(Location loc) {

    return _explored[loc.x][loc.y];
  }

  public Unit visibleUnit(Location loc) {

    if (_game == null) {
      throw new RuntimeException("null game");
    }

    Unit u = _game.unitAtLocation(loc);
    if (u == null)
      return null;
    if (u.getOwner() == this)
      return u;
    else if (u.isVisible(getVisibility(loc))) {
      return u;
    }
    return null;
  }


  protected void unitsNeedOrders() {
    
    if (isRobot()) {
      throw new SaDException("The override method should have been called!");
    }
    
    
   
    Unit pending = popPendingOrders();
    while (pending != null) {
      if (pending.hasOrders()) {
        pending = popPendingOrders();
      } else {
        break;
      }
    }
 
    if (pending == null) {
      for (Unit u : _units) {
        if (!u.hasOrders()) {
          pending = u;
          break;
        }
      }
    }
      
    
    if (pending != null) {
      _game.unitChange(pending);
    }
    
    if (_game.selectedUnit() == null) {
      throw new SaDException("Turn needs units with orders but all units appear to already have them");
    }
    
    _game.waitUser();
  }
  


  public boolean isRobot() {
    return false;
  }

  
  public List<Unit> unplayedUnits() {
    ArrayList<Unit> units = new ArrayList<Unit>();
    
    for (Unit u : _units) {
      if (!u.turn().isDone()) {
        units.add(u);
      }
    }
    
    Unit selected = _game.selectedUnit();
    if (selected != null) {
      Location loc = selected.getLocation();
      List<Unit> atLoc = _game.unitsAtLocation(loc);
      for (Unit u : atLoc) {
        if (units.contains(u)) {
          units.remove(u);
          units.add(0, u);
        }
      }
    }
   
    return units;
  }
  
  /**
   * Finds cities within n spaces of the frontier
   * @param dist
   * @return
   */
  public List<City> frontierCities(int dist) {
    
    List<City> list = new ArrayList<City>();
    for (City c : _cities) {
      ArrayList<Location> locations = getFrontier(c.getLocation(), Travel.AIR, dist);
      if (!locations.isEmpty()) {
        list.add(c);
      }
    }
    return list;
  }
  
  
  
  
 
  
  private List<City> findReachable(City start, List<City> options, int dist) {
    List<City> near = new ArrayList<City>();
    for (City c : options) {
      if (c.isNear(start, dist)) {
        near.add(c);
      }
    }
    return near;
  }
  
  public class HopState {
    
  }
  
  
  
 
  
  public int findRoute(Graph<City, HopState> graph, Queue<GraphNode<City, HopState>> queue, GraphNode<City, HopState> dest) {
    
    GraphNode<City, HopState> start = queue.peek();
    Set<GraphNode<City, HopState>> relatives = start.relatives();
    
    for (GraphNode<City, HopState> gn : relatives) {
      if (gn.equals(dest)) {
        queue.add(dest);
        
        int dist = start.getContent().getLocation().distance(dest.getContent().getLocation());
        return dist;
      }
    }
    
    Queue<GraphNode<City, HopState>> shortestQueue = null;
    int shortestRoute = Integer.MAX_VALUE;
    for (GraphNode<City, HopState> gn : relatives) {
      if (queue.contains(gn)) {
        continue;
      }
      Queue<GraphNode<City, HopState>> queue2 = new LinkedList<GraphNode<City, HopState>>();
      queue2.add(gn);
      int rdist = findRoute(graph, queue2, dest);
      if (rdist == -1) {
        continue;
      }
      if (rdist < shortestRoute) {
        shortestQueue = queue2;
        shortestRoute = rdist;
      }
    }
    
    if (shortestQueue == null) {
      return -1;
    }
    else {
      for (GraphNode<City, HopState> qe : shortestQueue) {
        queue.add(qe);
      }
      return shortestRoute;
    }
  }
  

  public City findHopCity(City city, int dist){
  
    Graph<City, HopState> graph = new Graph<City, HopState>() {
      int _dist = dist;
      
      @Override
      protected List<City> findRelatives(City start) {
        return findReachable(start, _cities, _dist);
      }
    };
    
    GraphNode<City, HopState> startCityNode = graph.buildGraphNodes(city);
       
    List<City> frontier = frontierCities(dist);
    List<City> reachableFrontier = new ArrayList<City>();
    for (City c : frontier) {
      if (graph.containsKey(c)) {
        reachableFrontier.add(c);
      }
    }
    
    if (reachableFrontier.isEmpty()) {
      return null;
    }
    
    Queue<GraphNode<City, HopState>> queue = new LinkedList<GraphNode<City, HopState>>();
    int shortest = Integer.MAX_VALUE;
    City nextHop = null;
    for (City c : reachableFrontier) {
      if (graph.containsKey(c)) {
        queue.clear();
        queue.add(startCityNode);
        int route = findRoute(graph, queue, graph.get(c));
        if (route < shortest) {
          nextHop = queue.peek().getContent();
        }
      }
    }
    return nextHop;
  }
}