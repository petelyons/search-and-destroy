package com.developingstorm.games.sad;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.developingstorm.games.astar.AStar;
import com.developingstorm.games.astar.AStarNode;
import com.developingstorm.games.astar.AStarState;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.RandomUtil;
import com.developingstorm.util.Tracer;

/**
 * Class information
 */
public class Game implements BoardLens {

  private volatile HexBoardMap _gridMap;

  private volatile Player[] _players;

  private volatile Player _currentPlayer;

  private volatile int _numPlayers;

  private volatile Board _board;

  private volatile ArrayList<Unit> _allUnits;

  private volatile GameListener _gameListener;

  private volatile int _turn;

  private volatile Order _currentOrder;

  private volatile boolean _waiting;

  private volatile boolean _pause;

  private volatile List<Unit>[][] _locations;

  private volatile HexBoardContext _ctx;
  
  private volatile Unit _selectedUnit;



  @SuppressWarnings("unchecked")
  public Game(Player[] players, HexBoardMap grid, HexBoardContext ctx) {
    _ctx = ctx;
    _gameListener = null;
    _players = players;
    _numPlayers = players.length;
    _allUnits = new ArrayList<Unit>();
    
    _turn = 0;
    _selectedUnit = null;
    
    
    _gridMap = grid;
    
    
    initGameTrace();
    
    Location.test();

    _pause = _waiting = false;

    
    _board = new Board(this, _gridMap, _ctx);

    for (int x = 0; x < _players.length; x++) {
      if (_players[x] != null)
        _players[x].setGame(this);
    }

    _currentPlayer = _players[0];

    initBoard();

    int w = _gridMap.getWidth();
    int h = _gridMap.getWidth();
    _locations = (List<Unit>[][]) new List[w][h];
    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        _locations[i][j] = Collections
            .synchronizedList(new ArrayList<Unit>());
      }
    }

  }

  private void initGameTrace() {
    Date today = new Date();
    //formatting date in Java using SimpleDateFormat
    SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");
    String gameDate = dateFormater.format(today);

    StringBuilder sb = new StringBuilder();
    sb.append("SaD-");
    sb.append(gameDate);
    sb.append(".log");
    
    try {
      PrintStream ps = new PrintStream(sb.toString());
      Tracer.INSTANCE.setLogStream(ps);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Path calcPath(Player player, Location from, Location to, Travel travel) {
    Path p = calcTravelPath(player, from, to, travel, true, true);
    if (p == null) {
      p = calcTravelPath(player, from, to, travel, false, true);
    }
    return p;
  }

  public Path calcAbsolutePath(Player player, Location from, Location to,
      Travel travel) {
    return calcTravelPath(player, from, to, travel, true, false);
  }

  private Path calcTravelPath(Player player, Location from, Location to,
      Travel travel, boolean checkBlocked, boolean canExplore) {

    MapState.start(this, _board, travel, player, to, checkBlocked, canExplore);

    MapState start = MapState.getUntested(from);
    MapState goal = MapState.getUntested(to);
    if (start == null || goal == null) {
      throw new SaDException("Invalid path end points");
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

  public boolean isWaiting() {
    return _waiting;
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

  private void initBoard() {
    _board.init();
    assignCities();
  }

  private void assignCities() {
    int assigned = 0;

    for (City c : _board.getCities()) {
      if (assigned == _numPlayers)
        break;

      Location loc = c.getLocation();
      if (_board.isCoast(loc)) {
        c.setOwner(_players[assigned]);
        assigned++;
      }
    }
  }

  public Board getBoard() {
    return _board;
  }

  public Unit createUnit(Type produces, Player owner, Location location) {
    Unit u = new Unit(produces, owner, location, this);
    _allUnits.add(u);
    return u;
  }

  public void killUnit(Unit u, boolean showDeath) {
    Log.debug(this, "Killing Unit: " + u);
    u.kill();
    _allUnits.remove(u);
    _gameListener.killUnit(u, showDeath);
    List<Unit> list = modifiableUnitsAtLocation(u.getLocation());
    list.remove(u);

  }

  public void killUnit(Unit u) {
    killUnit(u, true);
  }

  public void killUnits(List<Unit> units) {

    for (Unit u : units) {
      killUnit(u, false);
    }
  }

  private boolean resolveUnitAttack(Unit atk, Unit def) {

    Type at = atk.getType();
    Type dt = def.getType();

    // trade blows until someone dies
    while (true) {
      int attack;
      // attacker hit
      if (RandomUtil.nextBoolean()) {
        attack = at.getAttack();

        _gameListener.hitLocation(def.getLocation());
        if (attack == 0 && def.getAttack() == 0) {
          attack = 1;
        }

        if (def.hit(attack)) {
          return true;
        }
      }

      // defender hit
      if (RandomUtil.nextBoolean()) {
        attack = def.getAttack();
        _gameListener.hitLocation(atk.getLocation());

        if (attack == 0 && def.getAttack() == 0) {
          attack = 1;
        }
        if (atk.hit(attack)) {
          return false;
        }
      }
    }
  }

  private boolean resolveCityAttack(Unit atk, City def) {

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

  public Unit unitAtLocation(Location loc) {
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

  private List<Unit> modifiableUnitsAtLocation(Location loc) {
    return _locations[loc.x][loc.y];
  }

  void changeUnitLoc(Unit u, Location loc) {
    List<Unit> l = modifiableUnitsAtLocation(u.getLocation());
    List<Unit> l2 = modifiableUnitsAtLocation(loc);
    l.remove(u);
    l2.add(u);
  }

  void initUnit(Unit u) {
    List<Unit> l = modifiableUnitsAtLocation(u.getLocation());
    l.add(u);
  }

  private ResponseCode resolveLoad(Unit u, Unit t) {
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
      return ResponseCode.CANCEL_ORDER;
    }

    Unit blocking = unitAtLocation(dest);
    if (blocking == null || blocking.equals(u)) {
      u.move(dest);
      if (u.movesLeft() > 0) {
        return ResponseCode.STEP_COMPLETE;
      }
      else {
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
      if (blocking.turn().isDone()) {
        Log.debug(u, "Destination blocked by unit that has already moved. Cancelling:" + blocking);
        return ResponseCode.CANCEL_ORDER;
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


  public void issueOrders(Unit u, OrderType ot, Location dest, Unit target) {
    issueOrders(u, Order.factory(this, u, ot, dest, target));
  }


  public void issueOrders(Unit u, Order order) {

    Log.info(u, "Being Issuing order: " + order);
    if (u.getOwner() != _currentPlayer) {
      throw new SaDException("Unit not owned but being issued orders!");
    }
    u.assignOrder(order);
  }

  private void signalGameThread() {
    synchronized (this) {
      if (_waiting) {
        _waiting = false;
        notify();
      }
    }
  }
  
  
  public synchronized Unit selectedUnit() {
    return _selectedUnit;
  }

  public void endWait(Unit u) {
    if (u != null) {
      u.getOwner().pushPendingPlay(u);
    }
    signalGameThread();
  }

  public String toString() {
    return "Game";
  }

  public void continuePlay() {
    _pause = false;
    signalGameThread();
  }

  public void pausePlay() {
    _pause = false;
  }

  public void waitUser() {

    while (true) {
      try {
        Unit u = selectedUnit();
        trackUnit(u);
        synchronized (this) {
          Log.debug(u, "Waiting for order...");
          _waiting = true;
          _gameListener.notifyWait();
          wait();
          return;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return;
    }
  }

  private void playerChange() {
    if (_gameListener != null)
      _gameListener.selectPlayer(_currentPlayer);
  }

  void unitChange(Unit u) {
    if (u != null)
      Log.debug(u, "SELECTED");
      _selectedUnit = u;
      _gameListener.selectUnit(u);
  }

  public void play() {
    int uc;
    int cc;
    Player p;

    try {
      playerChange();
      
      //DEBUG
      Path debugPath = calcTravelPath(_currentPlayer, Location.get(23,41), Location.get(18, 42), Travel.AIR, false, false);
      Log.debug(debugPath, "Found this PATH");
      //DEBUG

      do {
        p = _currentPlayer;
        uc = p.unitCount();
        cc = p.cityCount();
        if (uc == 0 && cc == 0) {
          ;
        } else {
          GameTurn turn = new GameTurn(this, p, _turn);
          turn.play();
        }
        _currentPlayer = nextPlayer();
        if (_currentPlayer == _players[0]) {
          Log.debug(this, "Starting turn: " + _turn);
          _turn++;
          _gameListener.newTurn(_turn);
          
        }
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



}