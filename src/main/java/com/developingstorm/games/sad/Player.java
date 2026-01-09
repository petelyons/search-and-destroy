package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationLens;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.Graph;
import com.developingstorm.util.GraphNode;



public class Player implements UnitLens, LocationLens {

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
  private EdictFactory _edictFactory;
  

  public Player(String name, int id) {

    _id = id;
    _name = name;
    _cities = new ArrayList<City>();
    _units = new ArrayList<Unit>();
    _unownedCities = new HashSet<City>();
    _enemyCities = new HashSet<City>();
    _enemyUnits = new HashSet<Unit>();
    _pendingPlay = new LinkedList<>();
    _pendingOrders = new LinkedList<>();
    
    _edictFactory = new EdictFactory(this);
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
    Player other = (Player) obj;
    if (_id != other._id)
      return false;
    return true;
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
    return "Human: I=" + _id;
  }

  public int getId() {

    return _id;
  }
  
  
  public EdictFactory edictFactory() {
    return _edictFactory;
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
    PathFinder finder = new PathFinder(u, c.getLocation());
    return finder.getPath();
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

  public void removeUnit(Unit u) {
    _units.remove(u);
  }
  
  public List<Unit> reachableEnemies(Unit u) {

    ArrayList<Unit> list = new ArrayList<Unit>();
    ArrayList<Location> reachable = getReachable(u);
    for (Location loc : reachable) {
      
      if (isExplored(loc)) {
        Unit u2 = visibleUnit(loc);
        if (u2 != null && !u2.getOwner().equals(this)) {
          City c = _game.cityAtLocation(u2.getLocation());
          if (u.canAttackCity() || c == null) {
            list.add(u2);
          }
        }
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
    for (Location loc : reachable) {
      if (isExplored(loc)) {
        City c = _board.getCity(loc);
        if (c != null) {
          list.add(c);
        }
      }
    }
    return list;
  }
  
  public List<City> reachableCities(Location origin, Travel t, int dist) {

    ArrayList<City> list = new ArrayList<City>();
    ArrayList<Location> reachable = getReachable(origin, t, dist);
    for (Location loc : reachable) {
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
  
  public Set<Unit> getKnownEnemies() {
   
    Set<Unit> set = new HashSet<Unit>();

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
              set.add(u);
            }
          }
        }
      }
    }
    return set;
  }

  private Set<City> getCitiesBuildingArmies() {

    Set<City> set = new HashSet<City>();
    for (City c : _cities) {
      if (c.getProduction() == Type.INFANTRY) {
        set.add(c);
      }
    }
    return set;
  }

  public Set<Continent> getColonizedContinents() {
    Set<Continent> colonized = new HashSet<Continent>();
    for (City c : _cities) {
      Continent cont = c.getContinent();
      if (cont == null) {
        Log.error(c, "City not on continent!");
        throw new SaDException("Cities must be on a continent!!!");
      }
      colonized.add(c.getContinent());
    }
    return colonized;
  }

  private void calcEnemyActivity() {

    int width = _board.getWidth();
    int height = _board.getHeight();

    _enemyActivity = new int[width][height];

    for(Unit u : _enemyUnits) {
      List<Location> influence = u.getAreaOfInfluence();
      for (Location loc : influence) {
        _enemyActivity[loc.x][loc.y]++;
      }
    }
  }

  
  public Set<City> enemyCities() {
    return _enemyCities;
    
  }
  
  
  private void calcContinentsLoadingPositions() {

  
    // ArrayList _board.getCoast(
  }

 // private void calcTargetContinents() {

   // int[] ucont = getContinentsContainingCities(_unownedCities.iterator());
   // int[] ocont = getContinentsContainingCities(_cities.iterator());

    // for (int x = 0; x < continents.length; x++) {
    // chooseLoadingPosition(continents[x]);
    // }
    // ArrayList _board.getCoast(
//  }

 


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
    Vision newVis = v;
    Vision ov = _visible[loc.x][loc.y];
    if ((ov == Vision.SURFACE && v == Vision.WATER)
        || (ov == Vision.WATER && v == Vision.SURFACE) || ov == Vision.COMPLETE) {
      newVis = Vision.COMPLETE;
    }
    _visible[loc.x][loc.y] = newVis;
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
  
