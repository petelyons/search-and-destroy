package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.orders.Attack;
import com.developingstorm.games.sad.orders.Explore;
import com.developingstorm.games.sad.orders.HeadHome;
import com.developingstorm.games.sad.orders.Move;
import com.developingstorm.games.sad.orders.MoveEast;
import com.developingstorm.games.sad.orders.MoveNorthEast;
import com.developingstorm.games.sad.orders.MoveNorthWest;
import com.developingstorm.games.sad.orders.MoveSouthEast;
import com.developingstorm.games.sad.orders.MoveSouthWest;
import com.developingstorm.games.sad.orders.MoveWest;
import com.developingstorm.games.sad.orders.Patrol;
import com.developingstorm.games.sad.orders.Sentry;
import com.developingstorm.games.sad.orders.SkipTurn;
import com.developingstorm.games.sad.orders.Unload;
import com.developingstorm.games.sad.turn.UnitTurnState;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.RandomUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Units are the moveable pieces of the game.
 * Units have attributes based on their Type. Infantry, Battleships and Bombers are all examples of Types of units.
 * Different Types of units can travel in different terrains.
 * Units have a 'life' which controls how much damage they can absorb, and how much they can move.
 * Some units can carry other units.
 * Units are always owned by one of the games players.
 */
public abstract class Unit {

    static long s_unitCounter = 1;

    private volatile Type type; // This type of unit
    private volatile Player owner;
    private volatile Location loc;
    private volatile Board board;
    private volatile Travel travel;
    private volatile Game game;
    public volatile ArrayList<Unit> carries;
    public volatile Unit onboard;
    public volatile int dist;
    private volatile Order order;
    public volatile String name;
    public volatile long id;
    private volatile UnitTurnState turn;
    private boolean isDead = false; //Used to prevent endless looping during kill processing u->game->u->game->u->game...
    private volatile boolean unloadingMode = false; // Transport is actively unloading troops
    public Life life;

    // Production location tracking
    public volatile String productionContinentName;
    public volatile String productionCityName;

    public class Life {

        public volatile int hits;
        private volatile int fuel;
        private volatile int moves;
        private volatile boolean sleeping;

        public Life() {
            hits = Unit.this.type.getHits();
            fuel = Unit.this.type.getFuel();
            moves = Unit.this.type.getDist();
            sleeping = false;
        }

        public void repair() {
            if (this.hits < Unit.this.type.getHits()) {
                this.hits++;
            }
        }

        public void fuel() {
            fuel = Unit.this.type.getFuel();
        }

        public void resetForTurn() {
            moves = Unit.this.type.getDist();
        }

        public void move() {
            this.moves--;

            if (Unit.this.type.getFuel() != 0 && !atCity()) {
                this.fuel--;
            }
        }

        public void wake() {
            sleeping = false;
        }

        public void sleep() {
            sleeping = true;
        }

        public boolean isSleeping() {
            return sleeping;
        }

        public void burnMoves() {
            while (movesLeft() > 0) {
                move();
            }
        }

        public void burnMovesButNotFuel() {
            while (movesLeft() > 0) {
                this.moves--;
            }
        }

        public boolean hasDied() {
            return this.hits <= 0 || !hasFuel();
        }

        public boolean hasFuel() {
            if (Unit.this.travel != Travel.AIR) {
                return true;
            }
            return this.fuel > 0;
        }

        public int getFuel() {
            return this.fuel;
        }

        public int getMaxFuel() {
            return Unit.this.type.getFuel();
        }

        public boolean hasMoves() {
            return sleeping == false && this.moves > 0 && !hasDied();
        }

        public int turnAroundDist() {
            if (travel == Travel.AIR) {
                return (getMaxTravel() / 2);
            }
            return 0;
        }

        public boolean mustLand() {
            if (travel == Travel.AIR) {
                if (this.fuel < turnAroundDist()) {
                    return true;
                }
            }
            return false;
        }

