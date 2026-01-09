package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationLens;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.Graph;
import com.developingstorm.util.GraphNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
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
    protected String name;
    private volatile LinkedList<Unit> pendingPlay;
    private volatile LinkedList<Unit> pendingOrders;
    private EdictFactory edictFactory;

    public Player(String name, int id) {
        this.id = id;
        this.name = name;
        this.cities = new ArrayList<City>();
        this.units = new ArrayList<Unit>();
        this.unownedCities = new HashSet<City>();
        this.enemyCities = new HashSet<City>();
        this.enemyUnits = new HashSet<Unit>();
        this.pendingPlay = new LinkedList<>();
        this.pendingOrders = new LinkedList<>();

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
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Player other = (Player) obj;
        if (this.id != other.id) return false;
        return true;
    }

    public Unit popPendingPlay() {
        if (this.pendingPlay.isEmpty()) {
            return null;
        }
        return this.pendingPlay.pop();
    }

    public void pushPendingPlay(Unit u) {
        this.pendingPlay.push(u);
    }

    public Unit popPendingOrders() {
        if (this.pendingOrders.isEmpty()) {
            return null;
        }
        return this.pendingOrders.pop();
    }

    public void pushPendingOrders(Unit u) {
        this.pendingOrders.push(u);
    }

    public String toString() {
        return "Human: I=" + id;
    }

    public int getId() {
        return id;
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

    private void buildEnemyUnitList() {
        this.enemyUnits.clear();

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
                            this.enemyUnits.add(u);
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
        return this.visible[loc.x][loc.y];
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

    public void unitsNeedOrders() {
        if (isRobot()) {
            throw new SaDException(
                "The override method should have been called!"
            );
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
            this.game.selectUnit(pending);
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

    public int findRoute(
        Graph<City, HopState> graph,
        Queue<GraphNode<City, HopState>> queue,
        GraphNode<City, HopState> dest
    ) {
        GraphNode<City, HopState> start = queue.peek();
        Set<GraphNode<City, HopState>> relatives = start.relatives();

        for (GraphNode<City, HopState> gn : relatives) {
            if (gn.equals(dest)) {
                queue.add(dest);

                int dist = start
                    .getContent()
                    .getLocation()
                    .distance(dest.getContent().getLocation());
                return dist;
            }
        }

        Queue<GraphNode<City, HopState>> shortestQueue = null;
        int shortestRoute = Integer.MAX_VALUE;
        for (GraphNode<City, HopState> gn : relatives) {
            if (queue.contains(gn)) {
                continue;
            }
            Queue<GraphNode<City, HopState>> queue2 = new LinkedList<
                GraphNode<City, HopState>
            >();
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
        } else {
            for (GraphNode<City, HopState> qe : shortestQueue) {
                queue.add(qe);
            }
            return shortestRoute;
        }
    }

    public City findHopCity(City city, int dist) {
        final int distFinal = dist; // Capture parameter for anonymous class

        Graph<City, HopState> graph = new Graph<City, HopState>() {
            @Override
            protected List<City> findRelatives(City start) {
                return findReachable(start, Player.this.cities, distFinal);
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

        Queue<GraphNode<City, HopState>> queue = new LinkedList<
            GraphNode<City, HopState>
        >();
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
        return game;
    }

    public void startNewTurn() {
        Log.debug(this, "Starting new turn");
        for (City c : this.cities) {
            c.startNewTurn();
        }

        buildCityLists();
        buildEnemyUnitList();
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
            if (u.life().hasMoves()) {
                units.add(u);
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
            if (
                pending != null &&
                pending.hasOrders() &&
                unplayed.contains(pending)
            ) {
                unplayed.remove(pending);
                pending.turn().attemptTurn();
            }

            for (Unit u : unplayed) {
                if (!u.hasOrders() || u.isDead()) {
                    continue;
                }

                this.game.selectUnit(u);
                u.turn().attemptTurn();
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
