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

/**
 * Manages unit lifecycle: creation, destruction, and location tracking.
 * Extracted from Game.java to improve maintainability.
 */
class UnitManager {
    private final Game game;
    private final HexBoardMap gridMap;
    private final ArrayList<Unit> allUnits;
    private final Set<Unit>[][] locations;

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
                this.locations[i][j] = Collections.synchronizedSet(new HashSet<>());
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
        allUnits.add(u);
        return u;
    }

    /**
     * Kills a unit and removes it from the game.
     */
    synchronized void killUnit(Unit u, boolean showDeath) {
        Log.debug(game, "Killing Unit: " + u);
        u.kill();
        u.getOwner().removeUnit(u);
        allUnits.remove(u);
        game.getGameListener().killUnit(u, showDeath);
        removeUnitFromBoard(u);
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
        Set<Unit> l = getSetofUnitsAtLocation(u.getLocation());
        l.add(u);
        validateLocations();
    }

    /**
     * Removes a unit from the board.
     */
    void removeUnitFromBoard(Unit u) {
        Log.info(u, "Removing unit from board");
        Set<Unit> l = getSetofUnitsAtLocation(u.getLocation());
        l.remove(u);
        validateLocations();
    }

    /**
     * Updates unit's location in the tracking grid.
     */
    void changeUnitLocation(Unit u, Location newLoc) {
        Set<Unit> oldSet = getSetofUnitsAtLocation(u.getLocation());
        Set<Unit> newSet = getSetofUnitsAtLocation(newLoc);
        oldSet.remove(u);
        newSet.add(u);
    }

    /**
     * Gets the set of units at a specific location.
     */
    Set<Unit> getSetofUnitsAtLocation(Location loc) {
        return locations[loc.x][loc.y];
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