        public String healthDesc() {
            return "" + this.hits + " of " + Unit.this.type.getHits();
        }

        public String moveDesc() {
            String s = "" + this.moves + " of " + dist;
            int maxTravel = getMaxTravel();
            if (maxTravel > 0) {
                s += " [" + this.fuel + " of " + maxTravel + "]";
            }
            return s;
        }

        public int movesLeft() {
            return moves;
        }

        public boolean attack(int attackVal) {
            if (attackVal == 0) {
                return isDead();
            }
            if (attackVal < 0) {
                throw new SaDException("BAD ATTACK VALUE");
            }
            this.hits -= attackVal;
            return isDead();
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append('[');
            sb.append("HITS=");
            sb.append(this.hits);
            sb.append("/");
            sb.append(Unit.this.type.getHits());
            sb.append(": MOVE=");
            sb.append(this.moves);
            sb.append("/");
            sb.append(Unit.this.type.getDist());
            if (Unit.this.type.getFuel() > 0) {
                sb.append(": FUEL=");
                sb.append(this.fuel);
                sb.append("/");
                sb.append(Unit.this.type.getFuel());
            }
            sb.append(']');
            return sb.toString();
        }

        public void kill() {
            hits = 0;
        }

        public int remainingFuel() {
            return fuel;
        }
    }

    protected Unit(Type t, Player owner, Location loc, Game game) {
        this.type = t;
        this.loc = loc;
        this.name = null;
        this.dist = this.type.getDist();
        this.travel = this.type.getTravel();
        this.owner = owner;
        this.game = game;
        this.board = this.game.getBoard();
        this.order = null;
        this.turn = new UnitTurnState(game, this);
        this.life = new Life();
        synchronized (this) {
            s_unitCounter++;
            this.id = s_unitCounter;
        }
        // A unit needs an ID before it can be placed!
        this.game.placeUnitOnBoard(this);
    }

    public boolean atCity() {
        return this.game.isCity(this.loc);
    }

    private synchronized void changeLoc(Location loc) {
        Log.info(this, "change location to " + loc);
        this.game.changeUnitLoc(this, loc);
        this.loc = loc;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        sb.append(toUIString());
        sb.append(": Own=");
        sb.append(this.owner);
        sb.append(": Loc=(");
        sb.append(this.loc);
        sb.append("): Life=");
        sb.append(this.life);
        if (this.onboard != null) {
            sb.append(": On=");
            sb.append(this.onboard.toUIString());
        }
        if (this.carries != null) {
            sb.append(": Carries=");
            sb.append(carriesDesc());
        }
        sb.append(']');
        return sb.toString();
    }

    public String carriesDesc() {
        if (!canCarry()) {
            return "N/A";
        }
        if (carries == null || this.carries.isEmpty()) {
            return "None";
        }
        StringBuffer sb = new StringBuffer();
        sb.append(this.carries.size());
        sb.append(" unit");
        if (this.carries.size() != 1) {
            sb.append("s");
        }
        sb.append(" (");
        for (int i = 0; i < this.carries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Unit u = this.carries.get(i);
            sb.append(u.getType().getAbr());
        }
        sb.append(")");
        return sb.toString();
    }

    public String toUIString() {
        StringBuffer sb = new StringBuffer();

        sb.append(this.type);
        sb.append(' ');
        sb.append(this.id);
        sb.append(": ");
        if (life == null) {
            sb.append("INIT");
        } else if (this.life.isSleeping()) {
            sb.append("SLEEP");
        } else {
            sb.append("AWAKE");
        }
        sb.append(": ");
        sb.append(
            (this.order != null) ? this.order.getType().toString() : "No Orders"
        );
        return sb.toString();
    }

    public boolean hasOrders() {
        return (this.order != null);
    }

    public String typeDesc() {
        if (this.name != null) {
            return (this.type + " " + this.name);
        } else {
            return this.type.toString() + " " + id;
        }
    }

