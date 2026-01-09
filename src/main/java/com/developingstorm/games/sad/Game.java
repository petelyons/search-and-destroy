package com.developingstorm.games.sad;

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

/**
 * Class information
 */
public class Game implements UnitLens, LocationLens {

    private volatile HexBoardMap _gridMap;

    private volatile Player[] _players;

    private volatile Player _currentPlayer;

    private volatile Board _board;

    private final UnitManager unitManager;
    private final CombatResolver combatResolver;
    private final CityManager cityManager;
    private final MovementResolver movementResolver;

    private volatile GameListener _gameListener;

    private volatile int _turn;

    private volatile boolean _paused;

    private volatile HexBoardContext _ctx;

    private volatile Unit _selectedUnit;

    private LinkedList<Runnable> _pendingActions;

    private volatile boolean _endPlay;

    @SuppressWarnings("unchecked")
    public Game(Player[] players, HexBoardMap grid, HexBoardContext ctx) {
        try {
            _ctx = ctx;
            _gameListener = null;
            _players = players;
            _pendingActions = new LinkedList<Runnable>();

            _turn = 0;
            _selectedUnit = null;
            _gridMap = grid;

            // Initialize unit manager
            unitManager = new UnitManager(this, grid);

            // Initialize combat resolver
            combatResolver = new CombatResolver(this, unitManager);

            initGameTrace();

            Location.test();

            _paused = false;

            _board = new Board(this, _gridMap, _ctx);

            for (int x = 0; x < _players.length; x++) {
                if (_players[x] != null) _players[x].setGame(this);
            }

            _currentPlayer = _players[0];

            _board.init();

            // Initialize city manager after board is initialized
            cityManager = new CityManager(
                this,
                _board,
                combatResolver,
                unitManager
            );

            // Initialize movement resolver after all other managers
            movementResolver = new MovementResolver(
                this,
                _board,
                cityManager,
                combatResolver,
                unitManager
            );

            cityManager.assignCities();
        } catch (Exception e) {
            Log.error("Could not create new game.");
            throw e;
        }
    }

    private static void initGameTrace() {
        Date today = new Date();
        //formatting date in Java using SimpleDateFormat
        SimpleDateFormat dateFormater = new SimpleDateFormat(
            "yyyy-MM-dd-kk-mm-ss"
        );
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

    public Path calcPath(
        Player player,
        Location from,
        Location to,
        Travel travel
    ) {
        return PathCalculator.calcPath(
            this,
            _board,
            _gameListener,
            player,
            from,
            to,
            travel
        );
    }

    public Path calcTravelPath(
        Player player,
        Location from,
        Location to,
        Travel travel,
        boolean checkBlocked,
        boolean canExplore
    ) {
        return PathCalculator.calcTravelPath(
            this,
            _board,
            _gameListener,
            player,
            from,
            to,
            travel,
            checkBlocked,
            canExplore
        );
    }

    public boolean isPaused() {
        return _paused;
    }

    public void setGameListener(GameListener gameListener) {
        _gameListener = gameListener;
    }

    GameListener getGameListener() {
        return _gameListener;
    }

    public void trackUnit(Unit u) {
        if (_gameListener != null && u != null) {
            _gameListener.trackUnit(u);
        } else {
            Log.debug(this, "Tracking null unit");
        }
    }

    public Board getBoard() {
        return _board;
    }

    Player[] getPlayers() {
        return _players;
    }

    public synchronized Unit createUnit(
        Type type,
        Player owner,
        Location location
    ) {
        return unitManager.createUnit(type, owner, location);
    }

    public synchronized void killUnit(Unit u, boolean showDeath) {
        unitManager.killUnit(u, showDeath);
    }

    public List<Unit> units() {
        return unitManager.getAllUnits();
    }

    public void killUnit(Unit u) {
        unitManager.killUnit(u);
    }

    public void killUnits(List<Unit> units) {
        unitManager.killUnits(units);
    }

    private synchronized boolean resolveUnitAttack(Unit atk, Unit def) {
        return combatResolver.resolveUnitAttack(atk, def);
    }

    private synchronized boolean resolveCityAttack(Unit atk, City def) {
        return combatResolver.resolveCityAttack(atk, def);
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
        return cityManager.cityAtLocation(loc, _currentPlayer);
    }

    public boolean isCity(Location loc) {
        return cityManager.isCity(loc);
    }

    public List<Unit> unitsAtLocation(Location loc) {
        ArrayList<Unit> newList = new ArrayList<Unit>();
        Set<Unit> unitsAtLoc = unitManager.getSetofUnitsAtLocation(loc);
        synchronized (unitsAtLoc) {
            for (Unit u : unitsAtLoc) {
                newList.add(u);
            }
        }
        return newList;
    }

    private Set<Unit> getSetofUnitsAtLocation(Location loc) {
        return unitManager.getSetofUnitsAtLocation(loc);
    }

    void changeUnitLoc(Unit u, Location loc) {
        unitManager.changeUnitLocation(u, loc);
    }

    void placeUnitOnBoard(Unit u) {
        unitManager.placeUnitOnBoard(u);
    }

    void removeUnitFromBoard(Unit u) {
        unitManager.removeUnitFromBoard(u);
    }

    public ResponseCode resolveMove(Unit u, final Location dest) {
        return movementResolver.resolveMove(u, dest);
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
                    Thread t = new Thread(() -> _gameListener.notifyWait());
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
        synchronized (_pendingActions) {
            _pendingActions.offer(runnable);
        }
    }

    public void postAndRunGameAction(Runnable runnable) {
        synchronized (_pendingActions) {
            _pendingActions.offer(runnable);
        }
        resume(null);
    }

    private void processPostedGameActions() {
        synchronized (_pendingActions) {
            while (!_pendingActions.isEmpty()) {
                Runnable r = _pendingActions.pop();
                r.run();
            }
        }
    }

    private void playerChange() {
        if (_gameListener != null) _gameListener.selectPlayer(_currentPlayer);
    }

    public void selectUnit(Unit u) {
        if (u != null) _selectedUnit = u;
        _gameListener.selectUnit(u);
    }

    public void play() {
        int uc;
        int cc;

        try {
            synchronized (this) {
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
                    return;
                } else if (cc == 0) {
                    if (!_currentPlayer.hasUnitsThatCaptureACity()) {
                        _gameListener.gameOver(nextPlayer());
                        return;
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
    @Override
    public boolean isExplored(Location loc) {
        return true;
    }

    /**
     *
     */
    @Override
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
        Log.info(
            "======================================================================================================="
        );
        Log.info("GAME DUMP: " + toString());

        for (Player player : _players) {
            Log.info(
                "-------------------------------------------------------------------------------------------------------"
            );
            Log.info("PLAYER: " + player);
            Log.info("--------------------------------------------------");
            player.forEachUnit((Unit u) -> {
                Log.info(u.toString());
            });
        }

        Log.info(
            "------------------------------------------------------------------------------------------------------"
        );
        Log.info(" CITIES");
        Log.info(
            "------------------------------------------------------------------------------------------------------"
        );
        for (City city : _board.getCities()) {
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
        synchronized (this) {
            _endPlay = true;
        }
    }
}