  public Set<Continent> getDiscoveredContinents() {
    int width = _board.getWidth();
    int height = _board.getHeight();

    Set<Continent> set = new HashSet<Continent>();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Location loc = Location.get(x, y);
        Continent cont = _board.getContinent(loc);
        if (cont != null) {
          set.add(cont);
        }
      }
    }
    return set;
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


  public void unitsNeedOrders() {
    
    if (isRobot()) {
      throw new SaDException("The override method should have been called!");
    }
   
    Unit pending = popPendingOrders();
    while (pending != null) {
      if (pending.hasOrders() || pending.turn().isDone()) {
        pending = popPendingOrders();
      } else {
        break;
      }
    }
 
    
    if (pending != null) {
      _game.selectUnit(pending);
    }
    
    pending = _game.selectedUnit();
    if (pending != null) {
      if (!pending.life().hasMoves()) {
        pending = null;
      }
    }
    
    if (pending == null) {
      List<Unit> unplayed = unplayedUnits();
      if (unplayed.isEmpty()) {
        throw new SaDException("Trying to get orders when no units are playable!");
      }
      _game.selectUnit(unplayed.get(0));
    }
    
    _game.pause();
  }
  


  @SuppressWarnings("static-method")
  public boolean isRobot() {
    return false;
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
  
  private static List<City> findReachable(City start, List<City> options, int dist) {
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

  public Game getGame() {
   return _game;
  }

   public void startNewTurn() {
    Log.debug(this, "Starting new turn");
    for (City c: _cities) {
      c.startNewTurn();
    }

    buildCityLists();
    buildEnemyUnitList();
    calcEnemyActivity();
    calcContinentsLoadingPositions();

    for (Unit u : _units) {
      u.turn().beginTurn();
    }


   UnitStats unitStats = new UnitStats(_units, _cities);
    Log.info("===========================================================================\r\nStarting new turn with:\n" + unitStats);
  }

  public void completeTurn() {
   
    @SuppressWarnings("unchecked")
    List<Unit> list = (List<Unit>) _units.clone();
    for (Unit u : list) {
      u.turn().completeTurn();
    }
   
  }
  
  public UnitStats unitStats() {
    return new UnitStats(_units, _cities);
  }
  

  static class OrderStateCounts {
    int availableMoves = 0;
    int noOrders;
  }

  private static OrderStateCounts analyseUnplayed(List<Unit> units) {
    OrderStateCounts counts = new OrderStateCounts();
    for (Unit u : units) {

      if (u.turn().isDone() == false && !u.hasOrders() && u.life().isSleeping() == false) {
        counts.noOrders++;
      }
      counts.availableMoves += u.life().movesLeft();
    }
    return counts;
  }
  
  
  public List<Unit> unplayedUnits() {
    ArrayList<Unit> units = new ArrayList<Unit>();
    
 //   forEachUnit((Unit u)->{Log.info(u, (u.life().hasMoves() ? "HAS MOVES" : "NO MOVES")); });
    
    forEachUnit((Unit u)->{if (u.life().hasMoves()) {units.add(u);} });
    
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
    
    Log.info("Found " + units.size() + " potentially playable units");
   
    return units;
  }

  public void play() {
    
    int previousMovesLeft = 0;
    
    startNewTurn();
    
    while (true) {
      List<Unit> unplayed = unplayedUnits();
    
      if (unplayed.isEmpty()) {
        Log.info(this, "Turn over");
        break;
      }
    
      Unit pending = popPendingPlay();
      // Move the unit the user interacted with
      if (pending != null && pending.hasOrders()  && unplayed.contains(pending)) {
        unplayed.remove(pending);
        pending.turn().attemptTurn();
      }
      
      for (Unit u : unplayed) {
        if (!u.hasOrders() || u.isDead()) {
          continue;
        }
        
        _game.selectUnit(u);
        u.turn().attemptTurn();
      }
      
      unplayed = unplayedUnits();
      OrderStateCounts orderStats = analyseUnplayed(unplayed);
      
      if (orderStats.availableMoves == previousMovesLeft && orderStats.noOrders == 0) {
        Log.debug("Nothing moved this pass.  Consider the turn done");
        break;
      }
 
      previousMovesLeft = orderStats.availableMoves;
      
      if ( orderStats.noOrders > 0) {
        Log.debug(this, "Needs more orders");
        unitsNeedOrders();
        previousMovesLeft = -1;
      }

      // pass++;
    }
    
    completeTurn();
      
  }

  // Store the full player info
  public Object toJson() {
    // TODO Auto-generated method stub
    return null;
  }
  
  // Store enough to discover the correct player
  public Object toJsonLink() {
    return _name;
  }
  
  public UnitStats getStats() {
    UnitStats us = new UnitStats(_units, _cities);
    return us;
  }
  
  
  public List<Unit> getUnitsOnContinent(Continent cont) {
    List<Unit> units = new ArrayList<Unit>();
    Set<Location> locations = cont.getLocations();
    for(Location loc: locations) {
     List<Unit> lu =  _game.unitsAtLocation(loc);
     units.addAll(lu);
    }
    return units;
  }
  
  public UnitStats getContinentStats(Continent cont) {
    List<City> cities = cont.getCities();
    List<Unit> units = getUnitsOnContinent(cont);
    UnitStats stats = new UnitStats(units, cities);
    return stats;
  }

  public boolean hasUnitsThatCaptureACity() {
    for (Unit u : _units) {
      if (u.getType().equals(Type.ARMOR) || u.getType().equals(Type.INFANTRY)) {
        return true;
      }
    }
    return false;
  }

}