    public String locationDesc() {
        return this.loc.toString();
    }

    public Location getLocation() {
        return this.loc;
    }

    public void move(Location loc) {
        if (this.loc.distance(loc) == 0) {
            Log.error(this, "Attempting to spend move with no move");
            throw new SaDException("Invalid non-move");
        }

        if (this.loc.distance(loc) > 1) {
            Log.error(this, "Invalid Move to location " + loc);
            throw new SaDException("Invalid Move:");
        }

        if (!this.life.hasMoves()) {
            throw new SaDException("This unit shouldn't be moving! " + this);
        }

        // If this unit is being carried, remove it from the transport
        if (this.onboard != null) {
            Unit transport = this.onboard;
            transport.removeCarried(this);
            Log.info(this, "Unloaded from transport " + transport);
        }

        changeLoc(loc);

        this.life.move();
        this.owner.adjustVisibility(this);
        this.game.trackUnit(this);

        // If this unit was on-board another unit, and it moved then it's no longer carried
        if (this.onboard != null) {
            this.onboard.removeCarried(this);
        }

        // Move the carried units
        if (this.carries != null) {
            for (Unit u2 : this.carries) {
                u2.changeLoc(this.loc);
            }

            // If this is a transport not along coast, carried units should be in sentry
            if (this.isTransport() && !isAlongCoast()) {
                // Clear unloading mode when leaving coast
                if (this.unloadingMode) {
                    setUnloadingMode(false);
                }
                for (Unit u2 : this.carries) {
                    if (!u2.inSentryMode()) {
                        u2.orderSentry();
                    }
                }
            }
        }

        // Land the aircraft
        if (travel == Travel.AIR) {
            City city = this.board.getCity(loc);
            if (city != null && city.getOwner() == getOwner()) {
                this.turn.clearOrderAndCompleteTurn();
            }
        }

        // Auto-load onto transport if moving into same hex
        if (this.travel == Travel.LAND && !this.isCarried()) {
            // Look for a friendly transport at this location
            List<Unit> unitsHere = this.game.unitsAtLocation(loc);
            for (Unit otherUnit : unitsHere) {
                if (
                    otherUnit != this &&
                    otherUnit.getOwner() == this.owner &&
                    otherUnit.isTransport() &&
                    otherUnit.canLoad(this)
                ) {
                    // Auto-load onto the transport
                    otherUnit.load(this);
                    Log.info(this, "Auto-loaded onto transport at " + loc);
                    this.turn.clearOrderAndCompleteTurn();
                    break; // Only load onto one transport
                }
            }
        }

        // If the unit ran out of fuel, it dies
        if (!this.life.hasFuel()) {
            this.game.killUnit(this);
        }
    }

    public UnitTurnState turn() {
        return turn;
    }

    public Travel getTravel() {
        return travel;
    }

    public Type getType() {
        return type;
    }

    public boolean inSentryMode() {
        return (this.order != null && this.order.getType() == OrderType.SENTRY);
    }

    public Player getOwner() {
        return owner;
    }

    public int getAttack() {
        return this.type.getAttack();
    }

    public boolean canTravel(Location loc) {
        Travel t = this.type.getTravel();
        if (t == Travel.AIR) return true;
        if (t == Travel.SEA && this.board.isWater(loc)) return true;
        if (t == Travel.LAND && this.board.isLand(loc)) return true;
        return false;
    }

    public Path getPath(Location loc) {
        Path p = this.turn.getPath(loc);
        if (p != null && p.isEmpty()) {
            return null;
        }
        return p;
    }

    public boolean isDead() {
        return this.life.hasDied() || isDead;
    }

    public boolean isUnloadingMode() {
        return this.unloadingMode;
    }

