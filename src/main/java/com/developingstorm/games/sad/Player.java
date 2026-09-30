package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationLens;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.Graph;
import com.developingstorm.util.GraphNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class Player implements UnitLens, LocationLens {

    public int id;
    protected Board board;
    protected ArrayList<City> cities;
    protected ArrayList<Unit> units;
    protected Game game;
    protected HashSet<City> unownedCities;
    protected HashSet<City> enemyCities;
    protected HashSet<Unit> enemyUnits;
    protected Vision[][] visible;
    protected boolean[][] explored;
    protected int enemyActivity[][];
    protected ContactMap contactMap;
    protected String name;

    // Thread-safe queues using lock-free concurrent collections
    private final ConcurrentLinkedQueue<Unit> pendingPlay;
    private final ConcurrentLinkedQueue<Unit> pendingOrders;

    private EdictFactory edictFactory;

    // Flag to signal the game loop to end the current turn immediately
    private volatile boolean endTurnRequested;

    public Player(String name, int id) {
        this.id = id;
        this.name = name;
        this.cities = new ArrayList<City>();
        this.units = new ArrayList<Unit>();
        this.unownedCities = new HashSet<City>();
        this.enemyCities = new HashSet<City>();
        this.enemyUnits = new HashSet<Unit>();

        // Use thread-safe concurrent queues
        this.pendingPlay = new ConcurrentLinkedQueue<>();
        this.pendingOrders = new ConcurrentLinkedQueue<>();
        this.contactMap = new ContactMap();

        this.edictFactory = new EdictFactory(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Player)) return false;
        Player other = (Player) obj;
        return this.id == other.id;
    }

    /**
     * Remove and return the next unit from pending play queue.
     * Thread-safe using lock-free concurrent queue.
     */
    public Unit popPendingPlay() {
        return this.pendingPlay.poll();
    }

    /**
     * Add a unit to the pending play queue.
     * Thread-safe using lock-free concurrent queue.
     */
    public void pushPendingPlay(Unit u) {
        if (u != null && !u.isDead()) {
            this.pendingPlay.offer(u);
        }
    }

    /**
     * Remove and return the next unit from pending orders queue.
     * Thread-safe using lock-free concurrent queue.
     */
    public Unit popPendingOrders() {
        return this.pendingOrders.poll();
    }

    /**
     * Add a unit to the pending orders queue.
     * Thread-safe using lock-free concurrent queue.
     */
    public void pushPendingOrders(Unit u) {
        Log.info(this, "Pushing unit to pending orders: " + u);
        if (u != null && !u.isDead()) {
            this.pendingOrders.offer(u);
        }
    }

    /**
     * Request that the current turn end immediately, skipping all remaining units.
     */
    public void requestEndTurn() {
        this.endTurnRequested = true;
    }

    public String toString() {
        return "Human: I=" + id;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public EdictFactory edictFactory() {
        return edictFactory;
    }

    public void setGame(Game g) {
        this.game = g;
        this.board = this.game.getBoard();

        int width = this.board.getWidth();
        int height = this.board.getHeight();

        this.explored = new boolean[width][height];
        this.visible = new Vision[width][height];

        // Initialize all visibility to NONE
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.visible[x][y] = Vision.NONE;
            }
        }
    }

    public Board getBoard() {
        return this.board;
    }

    public void loseCity(City c) {
        this.cities.remove(c);
    }

    public void captureCity(City c) {
        adjustVisibility(c);
        this.cities.add(c);
        initProduction(c);
    }

    public boolean ownsCity(City c) {
        return this.cities.contains(c);
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
            list = cities;
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
        this.units.add(u);
    }

    public void removeUnit(Unit u) {
        this.units.remove(u);
        // Also remove from pending queues to prevent dead units from being played
        this.pendingPlay.remove(u);
        this.pendingOrders.remove(u);
    }

    public List<Unit> reachableEnemies(Unit u) {
        ArrayList<Unit> list = new ArrayList<Unit>();
        ArrayList<Location> reachable = getReachable(u);
        for (Location loc : reachable) {
            if (isExplored(loc)) {
                Unit u2 = visibleUnit(loc);
                if (u2 != null && !u2.getOwner().equals(this)) {
                    City c = this.game.cityAtLocation(u2.getLocation());
                    if (u.canAttackCity() || c == null) {
                        list.add(u2);
                    }
                }
            }
        }
        return list;
    }

    public List<City> getCities() {
        return cities;
    }

    public List<Unit> getUnits() {
        return units;
    }

    public List<City> getCoastalCities() {
        List<City> list = new ArrayList<City>();
        for (City c : this.cities) {
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
                City c = this.board.getCity(loc);
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
                City c = this.board.getCity(loc);
                list.add(c);
            }
        }
        return list;
    }

    public int unitCount() {
        return this.units.size();
    }

    public int cityCount() {
        return this.cities.size();
    }

    public boolean isTurnDone() {
        for (Unit u : this.units) {
            if (u.turn().isDone() == false) {
                return false;
            }
        }
        return true;
    }

    private void buildCityLists() {
        this.unownedCities.clear();
        this.enemyCities.clear();

        int width = this.board.getWidth();
        int height = this.board.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Location loc = Location.get(x, y);
                if (isExplored(loc)) {
                    City c = this.board.getCity(loc);
                    if (c != null) {
                        Player p = c.getOwner();
                        if (p == null) {
                            this.unownedCities.add(c);
                        } else if (p != this) {
                            this.enemyCities.add(c);
                        }
                    }
                }
            }
        }
    }

    public Set<Unit> getKnownEnemies() {
        Set<Unit> set = new HashSet<Unit>();

        int width = this.board.getWidth();
        int height = this.board.getHeight();

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

    /**
     * Two-ring detection pass. Populates {@code contactMap} with every enemy
     * unit that any owned unit or city can detect, marking it identified if
     * within that spotter's identification range (or auto-identifying
     * submarines under WATER/COMPLETE vision). Also refreshes/decays ghosts
     * and re-populates the legacy {@code enemyUnits} set from current
     * contacts only (for backwards compatibility with existing callers).
     */
    void scanForContacts() {
        this.enemyUnits.clear();
        int currentTurn = this.game.getTurn();

        for (Player other : this.game.getPlayers()) {
            if (other == this) continue;
            for (Unit enemy : other.getUnits()) {
                if (enemy.isDead()) continue;
                Location eLoc = enemy.getLocation();

                boolean detectedThisTurn = false;
                boolean bestIdentified = false;
                int bestSpotterQuality = 0;

                // Owned units as spotters
                for (Unit u : this.units) {
                    Type ut = u.getType();
                    int dist = u.getLocation().distance(eLoc);
                    if (dist > ut.getDetectionRange()) continue;
                    if (!enemy.isVisible(ut.getVision())) continue;

                    detectedThisTurn = true;
                    boolean identified = dist <= ut.getIdentificationRange();
                    // Submarines under WATER or COMPLETE vision are auto-ID'd
                    if (
                        enemy.getType() == Type.SUBMARINE &&
                        (ut.getVision() == Vision.WATER ||
                            ut.getVision() == Vision.COMPLETE)
                    ) {
                        identified = true;
                    }
                    if (identified) bestIdentified = true;
                    if (ut.getSpotterQuality() > bestSpotterQuality) {
                        bestSpotterQuality = ut.getSpotterQuality();
                    }
                }

                // Owned cities as spotters (detectionRange=3, idRange=3,
                // quality=2, COMPLETE vision). Short-circuit: if a unit spotter
                // already produced an identified sighting at max quality, a
                // city can't upgrade it. Relies on the invariant that
                // bestIdentified is only set after detectedThisTurn=true in
                // the unit loop above.
                if (!bestIdentified || bestSpotterQuality < 2) {
                    for (City c : this.cities) {
                        int dist = c.getLocation().distance(eLoc);
                        if (dist > 3) continue;
                        // Cities have COMPLETE vision — everything is visible
                        detectedThisTurn = true;
                        bestIdentified = true;
                        if (bestSpotterQuality < 2) bestSpotterQuality = 2;
                    }
                }

                if (detectedThisTurn) {
                    this.enemyUnits.add(enemy);
                    this.contactMap.updateContact(
                        enemy.id,
                        eLoc,
                        enemy.getType(),
                        enemy.getTravel(),
                        other,
                        bestIdentified,
                        bestSpotterQuality,
                        currentTurn
                    );
                }
            }
        }

        // Decay stale contacts; drop dead units
        this.contactMap.refreshContacts(
            currentTurn,
            unitId -> {
                Unit u = this.game.getUnitById(unitId);
                return u == null || u.isDead();
            }
        );
    }

    private Set<City> getCitiesBuildingArmies() {
        Set<City> set = new HashSet<City>();
        for (City c : this.cities) {
            if (c.getProduction() == Type.INFANTRY) {
                set.add(c);
            }
        }
        return set;
    }

    public Set<Continent> getColonizedContinents() {
        Set<Continent> colonized = new HashSet<Continent>();
        for (City c : this.cities) {
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
        int width = this.board.getWidth();
        int height = this.board.getHeight();

        enemyActivity = new int[width][height];

        for (Unit u : this.enemyUnits) {
            List<Location> influence = u.getAreaOfInfluence();
            for (Location loc : influence) {
                this.enemyActivity[loc.x][loc.y]++;
            }
        }
    }

    public Set<City> enemyCities() {
        return enemyCities;
    }

    private void calcContinentsLoadingPositions() {
        // ArrayList this.board.getCoast(
    }

    // private void calcTargetContinents() {

    // int[] ucont = getContinentsContainingCities(this.unownedCities.iterator());
    // int[] ocont = getContinentsContainingCities(this.cities.iterator());

    // for (int x = 0; x < continents.length; x++) {
    // chooseLoadingPosition(continents[x]);
    // }
    // ArrayList this.board.getCoast(
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

    private void expandReachable(
        Unit u,
        int max,
        Location orig,
        Location loc,
        byte[][] reachable
    ) {
        List<BoardHex> ring = this.board.getRing(loc, 1);
        for (BoardHex hex : ring) {
            Location l2 = hex.getLocation();
            if (reachable[l2.x][l2.y] == NOTTESTED) {
                boolean b = this.board.isTravelable(u, l2);
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

    private void expandReachable(
        Travel t,
        int max,
        Location orig,
        Location loc,
        byte[][] reachable
    ) {
        List<BoardHex> ring = this.board.getRing(loc, 1);
        for (BoardHex hex : ring) {
            Location l2 = hex.getLocation();
            if (reachable[l2.x][l2.y] == NOTTESTED) {
                boolean b = this.board.isTravelable(t, l2);
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
        // return this.board.getContinent(u.getLocation());
        // }

        byte[][] reachable =
            new byte[this.board.getWidth()][this.board.getHeight()];
        Location loc = unit.getLocation();
        reachable[loc.x][loc.y] = REACHABLE;
        // Only look out as far as the unit can travel or
        int max = unit.getMaxTravel();
        if (max <= 0) {
            max = 20; // we don't want to send units half-way round the world
        }
        expandReachable(unit, max, loc, loc, reachable);

        for (int x = 0; x < this.board.getWidth(); x++) {
            for (int y = 0; y < this.board.getHeight(); y++) {
                if (reachable[x][y] == REACHABLE) {
                    dest.add(Location.get(x, y));
                }
            }
        }
        return dest;
    }

    public ArrayList<Location> getReachable(
        Location loc,
        Travel trav,
        int dist
    ) {
        ArrayList<Location> dest = new ArrayList<Location>();

        byte[][] reachable =
            new byte[this.board.getWidth()][this.board.getHeight()];
        reachable[loc.x][loc.y] = REACHABLE;
        // Only look out as far as the unit can travel or
        int max = dist;
        expandReachable(trav, max, loc, loc, reachable);

        for (int x = 0; x < this.board.getWidth(); x++) {
            for (int y = 0; y < this.board.getHeight(); y++) {
                if (reachable[x][y] == REACHABLE) {
                    dest.add(Location.get(x, y));
                }
            }
        }
        return dest;
    }

    public boolean isFrontier(Location loc) {
        if (isExplored(loc)) {
            List<?> ring = this.board.getRing(loc, 1);
            Iterator<?> itr = ring.iterator();
            while (itr.hasNext()) {
                BoardHex hex = (BoardHex) itr.next();
                Location l2 = hex.getLocation();
                if (!isExplored(l2)) return true;
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
        this.units.forEach(consumer);
    }

    /**
     * Clears all current visibility (but preserves explored areas).
     * Should be called at the start of each turn before recalculating visibility.
     * Also called when loading a saved game to ensure clean visibility state.
     */
    public void clearVisibility() {
        for (int x = 0; x < this.visible.length; x++) {
            for (int y = 0; y < this.visible[x].length; y++) {
                this.visible[x][y] = Vision.NONE;
            }
        }
    }

    private void markVisible(Location loc, Vision v) {
        Vision newVis = v;
        Vision ov = this.visible[loc.x][loc.y];
        if (
            (ov == Vision.SURFACE && v == Vision.WATER) ||
            (ov == Vision.WATER && v == Vision.SURFACE) ||
            ov == Vision.COMPLETE
        ) {
            newVis = Vision.COMPLETE;
        }
        this.visible[loc.x][loc.y] = newVis;
    }

    private void markExplored(Location loc) {
        this.explored[loc.x][loc.y] = true;
    }

    /**
     * Directly marks a location as explored (used for save/load).
     * Public to allow GameStateSerializer to restore explored state.
     */
    public void markExploredDirect(Location loc) {
        this.explored[loc.x][loc.y] = true;
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
        int width = this.board.getWidth();
        int height = this.board.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.visible[x][y] = Vision.NONE;
            }
        }
    }

    public Set<Continent> getDiscoveredContinents() {
        int width = this.board.getWidth();
        int height = this.board.getHeight();

        Set<Continent> set = new HashSet<Continent>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Location loc = Location.get(x, y);
                Continent cont = this.board.getContinent(loc);
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
        Vision v = this.visible[loc.x][loc.y];
        return v != null ? v : Vision.NONE;
    }

    public boolean isExplored(Location loc) {
        return this.explored[loc.x][loc.y];
    }

    public Unit visibleUnit(Location loc) {
        if (game == null) {
            throw new RuntimeException("null game");
        }

        Unit u = this.game.unitAtLocation(loc);
        if (u == null) return null;
        if (u.getOwner() == this) return u;
        else if (u.isVisible(getVisibility(loc))) {
            return u;
        }
        return null;
    }

    public ContactMap getContactMap() {
        return contactMap;
    }

    public Collection<Contact> getAllContacts() {
        return contactMap.getAllContacts();
    }

    public Collection<Contact> getGhostContacts() {
        return contactMap.getGhostContacts(this.game.getTurn());
    }

    public Collection<Contact> getIdentifiedContacts() {
        List<Contact> out = new ArrayList<>();
        for (Contact c : contactMap.getAllContacts()) {
            if (c.identified) out.add(c);
        }
        return out;
    }

    public void unitsNeedOrders() {
        if (isRobot()) {
            throw new SaDException(
                "The override method should have been called!"
            );
        }

        Log.info(
            this,
            "unitsNeedOrders() called, checking pendingOrders queue"
        );
        Unit pending = popPendingOrders();
        while (pending != null) {
            Log.info(
                this,
                "  Checking pending unit: " +
                    pending +
                    " hasOrders=" +
                    pending.hasOrders() +
                    " isDone=" +
                    pending.turn().isDone()
            );
            if (pending.hasOrders() || pending.turn().isDone()) {
                Log.info(
                    this,
                    "    Skipping this unit (already has orders or turn is done)"
                );
                pending = popPendingOrders();
            } else {
                Log.info(this, "    Found unit that needs orders!");
                break;
            }
        }

        if (pending != null) {
            Log.info(this, "Selecting unit from pendingOrders: " + pending);
            this.game.selectUnit(pending);
        } else {
            Log.info(this, "No units in pendingOrders need orders");
        }

        pending = this.game.selectedUnit();
        if (pending != null) {
            if (!pending.life().hasMoves()) {
                pending = null;
            }
        }

        if (pending == null) {
            List<Unit> unplayed = unplayedUnits();
            if (unplayed.isEmpty()) {
                throw new SaDException(
                    "Trying to get orders when no units are playable!"
                );
            }
            this.game.selectUnit(unplayed.get(0));
        }

        this.game.pause();
    }

    @SuppressWarnings("static-method")
    /**
     * Build a robot version of this player carrying the same state. Subclasses override.
     */
    public Player toRobotForm() {
        Robot r = new Robot(this.name, this.id);
        transferStateTo(r);
        r.initBrain();
        return r;
    }

    /**
     * Build a human version of this player carrying the same state. Subclasses override.
     */
    public Player toHumanForm() {
        return this;
    }

    /**
     * Move transferable state from this player to {@code target}. After this call,
     * this player is drained — used only by toRobotForm/toHumanForm.
     */
    protected void transferStateTo(Player target) {
        target.board = this.board;
        target.cities = this.cities;
        target.units = this.units;
        target.game = this.game;
        target.unownedCities = this.unownedCities;
        target.enemyCities = this.enemyCities;
        target.enemyUnits = this.enemyUnits;
        target.visible = this.visible;
        target.explored = this.explored;
        target.enemyActivity = this.enemyActivity;
        target.contactMap = this.contactMap;
        target.endTurnRequested = this.endTurnRequested;
        target.edictFactory = new EdictFactory(target);

        Unit u;
        while ((u = this.pendingPlay.poll()) != null) {
            target.pendingPlay.offer(u);
        }
        while ((u = this.pendingOrders.poll()) != null) {
            target.pendingOrders.offer(u);
        }
    }

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
        for (City c : this.cities) {
            ArrayList<Location> locations = getFrontier(
                c.getLocation(),
                Travel.AIR,
                dist
            );
            if (!locations.isEmpty()) {
                list.add(c);
            }
        }
        return list;
    }

    private static List<City> findReachable(
        City start,
        List<City> options,
        int dist
    ) {
        List<City> near = new ArrayList<City>();
        for (City c : options) {
            if (c.isNear(start, dist)) {
                near.add(c);
            }
        }
        return near;
    }

    public class HopState {}

    /**
     * Find the next city an air unit should fly to from {@code city} to reach
     * the nearest (fewest hops) owned city that borders unexplored territory.
     * Each hop must be within {@code dist}. Returns null if none is reachable.
     */
    public City findHopCity(City city, int dist) {
        if (city == null) {
            return null;
        }
        final int distFinal = dist; // Capture parameter for anonymous class

        Graph<City, HopState> graph = new Graph<City, HopState>() {
            @Override
            protected List<City> findRelatives(City start) {
                return findReachable(start, Player.this.cities, distFinal);
            }
        };

        GraphNode<City, HopState> startCityNode = graph.buildGraphNodes(city);

        Set<City> reachableFrontier = new HashSet<City>();
        for (City c : frontierCities(dist)) {
            if (c != city && graph.containsKey(c)) {
                reachableFrontier.add(c);
            }
        }

        if (reachableFrontier.isEmpty()) {
            return null;
        }

        // Breadth-first search, remembering the first hop taken to reach each node
        Map<GraphNode<City, HopState>, GraphNode<City, HopState>> firstHop =
            new HashMap<GraphNode<City, HopState>, GraphNode<City, HopState>>();
        Queue<GraphNode<City, HopState>> queue = new LinkedList<
            GraphNode<City, HopState>
        >();
        firstHop.put(startCityNode, null);
        queue.add(startCityNode);

        while (!queue.isEmpty()) {
            GraphNode<City, HopState> node = queue.poll();
            for (GraphNode<City, HopState> next : node.relatives()) {
                if (firstHop.containsKey(next)) {
                    continue;
                }
                GraphNode<City, HopState> hop =
                    (node == startCityNode) ? next : firstHop.get(node);
                if (reachableFrontier.contains(next.getContent())) {
                    return hop.getContent();
                }
                firstHop.put(next, hop);
                queue.add(next);
            }
        }
        return null;
    }

    public Game getGame() {
        return game;
    }

    public void startNewTurn() {
        Log.debug(this, "Starting new turn");

        // Clear visibility at the start of each turn
        clearVisibility();

        // Recalculate visibility based on current unit and city positions
        for (City c : this.cities) {
            c.startNewTurn();
            adjustVisibility(c);
        }

        for (Unit u : this.units) {
            adjustVisibility(u);
        }

        buildCityLists();
        scanForContacts();
        calcEnemyActivity();
        calcContinentsLoadingPositions();

        for (Unit u : this.units) {
            u.turn().beginTurn();
        }

        UnitStats unitStats = new UnitStats(this.units, this.cities);
        Log.info(
            "===========================================================================\r\nStarting new turn with:\n" +
                unitStats
        );
    }

    public void completeTurn() {
        @SuppressWarnings("unchecked")
        List<Unit> list = (List<Unit>) this.units.clone();
        for (Unit u : list) {
            u.turn().completeTurn();
        }
    }

    public UnitStats unitStats() {
        return new UnitStats(this.units, this.cities);
    }

    static class OrderStateCounts {

        int availableMoves = 0;
        int noOrders;
    }

    private static OrderStateCounts analyseUnplayed(List<Unit> units) {
        OrderStateCounts counts = new OrderStateCounts();
        for (Unit u : units) {
            if (
                u.turn().isDone() == false &&
                !u.hasOrders() &&
                u.life().isSleeping() == false
            ) {
                counts.noOrders++;
            }
            counts.availableMoves += u.life().movesLeft();
        }
        return counts;
    }

    public List<Unit> unplayedUnits() {
        ArrayList<Unit> units = new ArrayList<Unit>();

        //   forEachUnit((Unit u)->{Log.info(u, (u.life().hasMoves() ? "HAS MOVES" : "NO MOVES")); });

        forEachUnit((Unit u) -> {
            if (u.turn().isDone()) {
                return;
            }
            if (u.life().hasMoves()) {
                if (!u.isCarried()) {
                    units.add(u);
                } else if (
                    !u.hasOrders() &&
                    u.onboard != null &&
                    u.onboard.isUnloadingMode() &&
                    u.canDisembark()
                ) {
                    // Carried unit woken for unloading — only if there's a valid hex
                    units.add(u);
                }
            }
        });

        Unit selected = this.game.selectedUnit();
        if (selected != null) {
            Location loc = selected.getLocation();
            List<Unit> atLoc = this.game.unitsAtLocation(loc);
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

    /**
     * Play every unit the user interacted with since the last pass. These run even if carried,
     * since carried units are not in the unplayed list. The queue is drained completely:
     * each order the user gives adds an entry, and most go stale (the unit already moved in
     * the main loop), so taking one entry per pass left carried units' orders stuck behind them.
     */
    void playPendingUnits(List<Unit> unplayed) {
        Set<Unit> played = new HashSet<>();
        Unit pending;
        while ((pending = popPendingPlay()) != null) {
            if (!pending.hasOrders() || pending.isDead() || !played.add(pending)) {
                continue;
            }
            unplayed.remove(pending);
            try {
                pending.turn().attemptTurn();
            } catch (SaDException e) {
                // Unit may have died during turn execution - log and continue
                Log.warn(
                    pending,
                    "Exception during turn execution: " + e.getMessage()
                );
            }
        }
    }

    public void play() {
        int previousMovesLeft = 0;
        long turnStartTime = System.currentTimeMillis();
        int totalUnits = this.units.size();
        int unitsMoved = 0;
        this.endTurnRequested = false;

        // After loading a saved game, always pause for human input first.
        // This ensures the human player gets control before any auto-execution.
        if (!isRobot() && this.game.consumeResumingFromLoad()) {
            List<Unit> unplayed = unplayedUnits();
            if (!unplayed.isEmpty()) {
                Log.info(this, "Resuming from load - pausing for human input");
                this.game.selectUnit(unplayed.get(0));
                this.game.pause();
            }
        }

        while (true) {
            if (endTurnRequested) {
                Log.info(this, "End turn requested by player");
                break;
            }

            List<Unit> unplayed = unplayedUnits();

            if (unplayed.isEmpty()) {
                Log.info(this, "Turn over");
                break;
            }

            playPendingUnits(unplayed);

            for (Unit u : unplayed) {
                if (!u.hasOrders() || u.isDead()) {
                    continue;
                }

                this.game.selectUnit(u);
                int movesBeforeAttempt = u.life().movesLeft();
                try {
                    u.turn().attemptTurn();
                    // Track if unit moved (consumed moves)
                    if (u.life().movesLeft() < movesBeforeAttempt) {
                        unitsMoved++;
                    }
                } catch (SaDException e) {
                    // Unit may have died during turn execution - log and continue
                    Log.warn(
                        u,
                        "Exception during turn execution: " + e.getMessage()
                    );
                }
            }

            unplayed = unplayedUnits();
            OrderStateCounts orderStats = analyseUnplayed(unplayed);

            if (
                orderStats.availableMoves == previousMovesLeft &&
                orderStats.noOrders == 0
            ) {
                Log.debug("Nothing moved this pass.  Consider the turn done");
                break;
            }

            previousMovesLeft = orderStats.availableMoves;

            if (orderStats.noOrders > 0) {
                if (orderStats.availableMoves == 0) {
                    Log.info(
                        this,
                        "Units need orders but no moves available - ending turn"
                    );
                    break;
                }
                Log.debug(this, "Needs more orders");
                unitsNeedOrders();
                if (endTurnRequested) {
                    Log.info(
                        this,
                        "End turn requested during order assignment"
                    );
                    break;
                }
                previousMovesLeft = -1;
            }

            // pass++;
        }

        completeTurn();

        // Publish turn completion metrics
        long turnDuration = System.currentTimeMillis() - turnStartTime;
        this.game.getEventBus().publish(
            new com.developingstorm.games.sad.events.TurnCompleteEvent(
                this.game.getTurn(),
                this,
                turnDuration,
                totalUnits,
                unitsMoved
            )
        );
    }

    // Store the full player info
    public Object toJson() {
        // TODO Auto-generated method stub
        return null;
    }

    // Store enough to discover the correct player
    public Object toJsonLink() {
        return name;
    }

    public UnitStats getStats() {
        UnitStats us = new UnitStats(this.units, this.cities);
        return us;
    }

    public List<Unit> getUnitsOnContinent(Continent cont) {
        List<Unit> units = new ArrayList<Unit>();
        Set<Location> locations = cont.getLocations();
        for (Location loc : locations) {
            List<Unit> lu = this.game.unitsAtLocation(loc);
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
        for (Unit u : this.units) {
            if (
                u.getType().equals(Type.ARMOR) ||
                u.getType().equals(Type.INFANTRY)
            ) {
                return true;
            }
        }
        return false;
    }
}
