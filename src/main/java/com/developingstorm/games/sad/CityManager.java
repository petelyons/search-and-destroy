package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;

/**
 * Manages city-related operations including assignment, queries, and city capture logic.
 * Extracted from Game.java to improve maintainability.
 */
class CityManager {
    private final Game game;
    private final Board board;
    private final CombatResolver combatResolver;
    private final UnitManager unitManager;

    CityManager(Game game, Board board, CombatResolver combatResolver, UnitManager unitManager) {
        this.game = game;
        this.board = board;
        this.combatResolver = combatResolver;
        this.unitManager = unitManager;
    }

    /**
     * Assigns coastal cities to players at game start.
     */
    void assignCities() {
        Player[] players = game.getPlayers();
        java.util.Set<Continent> taken = new java.util.HashSet<>();
        for (Player p : players) {
            City c = pickStartCity(taken);
            if (c == null) {
                throw new SaDException("Not enough coastal cities!");
            }
            c.setOwner(p);
            taken.add(c.getContinent());
        }
    }

    /**
     * A random unowned coastal city, preferring a continent no earlier player started on.
     * Falls back to any unowned coastal city when the map has too few continents.
     */
    private City pickStartCity(java.util.Set<Continent> taken) {
        java.util.List<City> fresh = new java.util.ArrayList<>();
        java.util.List<City> any = new java.util.ArrayList<>();
        for (City c : board.getCities()) {
            if (c.getOwner() == null && board.isCoast(c.getLocation())) {
                any.add(c);
                if (!taken.contains(c.getContinent())) {
                    fresh.add(c);
                }
            }
        }
        java.util.List<City> pool = fresh.isEmpty() ? any : fresh;
        return pool.isEmpty() ? null : pool.get(com.developingstorm.util.RandomUtil.getInt(pool.size()));
    }

    /**
     * Gets the city at the specified location if it belongs to the current player.
     */
    City cityAtLocation(Location loc, Player currentPlayer) {
        City c = board.getCity(loc);
        if (c != null && c.getOwner() == currentPlayer) {
            return c;
        }
        return null;
    }

    /**
     * Checks if there is a city at the specified location.
     */
    boolean isCity(Location loc) {
        City c = board.getCity(loc);
        return c != null;
    }

    /**
     * Handles unit interaction with a city (owned, unowned, or enemy).
     * @return ResponseCode indicating the result of the interaction, or null to continue with normal movement
     */
    ResponseCode handleCityInteraction(Unit u, Location dest, City ourCity, City unownedCity, City enemyCity) {
        Player mover = u.getOwner();

        // Moving into our own city
        if (ourCity != null) {
            u.move(dest);
            Log.debug(u, "Moved into City.");
            u.life().burnMovesButNotFuel();
            return ResponseCode.TURN_COMPLETE;
        }

        // Moving into unowned city
        if (unownedCity != null) {
            if (u.getTravel() != Travel.LAND) {
                Log.warn("Navy/Air cannot occupy new cities");
                return ResponseCode.CANCEL_ORDER;
            }

            if (combatResolver.resolveCityAttack(u, unownedCity)) {
                Log.info(u, "Captured city");
                unitManager.killUnit(u, false);
                unownedCity.setOwner(mover);
                return ResponseCode.TURN_COMPLETE;
            } else {
                Log.info(u, "destroyed in attack");
                unitManager.killUnit(u);
                return ResponseCode.DIED;
            }
        }

        // Moving into enemy city
        if (enemyCity != null) {
            if (!(u.getTravel() == Travel.LAND || u.getType() == Type.BOMBER)) {
                Log.warn("Only bombers and land troops can attack enemy city");
                return ResponseCode.CANCEL_ORDER;
            }

            Log.debug(u, "Attacking city " + enemyCity);

            if (u.getType() == Type.BOMBER) {
                if (combatResolver.resolveCityAttack(u, enemyCity)) {
                    Log.debug(u, "Bomber attack success.");
                    unitManager.killUnit(u, false);
                    enemyCity.bombCity();
                    return ResponseCode.DIED;
                } else {
                    Log.debug(u, "Attack failed - we died");
                    unitManager.killUnit(u);
                    return ResponseCode.DIED;
                }
            } else {
                if (combatResolver.resolveCityAttack(u, enemyCity)) {
                    Log.debug(u, "Attack completed objective acheived.");
                    unitManager.killUnit(u, false);
                    unitManager.killUnits(enemyCity.getUnits());
                    enemyCity.setOwner(mover);
                    return ResponseCode.DIED;
                } else {
                    Log.debug(u, "Attack failed - we died");
                    unitManager.killUnit(u);
                    return ResponseCode.DIED;
                }
            }
        }

        // No city interaction - continue with normal movement
        return null;
    }
}