    public void setUnloadingMode(boolean unloading) {
        this.unloadingMode = unloading;
        if (unloading) {
            Log.info(this, "Entering unloading mode");
        } else {
            Log.info(this, "Exiting unloading mode");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (this.id ^ (this.id >>> 32));
        result = prime * result + ((type == null) ? 0 : this.type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Unit other = (Unit) obj;
        if (this.id != other.id) return false;
        if (type == null) {
            if (other.type != null) return false;
        } else if (!this.type.equals(other.type)) return false;
        return true;
    }

    // Should only be called by (Game).killUnit
    void kill() {
        if (this.isDead) {
            return;
        }
        killCarried();
        this.life.kill();
        isDead = true;
        if (this.onboard != null) {
            this.onboard.removeCarried(this);
        }
    }

    public boolean isCarried() {
        return this.onboard != null;
    }

    public int carriedWeight() {
        if (carries == null) {
            return 0;
        }

        int weight = 0;
        for (Unit u : this.carries) {
            Type t = u.getType();
            weight += t.getWeight();
        }
        return weight;
    }

    public int carriableWeight() {
        return this.type.getCarryCount();
    }

    public Type[] getCarryTypes() {
        return this.type.getCarryTypes();
    }

    public int getWeight() {
        return this.type.getWeight();
    }

    public boolean canCarry(Unit u) {
        return (
            this.type.canCarry(u.getType()) &&
            carriedWeight() + u.getWeight() <= carriableWeight()
        );
    }

    public boolean canCarry(Type t) {
        return (this.type.canCarry(t));
    }

    /**
     * Check if this unit can load another unit
     */
    public boolean canLoad(Unit u) {
        if (!canCarry()) {
            return false;
        }
        if (!canCarry(u.getType())) {
            return false;
        }
        if (carriedWeight() + u.getType().getWeight() > carriableWeight()) {
            return false;
        }
        // Cargo planes can only load in cities
        if (this.type == Type.CARGO) {
            City city = this.board.getCity(this.loc);
            return city != null && city.getOwner() == this.owner;
        }
        return true;
    }

    /**
     * Load a unit onto this carrier/transport
     */
    public void load(Unit u) {
        if (!canLoad(u)) {
            throw new SaDException("Cannot load unit: " + u);
        }
        addCarried(u);
    }

    public void addCarried(Unit u) {
        addCarried(u, true);
    }

    /**
     * Adds a carried unit. If assignOrder is false, does not assign a sentry order
     * (used when restoring from save files where orders are restored separately).
     */
    public void addCarried(Unit u, boolean assignOrder) {
        if (carries == null) {
            carries = new ArrayList<Unit>();
        }
        if (u.onboard != null) {
            throw new SaDException("Unit already loaded" + u);
        }
        if (!u.getLocation().equals(this.loc)) {
            Log.warn(
                this,
                "Unit being loaded is not at exact location - adjusting position"
            );
            u.changeLoc(this.loc);
        }

        u.onboard = this;
        if (assignOrder) {
            u.orderSentry();
        }
        this.carries.add(u);
    }

    public void unload() {
        if (carries == null) {
            Log.debug(this, "Unload called but carries is null");
            return;
        }

        if (this.carries.isEmpty()) {
            Log.debug(this, "Unload called but no units are being carried");
            return;
        }

        // Count available adjacent hexes for unloading
        int availableSpaces = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                Location adjacent = Location.get(
                    this.loc.x + dx,
                    this.loc.y + dy
                );
                if (
                    adjacent != null && this.game.getBoard().onBoard(adjacent)
                ) {
                    // Check if any carried unit can move to this location
                    for (Unit u : this.carries) {
                        if (this.game.getBoard().isTravelable(u, adjacent)) {
                            availableSpaces++;
                            break; // Found space for at least one unit type
                        }
                    }
                }
            }
        }

        if (availableSpaces == 0) {
            Log.warn(
                this,
                "Cannot unload - no adjacent space available for carried units to move"
            );
            return;
        }

        Log.info(
            this,
            "Unloading " +
                this.carries.size() +
                " units (spaces available: " +
                availableSpaces +
                ")"
        );

        // Only activate ONE unit at a time to avoid race conditions
        // as spaces get filled by units that have already moved off
        Unit firstUnit = this.carries.get(0);
        Log.info(this, "  Activating first carried unit: " + firstUnit);
        firstUnit.activate();
        this.owner.pushPendingOrders(firstUnit);

        Log.info(
            this,
            "Unload complete, activated 1 of " + this.carries.size() + " units"
        );
    }

