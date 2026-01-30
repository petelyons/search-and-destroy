package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.types.*;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages unit lifecycle: creation, destruction, and location tracking.
 * Extracted from Game.java to improve maintainability.
 *
 * Thread-safety: Uses ReadWriteLock for efficient concurrent access.
 * - Multiple readers can query locations simultaneously
 * - Writers (move, create, kill) have exclusive access
 */
class UnitManager {

    private final Game game;
    private final HexBoardMap gridMap;
    private final ArrayList<Unit> allUnits;
    private final Set<Unit>[][] locations;

    // ReadWriteLock allows multiple concurrent readers but exclusive writers
    private final ReadWriteLock locationLock = new ReentrantReadWriteLock();

    @SuppressWarnings("unchecked")
    UnitManager(Game game, HexBoardMap gridMap) {
        this.game = game;
        this.gridMap = gridMap;
        this.allUnits = new ArrayList<>();

        // Initialize location tracking grid
        int w = gridMap.getWidth();
        int h = gridMap.getHeight();
        this.locations = (Set<Unit>[][]) new Set[w][h];
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                this.locations[i][j] = Collections.synchronizedSet(
                    new HashSet<>()
                );
            }
        }
    }

    /**
     * Returns the list of all units in the game.
     */
    List<Unit> getAllUnits() {
        return allUnits;
    }

    /**
     * Creates a new unit of the specified type.
     */
    synchronized Unit createUnit(Type type, Player owner, Location location) {
        Unit u = null;

        if (type == Type.INFANTRY) {
            u = new Infantry(owner, location, game);
        } else if (type == Type.ARMOR) {
            u = new Armor(owner, location, game);
        } else if (type == Type.BATTLESHIP) {
            u = new Battleship(owner, location, game);
        } else if (type == Type.BOMBER) {
            u = new Bomber(owner, location, game);
        } else if (type == Type.CARGO) {
            u = new Cargo(owner, location, game);
        } else if (type == Type.CARRIER) {
            u = new Carrier(owner, location, game);
        } else if (type == Type.CRUISER) {
            u = new Cruiser(owner, location, game);
        } else if (type == Type.DESTROYER) {
            u = new Destroyer(owner, location, game);
        } else if (type == Type.FIGHTER) {
            u = new Fighter(owner, location, game);
        } else if (type == Type.SUBMARINE) {
            u = new Submarine(owner, location, game);
        } else if (type == Type.TRANSPORT) {
            u = new Transport(owner, location, game);
        } else {
            throw new SaDException("Unsupported type");
        }

        // Assign names based on unit type
        if (isShipType(type)) {
            u.name = ShipNames.getName(type);
        } else if (UnitNames.shouldNameUnit(type)) {
            u.name = UnitNames.getName(owner, type);
        }

        // Track production location
        City productionCity = game.getBoard().getCity(location);
        if (productionCity != null) {
            u.productionCityName = productionCity.getName();
        }

        Continent productionContinent = game.getBoard().getContinent(location);
        if (productionContinent != null) {
            u.productionContinentName = productionContinent.getName();
        }

        allUnits.add(u);
        return u;
    }

    /**
     * Checks if a type is a naval vessel.
     */
    private boolean isShipType(Type type) {
        return (
            type == Type.DESTROYER ||
            type == Type.CRUISER ||
            type == Type.BATTLESHIP ||
            type == Type.CARRIER ||
            type == Type.SUBMARINE ||
            type == Type.TRANSPORT
        );
    }

    /**
     * Kills a unit and removes it from the game.
     */
    synchronized void killUnit(Unit u, boolean showDeath) {
        Log.debug(game, "Killing Unit: " + u);

        // Release unit name back to pool
        if (u.name != null) {
            if (isShipType(u.getType())) {
                ShipNames.releaseName(u.getType(), u.name);
            }
            // Note: Land/air unit names aren't released as they're player-specific
        }

        u.kill();
        u.getOwner().removeUnit(u);
        allUnits.remove(u);
        game
            .getEventBus()
            .publish(
                new com.developingstorm.games.sad.events.UnitKilledEvent(
                    u,
                    showDeath
                )
            );
        removeUnitFromBoard(u);

        // Remove from all players' last-seen tracking
        for (Player p : game.getPlayers()) {
            p.removeLastSeenEnemy(u.id);
        }
    }

    /**
     * Kills a unit with death animation.
     */
    void killUnit(Unit u) {
        killUnit(u, true);
    }

    /**
     * Kills multiple units without showing death animation for each.
     */
    void killUnits(List<Unit> units) {
        for (Unit u : units) {
            killUnit(u, false);
        }
    }

    /**
     * Places a unit on the board at its current location.
     */
    void placeUnitOnBoard(Unit u) {
        Log.info(u, "Placing unit on board");
        locationLock.writeLock().lock();
        try {
            Set<Unit> l = locations[u.getLocation().x][u.getLocation().y];
            l.add(u);
            validateLocations();
        } finally {
            locationLock.writeLock().unlock();
        }
    }

    /**
     * Removes a unit from the board.
     */
    void removeUnitFromBoard(Unit u) {
        Log.info(u, "Removing unit from board");
        locationLock.writeLock().lock();
        try {
            Set<Unit> l = locations[u.getLocation().x][u.getLocation().y];
            l.remove(u);
            validateLocations();
        } finally {
            locationLock.writeLock().unlock();
        }
    }

    /**
     * Updates unit's location in the tracking grid.
     * Now uses write lock to make the entire operation atomic.
     * Publishes UNIT_MOVED event to notify UI of the change.
     */
    void changeUnitLocation(Unit u, Location newLoc) {
        Location oldLoc = u.getLocation();

        locationLock.writeLock().lock();
        try {
            Set<Unit> oldSet = getSetofUnitsAtLocation(oldLoc);
            Set<Unit> newSet = getSetofUnitsAtLocation(newLoc);
            oldSet.remove(u);
            newSet.add(u);
        } finally {
            locationLock.writeLock().unlock();
        }

        // Publish event for UI notification
        game
            .getEventBus()
            .publish(
                new com.developingstorm.games.sad.events.UnitMovedEvent(
                    u.id,
                    oldLoc,
                    newLoc
                )
            );
    }

    /**
     * Gets the set of units at a specific location.
     * Uses read lock for thread-safe access.
     * Note: The returned set is still synchronized for iteration safety.
     */
    Set<Unit> getSetofUnitsAtLocation(Location loc) {
        locationLock.readLock().lock();
        try {
            return locations[loc.x][loc.y];
        } finally {
            locationLock.readLock().unlock();
        }
    }

    /**
     * Validates that all units are at their correct locations in the grid.
     */
    private void validateLocations() {
        int w = gridMap.getWidth();
        int h = gridMap.getHeight();
        int errors = 0;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                Set<Unit> units = locations[x][y];
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
        }

        if (errors > 0) {
            throw new SaDException("Location validation failed");
        }
    }
}
