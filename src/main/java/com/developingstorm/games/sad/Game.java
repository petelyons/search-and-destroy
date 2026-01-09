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

    private volatile HexBoardMap gridMap;

    private volatile Player[] players;

    public volatile Player currentPlayer;

    private volatile Board board;

    private final UnitManager unitManager;
    private final CombatResolver combatResolver;
    private final CityManager cityManager;
    private final MovementResolver movementResolver;

    private volatile GameListener gameListener;

    public volatile int turn;

    private volatile boolean paused;

    private volatile HexBoardContext ctx;

    private volatile Unit selectedUnit;

    private LinkedList<Runnable> pendingActions;

    private volatile boolean endPlay;

    @SuppressWarnings("unchecked")
    public Game(Player[] players, HexBoardMap grid, HexBoardContext ctx) {
        try {
            this.ctx = ctx;
            this.gameListener = null;
            this.players = players;
            this.pendingActions = new LinkedList<Runnable>();

            this.turn = 0;
            this.selectedUnit = null;
            this.gridMap = grid;

            // Initialize unit manager
            unitManager = new UnitManager(this, grid);

            // Initialize combat resolver
            combatResolver = new CombatResolver(this, unitManager);

            initGameTrace();

            Location.test();

            paused = false;

            board = new Board(this, this.gridMap, this.ctx);

            for (int x = 0; x < this.players.length; x++) {
                if (this.players[x] != null) this.players[x].setGame(this);
            }

            currentPlayer = this.players[0];

            this.board.init();

            // Initialize city manager after board is initialized
            cityManager = new CityManager(
                this,
                this.board,
                combatResolver,
                unitManager
            );

            // Initialize movement resolver after all other managers
            movementResolver = new MovementResolver(
                this,
                this.board,
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
            this.board,
            this.gameListener,
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
            this.board,
            this.gameListener,
            player,
            from,
            to,
            travel,
            checkBlocked,
            canExplore
        );
    }

    public boolean isPaused() {
        return paused;
    }

    public void setGameListener(GameListener gameListener) {
        this.gameListener = gameListener;
    }

    GameListener getGameListener() {
        return gameListener;
    }

    public void trackUnit(Unit u) {
        if (this.gameListener != null && u != null) {
            this.gameListener.trackUnit(u);
        } else {
            Log.debug(this, "Tracking null unit");
        }
    }

    public Board getBoard() {
        return board;
    }

    public Player[] getPlayers() {
        return players;
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
        if (!this.board.onBoard(loc)) {
            return null;
        }

        if (this.selectedUnit != null) {
            if (this.selectedUnit.getLocation().equals(loc)) {
                // Log.debug(this.selectedUnit, "Selected as unit @ location");
                return selectedUnit;
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
        return cityManager.cityAtLocation(loc, this.currentPlayer);
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
        for (int x = 0; x < this.players.length; x++) {
            if (this.players[x] == this.currentPlayer) {
                if (x == this.players.length - 1) {
                    break;
                } else {
                    return this.players[x + 1];
                }
            }
        }
        return this.players[0];
    }

    public Player currentPlayer() {
        return currentPlayer;
    }

    private void signalGameThread() {
        synchronized (this) {
            if (this.paused) {
                paused = false;
                notify();
            }
        }
    }

    public synchronized Unit selectedUnit() {
        if (this.selectedUnit != null && this.selectedUnit.isDead()) {
            selectedUnit = null;
            return null;
        }
        return selectedUnit;
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
                    paused = true;
                    Thread t = new Thread(() -> this.gameListener.notifyWait());
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
        synchronized (this.pendingActions) {
            this.pendingActions.offer(runnable);
        }
    }

    public void postAndRunGameAction(Runnable runnable) {
        synchronized (this.pendingActions) {
            this.pendingActions.offer(runnable);
        }
        resume(null);
    }

    private void processPostedGameActions() {
        synchronized (this.pendingActions) {
            while (!this.pendingActions.isEmpty()) {
                Runnable r = this.pendingActions.pop();
                r.run();
            }
        }
    }

    private void playerChange() {
        if (this.gameListener != null) this.gameListener.selectPlayer(
            this.currentPlayer
        );
    }

    public void selectUnit(Unit u) {
        if (u != null) selectedUnit = u;
        this.gameListener.selectUnit(u);
    }

    public void play() {
        int uc;
        int cc;

        try {
            synchronized (this) {
                if (this.endPlay) {
                    return;
                }
            }
            playerChange();

            do {
                uc = this.currentPlayer.unitCount();
                cc = this.currentPlayer.cityCount();
                if (cc == 0 && uc == 0) {
                    this.gameListener.gameOver(nextPlayer());
                    return;
                } else if (cc == 0) {
                    if (!this.currentPlayer.hasUnitsThatCaptureACity()) {
                        this.gameListener.gameOver(nextPlayer());
                        return;
                    }
                }

                if (!(uc == 0 && cc == 0)) {
                    this.currentPlayer.play();
                }
                currentPlayer = nextPlayer();

                if (currentPlayer == this.players[0]) {
                    Log.debug(this, "Starting turn: " + this.turn);
                    this.turn++;
                    this.gameListener.newTurn(this.turn);
                }
                processPostedGameActions();
                playerChange();
            } while (true);
        } catch (Throwable t) {
            t.printStackTrace();
            this.gameListener.abort();
        }
    }

    public synchronized int getTurn() {
        return turn;
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

        for (Player player : this.players) {
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
        for (City city : this.board.getCities()) {
            Log.info(city.toString());
        }
    }

    public Player getPlayer(String link) {
        for (Player p : this.players) {
            if (p.toJsonLink().equals(link)) {
                return p;
            }
        }
        throw new SaDException("No player named:" + link);
    }

    public City getCity(String link) {
        for (City c : this.board.getCities()) {
            if (c.toJsonLink().equals(link)) {
                return c;
            }
        }
        throw new SaDException("No city named:" + link);
    }

    public void end() {
        synchronized (this) {
            endPlay = true;
        }
    }
}
