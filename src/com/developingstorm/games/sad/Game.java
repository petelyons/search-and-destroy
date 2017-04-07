package com.developingstorm.games.sad;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import com.developingstorm.games.astar.AStar;
import com.developingstorm.games.astar.AStarNode;
import com.developingstorm.games.astar.AStarState;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationLens;
import com.developingstorm.games.sad.types.Armor;
import com.developingstorm.games.sad.types.Battleship;
import com.developingstorm.games.sad.types.Bomber;
import com.developingstorm.games.sad.types.Cargo;
import com.developingstorm.games.sad.types.Carrier;
import com.developingstorm.games.sad.types.Cruiser;
import com.developingstorm.games.sad.types.Destroyer;
import com.developingstorm.games.sad.types.Fighter;
import com.developingstorm.games.sad.types.Infantry;
import com.developingstorm.games.sad.types.Submarine;
import com.developingstorm.games.sad.types.Transport;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.RandomUtil;
import com.developingstorm.util.Tracer;

/**
 * Class information
 */
public class Game implements UnitLens, LocationLens {

  private volatile HexBoardMap _gridMap;

  private volatile Player[] _players;

  private volatile Player _currentPlayer;

  private volatile Board _board;

  private volatile ArrayList<Unit> _allUnits;

  private volatile GameListener _gameListener;

  private volatile int _turn;

  private volatile boolean _paused;

  private volatile Set<Unit>[][] _locations;

  private volatile HexBoardContext _ctx;
  
  private volatile Unit _selectedUnit;

  private LinkedList<Runnable> _pendingActions;

  private volatile  boolean _endPlay;