    public void removeCarried(Unit u) {
        if (this.carries != null) {
            this.carries.remove(u);
        }
        u.onboard = null;
    }

    public boolean isVisible(Vision v) {
        if (v == Vision.NONE) return false;
        if (v == Vision.SURFACE) {
            if (type == Type.SUBMARINE) return false;
            return true;
        }
        if (v == Vision.WATER) {
            if (travel == Travel.AIR || travel == Travel.LAND) {
                return false;
            }
            return true;
        }
        return true;
    }

    public ArrayList<Location> getAreaOfInfluence() {
        ArrayList<Location> list = new ArrayList<Location>();
        for (Location loc : list) {
            if (canTravel(loc)) {
                list.add(loc);
            }
        }
        return list;
    }

    void killCarried() {
        if (this.carries != null) {
            @SuppressWarnings("unchecked")
            List<Unit> copy = (List<Unit>) this.carries.clone();
            this.game.killUnits(copy);
        }
    }

    public void repairAndRefuel() {
        if (this.game.isCity(this.loc)) {
            this.life.repair();
            this.life.fuel();
        }
    }

    public void autoLoad() {
        if (this.game.isCity(this.loc)) {
            if (carriableWeight() > 0 && carriedWeight() < carriableWeight()) {
                List<Unit> ul = this.game.unitsAtLocation(this.loc);
                for (Unit u : ul) {
                    // Don't try to load ourselves
                    if (u == this) {
                        continue;
                    }
                    // Only load units that are not already on a transport (this or another)
                    if (u.isCarried() == false && u.onboard == null) {
                        if (canCarry(u)) {
                            Log.debug(
                                this,
                                "Auto-loading unit: " + u.toUIString()
                            );
                            addCarried(u);
                            // Wake transport if now full
                            if (
                                carriedWeight() >= carriableWeight() &&
                                inSentryMode()
                            ) {
                                this.life.wake();
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean hasCargo() {
        if (carries == null) {
            return false;
        }
        return !this.carries.isEmpty();
    }

    public void clearOrders() {
        order = null;
    }

    public void orderSentry() {
        assignOrder(newSentryOrder());
    }

    public void orderMove(Location loc) {
        assignOrder(newMoveOrder(loc));
    }

    public void orderEscort(Unit unitToEscort) {
        assignOrder(newEscortOrder(unitToEscort));
    }

    public void assignOrder(Order order) {
        this.order = order;
        // Wake up sleeping units when a new order is assigned
        // (except for sentry orders which intentionally put units to sleep)
        if (!order.getType().equals(OrderType.SENTRY)) {
            this.life.wake();
            // Reset turn state so unit can execute the new order this turn
            // This is important for edicts that apply to units that have already completed their turn
            this.turn.updateOrderState();
        }
    }

    public Location getClosestLocation(
        Collection<Location> locationsCollection
    ) {
        List<Location> locations = new ArrayList<>(locationsCollection);
        Collections.sort(locations, new DistanceComparator(this.loc));

        Location shortest = null;
        int shortestLen = 99999;

        for (Location loc : locations) {
            if (this.loc.equals(loc)) {
                return loc;
            }

            if (this.turn.isKnownObstruction(loc)) {
                continue;
            }

            Path p = getPath(loc);
            if (p == null) {
                continue;
            }
            int pLen = p.length();
            int tLen = this.loc.distance(loc);
            if (pLen == tLen) {
                return loc;
            } else if (pLen < shortestLen) {
                shortestLen = pLen;
                shortest = loc;
            }
        }
        return shortest;
    }

    private static class DistanceComparator implements Comparator<Object> {

        private Location loc;

        public DistanceComparator(Location loc) {
            this.loc = loc;
        }

        /**
         *
         */
        public int compare(Object arg0, Object arg1) {
            Location loc0 = (Location) arg0;
            Location loc1 = (Location) arg1;

            int dist0 = this.loc.distance(loc0);
            int dist1 = this.loc.distance(loc1);
            if (dist0 == dist1) {
                return 0;
            }
            if (dist0 < dist1) {
                return -1;
            }
            return 1;
        }
    }

    public Order getOrder() {
        return order;
    }

    public boolean canCarry() {
        return this.type.getCarryCount() > 0;
    }

    public boolean hasLanded() {
        if (travel == Travel.AIR) {
            City city = this.board.getCity(this.loc);
            return (city != null && city.getOwner() == getOwner());
        }
        throw new SaDException("hasLanded called for non air unit");
    }

    public boolean canAttackCity() {
        return (travel == Travel.LAND || type == Type.BOMBER);
    }

    public Explore newExploreOrder() {
        return new Explore(this.game, this);
    }

    public HeadHome newHeadHomeOrder() {
        return new HeadHome(this.game, this);
    }

    public Move newMoveOrder(Location loc) {
        if (loc == null) {
            throw new SaDException("Cannot move to NULL location");
        }
        return new Move(this.game, this, loc);
    }

    public Move newRandomMoveOrder() {
        List<Location> ring = this.loc.getRing(1);
        List<Location> reachable = new ArrayList();
        for (Location loc : ring) {
            if (!MapState.isBlocked(loc, true)) {
                reachable.add(loc);
            }
        }

        if (!reachable.isEmpty()) {
            Location loc = RandomUtil.randomValue(reachable);
            return new Move(this.game, this, loc);
        }
        return null;
    }

    public Sentry newSentryOrder() {
        return new Sentry(this.game, this);
    }

    public com.developingstorm.games.sad.orders.Escort newEscortOrder(
        Unit unitToEscort
    ) {
        return new com.developingstorm.games.sad.orders.Escort(
            this.game,
            this,
            unitToEscort
        );
    }

    public Unload newUnloadOrder() {
        return new Unload(this.game, this);
    }

    public MoveEast newMoveEast() {
        return new MoveEast(this.game, this);
    }

    public MoveWest newMoveWest() {
        return new MoveWest(this.game, this);
    }

    public MoveNorthWest newMoveNorthWest() {
        return new MoveNorthWest(this.game, this);
    }

    public MoveSouthWest newMoveSouthWest() {
        return new MoveSouthWest(this.game, this);
    }

    public MoveNorthEast newMoveNorthEast() {
        return new MoveNorthEast(this.game, this);
    }

    public MoveSouthEast newMoveSouthEast() {
        return new MoveSouthEast(this.game, this);
    }

    public SkipTurn newSkipTurn() {
        return new SkipTurn(this.game, this);
    }

    public Patrol newPatrolOrder(
        List<Location> waypoints,
        Patrol.PatrolMode mode
    ) {
        return new Patrol(this.game, this, waypoints, mode);
    }

    public Attack newAttackOrder(Location targetLocation) {
        return new Attack(this.game, this, targetLocation);
    }

    /**
     * Construct and order given an OrderType and an optional parameter.
     *
     * WHEN OrderType.MOVE, p1 MUST BE Location
     *
     * Otherwise p1 is ignored
     *
     * @param order
     * @param p1
     * @return
     */
    public Order newOrder(OrderType order, Object p1) {
        return newOrder(order, p1, false); // Default: no avoidance
    }

    public Order newOrder(OrderType order, Object p1, boolean avoidEnemies) {
        if (order.equals(OrderType.EXPLORE)) {
            return newExploreOrder();
        } else if (order.equals(OrderType.HEAD_HOME)) {
            return newHeadHomeOrder();
        } else if (order.equals(OrderType.MOVE)) {
            return new Move(this.game, this, (Location) p1, avoidEnemies);
        } else if (order.equals(OrderType.SENTRY)) {
            return newSentryOrder();
        } else if (order.equals(OrderType.UNLOAD)) {
            return newUnloadOrder();
        } else if (order.equals(OrderType.MOVE_WEST)) {
            return newMoveWest();
        } else if (order.equals(OrderType.MOVE_EAST)) {
            return newMoveEast();
        } else if (order.equals(OrderType.MOVE_NORTH_WEST)) {
            return newMoveNorthWest();
        } else if (order.equals(OrderType.MOVE_SOUTH_WEST)) {
            return newMoveSouthWest();
        } else if (order.equals(OrderType.MOVE_NORTH_EAST)) {
            return newMoveNorthEast();
        } else if (order.equals(OrderType.MOVE_SOUTH_EAST)) {
            return newMoveSouthEast();
        } else if (order.equals(OrderType.SKIPTURN)) {
            return newSkipTurn();
        } else if (order.equals(OrderType.ATTACK)) {
            return newAttackOrder((Location) p1);
        } else {
            throw new SaDException("Unknown order type:" + order.toString());
        }
    }

    public Life life() {
        return life;
    }

    public int getMaxTravel() {
        return this.type.getFuel();
    }

    public OrderResponse execOrder(Order alternate) {
        OrderResponse lastOrderResponse = null;
        if (isDead()) {
            throw new SaDException("Dead units should not be playing");
        }

        Log.debug(this, "Getting units orders");

        Order order = alternate;
        if (order == null) {
            order = getOrder();
        }
        if (order == null) {
            throw new SaDException("Attempting to play unit with no order!");
        }
        if (this != order.getAssignee()) {
            throw new SaDException("Order does not belong to unit running it!");
        }

        lastOrderResponse = order.execute();
        return lastOrderResponse;
    }

    public void activate() {
        Log.info(this, "Activating unit - clearing orders and waking");
        clearOrders();
        this.life.wake();
        // Update turn state - since we just cleared orders, this will set state to AWAITING_ORDERS
        // This ensures isDone() returns false so the unit can be selected for orders
        this.turn.updateOrderState();
        Log.info(
            this,
            "Turn state updated - unit is now awaiting orders (isDone=" +
                this.turn.isDone() +
                ")"
        );
    }

    public boolean isInfantry() {
        return type == Type.INFANTRY;
    }

    public boolean isArmour() {
        return type == Type.ARMOR;
    }

    public boolean isBomber() {
        return type == Type.BOMBER;
    }

    public boolean isTransport() {
        return type == Type.TRANSPORT;
    }

    public boolean isCargo() {
        return type == Type.CARGO;
    }

    public boolean isDestroyer() {
        return type == Type.DESTROYER;
    }

    public boolean isFighter() {
        return type == Type.FIGHTER;
    }

    public boolean isSubmarine() {
        return type == Type.SUBMARINE;
    }

    public boolean isCruiser() {
        return type == Type.CRUISER;
    }

    public boolean isBattleship() {
        return type == Type.BATTLESHIP;
    }

    public boolean isCarrier() {
        return type == Type.CARRIER;
    }

    public Continent getContinent() {
        return this.board.getContinent(this.loc);
    }

    /**
     * Checks if this unit (typically a transport on water) is along the coast.
     * Returns true if any adjacent hex is land.
     */
    public boolean isAlongCoast() {
        if (!this.board.isWater(this.loc)) {
            return false;
        }

        List<Location> ring = this.loc.getRing(1);
        for (Location neighborLoc : ring) {
            if (this.board.isLand(neighborLoc)) {
                return true;
            }
        }
        return false;
    }
}
