package com.developingstorm.games.sad;

import com.developingstorm.games.astar.AStar;
import com.developingstorm.games.astar.AStarNode;
import com.developingstorm.games.astar.AStarState;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationLens;
import com.developingstorm.games.sad.commands.GameCommand;
import com.developingstorm.games.sad.events.*;
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
import java.util.concurrent.ConcurrentLinkedQueue;

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

    public volatile int turn;

    private volatile boolean paused;

    private volatile HexBoardContext ctx;

    private volatile Unit selectedUnit;

    private LinkedList<Runnable> pendingActions;

    private volatile boolean endPlay;

    // When true, the human player's first turn should pause for input
    // even if all units have orders. Set after loading a saved game.
    private volatile boolean resumingFromLoad;

    // New architecture components for UI/Game separation
    private final GameEventBus eventBus;
    private final DebugEventBus debugEventBus;
    private volatile GameState gameState;
    private final ConcurrentLinkedQueue<GameCommand> commandQueue;

    @SuppressWarnings("unchecked")
    public Game(Player[] players, HexBoardMap grid, HexBoardContext ctx) {
        try {
            this.ctx = ctx;
            this.players = players;
            this.pendingActions = new LinkedList<Runnable>();

            // Initialize new architecture components
            this.eventBus = new GameEventBus();
            this.debugEventBus = new DebugEventBus();
            this.gameState = GameState.RUNNING;
            this.commandQueue = new ConcurrentLinkedQueue<>();

            this.turn = 0;
            this.selectedUnit = null;
            this.gridMap = grid;

            // Reset all naming pools for new game
            ShipNames.reset();
            UnitNames.reset();
            UnitNames.autoAssignThemes(players.length);

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

    public void trackUnit(Unit u) {
        if (u != null) {
            // Only track units owned by the human player (players[0])
            // This prevents the viewport from jumping to AI units
            if (this.players != null && this.players.length > 0) {
                Player humanPlayer = this.players[0];

                // Check if this unit belongs to the human player
                if (u.getOwner() != humanPlayer) {
                    // This is an AI unit - don't track it
                    return;
                }
            }

            eventBus.publish(new UnitTrackedEvent(u));
        } else {
            Log.debug(this, "Tracking null unit");
        }
    }

    public void trackLocation(Location loc) {}

    public Board getBoard() {
        return board;
    }

    public CombatResolver getCombatResolver() {
        return combatResolver;
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

    public Unit getUnitById(long unitId) {
        for (Unit u : unitManager.getAllUnits()) {
            if (u.id == unitId) {
                return u;
            }
        }
        return null;
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

        // When multiple units at location, prioritize current player's non-carried units
        if (this.currentPlayer != null) {
            for (Unit u : list) {
                if (!u.isCarried() && u.getOwner() == this.currentPlayer) {
                    //Log.debug(u, "Selected current player's non-carried unit");
                    return u;
                }
            }
        }

        // Fallback to any non-carried unit
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

    /**
     * Check if a unit can be placed at a location respecting stacking rules.
     * Stacking limit is 1 unit per hex with exceptions:
     * 1. Units in a city can stack freely.
     * 2. Units on a transport stack up to carrying capacity (handled separately).
     * 3. Air units can share a hex with a friendly land or sea unit.
     */
    public boolean canPlaceUnit(Unit unit, Location loc) {
        List<Unit> existing = unitsAtLocation(loc);
        if (existing.isEmpty()) {
            return true;
        }
        // Exception 1: cities allow stacking
        if (isCity(loc)) {
            return true;
        }
        // Exception 3: air unit can share with friendly non-air unit (and vice versa)
        if (unit.getTravel() == Travel.AIR) {
            for (Unit u : existing) {
                if (
                    u.getOwner() == unit.getOwner() &&
                    u.getTravel() != Travel.AIR
                ) {
                    // Air can share with friendly land/sea — but only one air unit allowed
                    return existing.size() == 1;
                }
            }
            return false;
        }
        // Non-air unit checking if it can share with an existing air unit
        for (Unit u : existing) {
            if (
                u.getOwner() == unit.getOwner() && u.getTravel() == Travel.AIR
            ) {
                return existing.size() == 1;
            }
        }
        // Default: 1 unit per hex, hex is occupied
        return false;
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

    public ResponseCode resolveMove(
        Unit u,
        final Location dest,
        final Location finalDest
    ) {
        return movementResolver.resolveMove(u, dest, finalDest);
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

    /**
     * Get the human player (always players[0]).
     * The UI should ALWAYS use this for rendering fog of war,
     * never currentPlayer() which switches between human and AI.
     *
     * @return The human player
     */
    public Player getHumanPlayer() {
        return this.players[0];
    }

    private void signalGameThread() {
        synchronized (this) {
            if (this.paused) {
                paused = false;
                notify();
            }

            // Also update game state
            if (
                this.gameState == GameState.AWAITING_ORDERS ||
                this.gameState == GameState.PAUSED
            ) {
                transitionState(this.gameState, GameState.RUNNING);
            }
        }
    }

    /**
     * Signals the game thread to continue (public wrapper for use by edicts).
     * Call this after automatically assigning orders to units that were waiting for player input.
     */
    public void continueGame() {
        signalGameThread();
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

                    // Update to new state machine
                    transitionState(
                        GameState.RUNNING,
                        GameState.AWAITING_ORDERS
                    );

                    // Wait loop that periodically checks for commands
                    while (
                        this.gameState == GameState.AWAITING_ORDERS && paused
                    ) {
                        // Process any commands that arrived while waiting
                        processCommands();

                        // If state changed to RUNNING, break out
                        if (this.gameState == GameState.RUNNING || !paused) {
                            break;
                        }

                        // Wait for a short time or until notified
                        try {
                            wait(50); // Poll every 50ms
                        } catch (InterruptedException e) {
                            break;
                        }
                    }

                    processPostedGameActions();
                    processCommands(); // Process any remaining commands
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

    private void playerChange() {}

    public void selectUnit(Unit u) {
        if (u != null) selectedUnit = u;
        eventBus.publish(new UnitSelectedEvent(u));
    }

    public void deselectUnit() {
        selectedUnit = null;
        eventBus.publish(new UnitSelectedEvent(null));
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
                // Process any pending commands at the start of each iteration
                processCommands();

                uc = this.currentPlayer.unitCount();
                cc = this.currentPlayer.cityCount();
                if (cc == 0 && uc == 0) {
                    eventBus.publish(new GameOverEvent(nextPlayer()));
                    transitionState(this.gameState, GameState.GAME_OVER);
                    return;
                } else if (cc == 0) {
                    if (!this.currentPlayer.hasUnitsThatCaptureACity()) {
                        eventBus.publish(new GameOverEvent(nextPlayer()));
                        transitionState(this.gameState, GameState.GAME_OVER);
                        return;
                    }
                }

                if (!(uc == 0 && cc == 0)) {
                    this.currentPlayer.play();
                }
                currentPlayer = nextPlayer();

                // Initialize visibility for the new player BEFORE notifying UI
                currentPlayer.startNewTurn();

                if (currentPlayer == this.players[0]) {
                    Log.debug(this, "Starting turn: " + this.turn);
                    this.turn++;
                    eventBus.publish(new NewTurnEvent(this.turn));
                }
                processPostedGameActions();
                processCommands(); // Process commands after each player's turn
                playerChange();
            } while (true);
        } catch (Throwable t) {
            t.printStackTrace();
            eventBus.publish(new GameAbortedEvent());
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

    /**
     * Mark that the game is resuming from a saved state.
     * The human player's first turn will pause for input.
     */
    public void setResumingFromLoad() {
        this.resumingFromLoad = true;
    }

    /**
     * Check and clear the resuming-from-load flag.
     * Returns true exactly once after a load.
     */
    public boolean consumeResumingFromLoad() {
        if (this.resumingFromLoad) {
            this.resumingFromLoad = false;
            return true;
        }
        return false;
    }

    // ========== New Architecture Methods ==========

    /**
     * Get the event bus for publishing and subscribing to game events.
     */
    public GameEventBus getEventBus() {
        return eventBus;
    }

    /**
     * Get the debug event bus for high-volume debugging events like pathfinding.
     */
    public DebugEventBus getDebugEventBus() {
        return debugEventBus;
    }

    /**
     * Get the current game state.
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Set the game state (used by commands and internal logic).
     */
    public void setGameState(GameState newState) {
        this.gameState = newState;
    }

    /**
     * Transition from one game state to another with validation.
     * Logs the transition and publishes appropriate events.
     */
    public void transitionState(GameState expectedCurrent, GameState newState) {
        synchronized (this) {
            if (this.gameState != expectedCurrent) {
                Log.warn(
                    this,
                    "State transition warning: expected " +
                        expectedCurrent +
                        " but was " +
                        this.gameState +
                        ", transitioning to " +
                        newState +
                        " anyway"
                );
            }

            GameState oldState = this.gameState;
            this.gameState = newState;

            Log.debug(this, "Game state: " + oldState + " -> " + newState);

            // Publish appropriate events
            switch (newState) {
                case PAUSED:
                    eventBus.publish(
                        new AbstractGameEvent(GameEventType.GAME_PAUSED) {}
                    );
                    break;
                case RUNNING:
                    if (
                        oldState == GameState.PAUSED ||
                        oldState == GameState.AWAITING_ORDERS
                    ) {
                        eventBus.publish(
                            new AbstractGameEvent(GameEventType.GAME_RESUMED) {}
                        );
                    }
                    break;
                case GAME_OVER:
                    // GameOver event is published separately with winner info
                    break;
                case AWAITING_ORDERS:
                    String unitMsg =
                        selectedUnit != null
                            ? selectedUnit.toString()
                            : "No unit selected";
                    eventBus.publish(new WaitingForOrdersEvent(unitMsg));
                    break;
            }
        }
    }

    /**
     * Submit a command to be executed on the game thread.
     * This is the primary way for UI to interact with game state.
     * Thread-safe and non-blocking.
     */
    public void submitCommand(GameCommand command) {
        if (command == null) {
            Log.warn(this, "Attempted to submit null command");
            return;
        }
        commandQueue.offer(command);
        Log.debug(
            this,
            "Command queued: " + command.getClass().getSimpleName()
        );
    }

    /**
     * Process all pending commands from the command queue.
     * Called by the game thread during its main loop.
     */
    public void processCommands() {
        GameCommand command;
        int processedCount = 0;
        while ((command = commandQueue.poll()) != null) {
            try {
                Log.debug(
                    this,
                    "Executing command: " + command.getClass().getSimpleName()
                );
                command.execute(this);
                processedCount++;
            } catch (Exception e) {
                Log.error(
                    "Error executing command " +
                        command.getClass().getSimpleName() +
                        ": " +
                        e.getMessage()
                );
                e.printStackTrace();
            }
        }
        if (processedCount > 0) {
            Log.debug(this, "Processed " + processedCount + " commands");
        }
    }
}