  @SuppressWarnings("unchecked")
  public Game(Player[] players, HexBoardMap grid, HexBoardContext ctx) {
    
    try {
      _ctx = ctx;
      _gameListener = null;
      _players = players;
      _allUnits = new ArrayList<Unit>();
      _pendingActions = new LinkedList<Runnable>();
      
      _turn = 0;
      _selectedUnit = null;
      _gridMap = grid;
      
      
      initGameTrace();
      
      Location.test();
  
      _paused = false;
  
      
      _board = new Board(this, _gridMap, _ctx);
  
      for (int x = 0; x < _players.length; x++) {
        if (_players[x] != null)
          _players[x].setGame(this);
      }
  
      _currentPlayer = _players[0];
  
    
      _board.init();
      assignCities();
    
      int w = _gridMap.getWidth();
      int h = _gridMap.getWidth();
      _locations = (Set<Unit>[][]) new Set[w][h];
      for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
          _locations[i][j] = Collections
              .synchronizedSet(new HashSet<Unit>());
        }
      }
    }
    catch(Exception e) {

      Log.error("Could not create new game.");
      throw e;
    }

  }

  private static void initGameTrace() {
    Date today = new Date();
    //formatting date in Java using SimpleDateFormat
    SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
    String gameDate = dateFormater.format(today);

    StringBuilder sb = new StringBuilder();
//    sb.append("SaD-");
//    sb.append(gameDate);
//    sb.append(".log");
    sb.append("SaD.log");
    
    try {
      PrintStream ps = new PrintStream(sb.toString());
      Tracer.INSTANCE.setLogStream(ps);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    Log.info("Game Date:" + gameDate);
  }

  public Path calcPath(Player player, Location from, Location to, Travel travel) {
//    Path p = calcTravelPath(player, from, to, travel, true, true);
 //   if (p == null || p.isEmpty()) {
//      p = calcTravelPath(player, from, to, travel, false, true);
//    }
    Path p = calcTravelPath(player, from, to, travel, false, true);

    return p;
  }

 
  public Path calcTravelPath(Player player, Location from, Location to,
      Travel travel, boolean checkBlocked, boolean canExplore) {

    MapState.start(this, _board, travel, player, to, checkBlocked, canExplore);

    MapState start = MapState.getUntested(from);
    MapState goal = MapState.getTerrainTested(to);
    if (start == null || goal == null) {
      if (goal == null) {
        Log.error("The path's goal is not reachable. Travel is " + travel + " loc==" + to + " " + _board.getTerrain(to) );
      }
      return null;
    }

    AStarNode s = new AStarNode(start, 1);
    AStarNode g = new AStarNode(goal, 1);
    AStar astar = new AStar(s, g, _board.getWidth(), _board.getHeight(),
        _gameListener.getWatcher());
    List<AStarState> list = astar.solve();

    Path path = null; 
    if (list != null) {
      Iterator<AStarState> itr = list.iterator();
      while (itr.hasNext()) {
        MapState state = (MapState) itr.next();
        Location loc = state.getLocation();
        if (path == null) {
          path = new Path(loc);
        } else {
          path.addLocation(loc);
        }
      }
    }
    if (path != null) {
      path.reverse();
    }
    
    Log.info(path, "calculated travel path:" + travel + " from " + from + " to " + to);
    
    if (path == null) {
      path = new Path(to);
    }
   
    return path;
  }

  public boolean isPaused() {
    return _paused;
  }

  public void setGameListener(GameListener gameListener) {
    _gameListener = gameListener;
  }

  public void trackUnit(Unit u) {
    if (_gameListener != null && u != null) {
      _gameListener.trackUnit(u);
    } else {
      Log.debug(this, "Tracking null unit");
    }
  }

 
  

  private City findUnownedCoastalCity() {
    for (City c : _board.getCities()) {
      if (_board.isCoast(c.getLocation()) && c.getOwner() == null) {
        return c;
      }
    }
    return null;
  }
  

  private void assignCities() {
    for (Player p : _players) {
      City c = findUnownedCoastalCity();
      if (c == null) {
        throw new SaDException("Not enough coastal cities!");
      }
      c.setOwner(p);
    }
  }

  public Board getBoard() {
    return _board;
  }

  public synchronized Unit createUnit(Type type, Player owner, Location location) {
    Unit u = null;
    
    if (type == Type.INFANTRY) {
      u = new Infantry(owner, location, this);
    } else if (type == Type.ARMOR) {
      u = new Armor(owner, location, this);
    } else if (type == Type.BATTLESHIP) {
      u = new Battleship(owner, location, this);
    } else if (type == Type.BOMBER) {
      u = new Bomber(owner, location, this);
    } else if (type == Type.CARGO) {
      u = new Cargo(owner, location, this);
    } else if (type == Type.CARRIER) {
      u = new Carrier(owner, location, this);
    } else if (type == Type.CRUISER) {
      u = new Cruiser(owner, location, this);
    } else if (type == Type.DESTROYER) {
      u = new Destroyer(owner, location, this);
    } else if (type == Type.FIGHTER) {
      u = new Fighter(owner, location, this);
    } else if (type == Type.SUBMARINE) {
      u = new Submarine(owner, location, this);
    } else if (type == Type.TRANSPORT) {
      u = new Transport(owner, location, this);
    } else {
      throw new SaDException("Unsupported type");
    }
    _allUnits.add(u);
    return u;
  }

  public synchronized void killUnit(Unit u, boolean showDeath) {
    Log.debug(this, "Killing Unit: " + u);
    u.kill();
    u.getOwner().removeUnit(u);
    _allUnits.remove(u);
    _gameListener.killUnit(u, showDeath);
    removeUnitFromBoard(u);
  }
  
  public List<Unit> units() {
    return _allUnits;
  }

  public void killUnit(Unit u) {
    killUnit(u, true);
  }

  public void killUnits(List<Unit> units) {

    for (Unit u : units) {
      killUnit(u, false);
    }
  }

  private synchronized boolean resolveUnitAttack(Unit atk, Unit def) {

    Type at = atk.getType();
   // Type dt = def.getType();

    // trade blows until someone dies
    while (true) {
      int attackStrength;
      // attacker hit
      if (RandomUtil.nextBoolean()) {
        attackStrength = at.getAttack();

        _gameListener.hitLocation(def.getLocation());
        if (attackStrength == 0 && def.getAttack() == 0) {
          attackStrength = 1;
        }

        if (def.life().attack(attackStrength)) {
          return true;
        }
      }

      // defender hit
      if (RandomUtil.nextBoolean()) {
        attackStrength = def.getAttack();
        _gameListener.hitLocation(atk.getLocation());

        if (attackStrength == 0 && def.getAttack() == 0) {
          attackStrength = 1;
        }
        if (atk.life().attack(attackStrength)) {
          return false;
        }
      }
    }
  }

  private synchronized boolean resolveCityAttack(Unit atk, City def) {

    if (def.getOwner() == null) {
      _gameListener.hitLocation(def.getLocation());
      return RandomUtil.nextBoolean();
    } else {
      _gameListener.hitLocation(def.getLocation());
      for (Unit defu : def.getUnits()) {

        _gameListener.hitLocation(defu.getLocation());
        if (defu.getTravel() == Travel.LAND) {
          if (resolveUnitAttack(atk, defu) == false) {
            return false;
          }
        } else {
          int k = RandomUtil.getInt(100);
          if (k >= 75) {
            killUnit(defu, false);
          }
        }
      }
      return RandomUtil.nextBoolean();
    }
  }

  public synchronized Unit unitAtLocation(Location loc) {
    if (!_board.onBoard(loc)) {
      return null;
    }
    
    if (_selectedUnit != null) {   
      if (_selectedUnit.getLocation().equals(loc)) {
       // Log.debug(_selectedUnit, "Selected as unit @ location");
        return _selectedUnit;
      }
    }

    List<Unit> list = unitsAtLocation(loc);
    if (list == null || list.isEmpty()) {
      return null;
    }
    if (list.size() == 1) {
      //Log.debug(list.get(0), "Selected unit 0");
      
      return list.get(0);
    }
    for (Unit u : list) {
      if (!u.isCarried()) {
        //Log.debug(u, "Selected non carried");
        return u;
      }
    }

    Log.error("ALL THE UNITS AT THE LOCATION CLAIM TO BE CARRIED!");
    for (Unit u : list) {
      Log.error(u, " claims to be carried");
    }

    throw new SaDException("Unit must not be carried!");
  }

  public City cityAtLocation(Location loc) {
    City c = _board.getCity(loc);
    if (c != null && c.getOwner() == _currentPlayer) {
      return c;
    }
    return null;
  }
  
  public boolean isCity(Location loc) {
    City c = _board.getCity(loc);
    return c != null;
  }

  public List<Unit> unitsAtLocation(Location loc) {
    ArrayList<Unit> newList = new ArrayList<Unit>();
    synchronized (_locations[loc.x][loc.y]) {
      for (Unit u : _locations[loc.x][loc.y]) {
        newList.add(u);
      }
    }
    return newList;
  }

  private Set<Unit> getSetofUnitsAtLocation(Location loc) {
    return _locations[loc.x][loc.y];
  }

  void changeUnitLoc(Unit u, Location loc) {
    Set<Unit> l = getSetofUnitsAtLocation(u.getLocation());
    Set<Unit> l2 = getSetofUnitsAtLocation(loc);
    l.remove(u);
    l2.add(u);
  }

  void placeUnitOnBoard(Unit u) {
    Log.info(u, "Placing unit on board");
    Set<Unit> l = getSetofUnitsAtLocation(u.getLocation());
    l.add(u);
    
    validateLocations();
  }
  
  void removeUnitFromBoard(Unit u) {
    Log.info(u, "Removing unit from board");
    Set<Unit> l = getSetofUnitsAtLocation(u.getLocation());
    l.remove(u);
    
    validateLocations();
  }

  private void validateLocations() {
    int w = _gridMap.getWidth();
    int h = _gridMap.getWidth();
    int errors = 0;
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        Set<Unit> units = _locations[x][y];
        if (units != null) {
          for (Unit u : units) {
            Location loc = u.getLocation();
            if (x != loc.x || y != loc.y) {
              Log.error(u, "Not at location " + x + "," + y);
              errors++;
            }
          }
        }
      }
      if (errors > 0) {
        throw new SaDException("Found " + errors + " units at wrong locations");
      }
    }

  }

  private static ResponseCode resolveLoad(Unit u, Unit t) {
    if (t.getOwner() == u.getOwner()) {
      if (t.canCarry(u)) {
        t.addCarried(u);
        return ResponseCode.YIELD_PASS;
      }
    }
    return null;
  }

  public ResponseCode resolveMove(Unit u, final Location dest) {
    if (u.getLocation().equals(dest)) {
      throw new SaDException("Unit already at location!");
    }
    City unownedCity = null;
    City enemyCity = null;
    City ourCity = null;
    Player mover = u.getOwner();

    if (_board.isCity(dest)) {
      City c = _board.getCity(dest);
      Player cityOwner = c.getOwner();
      if (cityOwner == mover) {
        ourCity = c;
      } else if (cityOwner != null) {
        enemyCity = c;
      } else {
        unownedCity = c;
      }
      if (ourCity != null) {
        Log.debug("Board reports destination " + dest + " is OUR city");
      }
      if (enemyCity != null) {
        Log.debug("Board reports destination " + dest + " is ENEMY city");
      }
      if (unownedCity != null) {
        Log.debug("Board reports destination " + dest + " is FREE city");
      }
    }

    if (ourCity != null) {
      u.move(dest);
      Log.debug(u, "Moved into City.");
      u.life().burnMovesButNotFuel();
      return ResponseCode.TURN_COMPLETE;
    } else if (unownedCity != null) {
      if (u.getTravel() != Travel.LAND) {
        Log.warn("Navy/Air cannot occupy new cities");
        return ResponseCode.CANCEL_ORDER;
      }

      if (resolveCityAttack(u, unownedCity)) {
        Log.info(u, "Captured city");
        killUnit(u, false);
        unownedCity.setOwner(mover);
        return ResponseCode.TURN_COMPLETE;
      } else {
        Log.info(u, "destroyed in attack");
        killUnit(u);
        return ResponseCode.DIED;
      }
    } else if (enemyCity != null) {
      if (!(u.getTravel() == Travel.LAND || u.getType() == Type.BOMBER)) {
        Log.warn("Only bombers and land troops can attack enemy city");
        return ResponseCode.CANCEL_ORDER;
      }

      Log.debug(u, "Attacking city " + enemyCity);

      if (u.getType() == Type.BOMBER) {
        if (resolveCityAttack(u, enemyCity)) {
          Log.debug(u, "Bomber attack success.");
          killUnit(u, false);
          enemyCity.bombCity();
          return ResponseCode.DIED;
        } else {
          Log.debug(u, "Attack failed - we died");
          killUnit(u);
          return ResponseCode.DIED;

        }
      } else {
        if (resolveCityAttack(u, enemyCity)) {
          Log.debug(u, "Attack completed objective acheived.");
          killUnit(u, false);
          killUnits(enemyCity.getUnits());
          enemyCity.setOwner(mover);
          return ResponseCode.DIED;
        } else {
          Log.debug(u, "Attack failed - we died");
          killUnit(u);
          return ResponseCode.DIED;
        }
      }
    }

    if (_board.isTravelable(u, dest) == false) {
      Unit blocking = unitAtLocation(dest);
      if (blocking != null) {
        ResponseCode rc = resolveLoad(u, blocking);
        if (rc != null) {
          Log.warn(u, "resolve load problem! " + rc);
          return rc;
        }
      }
      Log.debug(u, "Move - CANCEL");
      return ResponseCode.CANCEL_ORDER;
    }

    Unit blocking = unitAtLocation(dest);
    if (blocking == null || blocking.equals(u)) {
      u.move(dest);
      if (u.life().movesLeft() > 0) {
        Log.debug(u, "Moved - step complete");
        return ResponseCode.STEP_COMPLETE;
      }
      else {
        Log.debug(u, "Moved - turn complete");
        return ResponseCode.TURN_COMPLETE;
      }
    }


    ResponseCode rc = resolveLoad(u, blocking);
    if (rc != null) {
      Log.warn(u, "resolve load problem 2! " + rc + " " + blocking);
      return rc;
    }

    Player blocker = blocking.getOwner();
    if (blocker.equals(u.getOwner())) {
      if (!u.turn().isKnownObstruction(dest)) {
        u.turn().addObstruction(dest);
        return ResponseCode.YIELD_PASS;
      }
      
      if (blocking.turn().isDone()) {
        Log.debug(u, "Destination blocked by unit that has already moved. Cancelling:" + blocking);
        return ResponseCode.TURN_COMPLETE; 
      } else {
        blocker.pushPendingPlay(blocking);
        Log.debug(u, "Destination blocked by unit that has yet to move. Yielding:" + blocking);
        return ResponseCode.YIELD_PASS;
      }
    }
    while (true) {
      Log.debug(u, "Attacking " + blocking);
      boolean attackRes = resolveUnitAttack(u, blocking);
      if (attackRes) {
        Log.debug(u, "We killed them " + blocking);
        killUnit(blocking);
        u.move(dest);
        return ResponseCode.STEP_COMPLETE;
      } else {
        Log.debug(u, "We died while attacking " + blocking);
        killUnit(u);
        return ResponseCode.DIED;
      }
    }
  }

  public Player nextPlayer() {

    for (int x = 0; x < _players.length; x++) {
      if (_players[x] == _currentPlayer) {
        if (x == _players.length - 1) {
          break;
        } else {
          return _players[x + 1];
        }
      }
    }
    return _players[0];
  }

  public Player currentPlayer() {
    return _currentPlayer;
  }

  private void signalGameThread() {
    synchronized (this) {
      if (_paused) {
        _paused = false;
        notify();
      }
    }
  }
  
  
  public synchronized Unit selectedUnit() {
    if (_selectedUnit != null && _selectedUnit.isDead()) {
      _selectedUnit = null;
      return null;
    }
    return _selectedUnit;
  }

  public void resume(Unit u) {
    if (u != null) {
      selectUnit(u);
      u.getOwner().pushPendingPlay(u);
    }
    signalGameThread();
  }

  public String toString() {
    return "Game";
  }

  public void pause() {

    while (true) {
      try {
        Unit u = selectedUnit();
        if (u == null) {
          throw new SaDException("No unit is selected!");
        }
        trackUnit(u);
        synchronized (this) {
          Log.debug(u, "Waiting for order...");
          _paused = true;
          Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
              _gameListener.notifyWait();
            }});
          t.start();
          wait();
          processPostedGameActions();
          return;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }
  }
  
  public void postGameAction(Runnable runnable) {
    synchronized(_pendingActions) {
      _pendingActions.offer(runnable);
    }
  }
  
  public void postAndRunGameAction(Runnable runnable) {
    synchronized(_pendingActions) {
      _pendingActions.offer(runnable);
    }
    resume(null);
  }

  private void processPostedGameActions() {
    
    synchronized(_pendingActions) {
      while (!_pendingActions.isEmpty()) {
        Runnable r = _pendingActions.pop();
        r.run();
      }
    }
      
  }

  private void playerChange() {
    if (_gameListener != null)
      _gameListener.selectPlayer(_currentPlayer);
  }

  public void selectUnit(Unit u) {
    if (u != null)
      _selectedUnit = u;
      _gameListener.selectUnit(u);
  }
  
  
  public void play() {
    int uc;
    int cc;
   
    try {
      synchronized(this) {
        if (_endPlay) {
          return;
        }
      }
      playerChange();
      
      do {
        uc = _currentPlayer.unitCount();
        cc = _currentPlayer.cityCount();
        if (cc == 0 && uc == 0) {
         
          _gameListener.gameOver(nextPlayer());
        } else if (cc == 0) {
          if (!_currentPlayer.hasUnitsThatCaptureACity()) {
            _gameListener.gameOver(nextPlayer());
          }
        }
  
        
        if (!(uc == 0 && cc == 0)) {
          _currentPlayer.play();
        }
        _currentPlayer = nextPlayer();
        
        if (_currentPlayer == _players[0]) {
          Log.debug(this, "Starting turn: " + _turn);
          _turn++;
          _gameListener.newTurn(_turn);
          
        }
        processPostedGameActions();
        playerChange();
      } while (true);
    } catch (Throwable t) {
      t.printStackTrace();
      _gameListener.abort();
    }
  }



  public synchronized int getTurn() {
    return _turn;
  }

  /**
    *
    */
  public boolean isExplored(Location loc) {
    return true;
  }

  /**
    *
    */
  public Unit visibleUnit(Location loc) {
    return unitAtLocation(loc);
  }

  public List<Unit> unitsBorderingLocation(Location loc) {
    List<Unit> completeList = new ArrayList<Unit>();
    List<Location> ringList = loc.getRing(1);
    for (Location loc2 : ringList) {
      List<Unit> list = unitsAtLocation(loc2);
      completeList.addAll(list);
    }
    return completeList;
  }

  public void dump() {
    Log.info("=======================================================================================================");
    Log.info("GAME DUMP: " + toString());
    
    for (Player player : _players) {
      Log.info("-------------------------------------------------------------------------------------------------------");
      Log.info("PLAYER: " + player);
      Log.info("--------------------------------------------------");
      player.forEachUnit((Unit u)->{Log.info(u.toString());});
    }
    
    Log.info("------------------------------------------------------------------------------------------------------");
    Log.info(" CITIES");
    Log.info("------------------------------------------------------------------------------------------------------");
    for (City city :  _board.getCities()) {
      Log.info(city.toString());
    }
  }

  public Player getPlayer(String link) {
    for (Player p : _players) {
      if (p.toJsonLink().equals(link)) {
        return p;
      }
    }
    throw new SaDException("No player named:" + link);
  
  }

  public City getCity(String link) {
    for (City c : _board.getCities()) {
      if (c.toJsonLink().equals(link)) {
        return c;
      }
    }
    throw new SaDException("No city named:" + link);
  }

  public void end() {
    synchronized(this) {
      _endPlay = true;
    }
    
  }



}