package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;

/**
 * Resolves unit movement including blocking, loading, and combat.
 * Extracted from Game.java to improve maintainability.
 */
class MovementResolver {
    private final Game game;
    private final Board board;
    private final CityManager cityManager;
    private final CombatResolver combatResolver;
    private final UnitManager unitManager;

    MovementResolver(Game game, Board board, CityManager cityManager, CombatResolver combatResolver, UnitManager unitManager) {
        this.game = game;
        this.board = board;
        this.cityManager = cityManager;
        this.combatResolver = combatResolver;
        this.unitManager = unitManager;
    }

    /**
     * Attempts to load unit u onto transport t.
     * @return ResponseCode if load occurs, null otherwise
     */
    private static ResponseCode resolveLoad(Unit u, Unit t) {
        if (t.getOwner() == u.getOwner()) {
            if (t.canCarry(u)) {
                t.addCarried(u);
                return ResponseCode.YIELD_PASS;
            }
        }
        return null;
    }

    /**
     * Resolves a unit's move to a destination location.
     * Handles city interaction, blocking, loading, and combat.
     */
    ResponseCode resolveMove(Unit u, final Location dest) {
        if (u.getLocation().equals(dest)) {
            throw new SaDException("Unit already at location!");
        }

        // Check for city at destination and handle city interaction
        City unownedCity = null;
        City enemyCity = null;
        City ourCity = null;
        Player mover = u.getOwner();

        if (board.isCity(dest)) {
            City c = board.getCity(dest);
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
                Log.debug(
                    "Board reports destination " + dest + " is ENEMY city"
                );
            }
            if (unownedCity != null) {
                Log.debug(
                    "Board reports destination " + dest + " is FREE city"
                );
            }
        }

        // Delegate city interaction to CityManager
        ResponseCode cityResponse = cityManager.handleCityInteraction(u, dest, ourCity, unownedCity, enemyCity);
        if (cityResponse != null) {
            return cityResponse;
        }

        // Check if destination is travelable
        if (board.isTravelable(u, dest) == false) {
            Unit blocking = game.unitAtLocation(dest);
            if (blocking != null) {
                ResponseCode rc = resolveLoad(u, blocking);
                if (rc != null) {
                    Log.warn(u, "resolve load problem! " + rc);
                    return rc;
                }
            }
            Log.debug(u, "Move - CANCEL");
            return ResponseCode.CANCEL_ORDER;
        }

        // Check for blocking unit at destination
        Unit blocking = game.unitAtLocation(dest);
        if (blocking == null || blocking.equals(u)) {
            // No blocking unit - perform the move
            int pre = u.life().movesLeft();
            if (u.getLocation().equals(dest)) {
                throw new SaDException("Unit not actually moving!");
            }

            u.move(dest);
            int post = u.life().movesLeft();
            if (post >= pre) {
                throw new SaDException("Unit didn't move!");
            }
            if (u.life().movesLeft() > 0) {
                Log.debug(u, "Moved - step complete");
                return ResponseCode.STEP_COMPLETE;
            } else {
                Log.debug(u, "Moved - turn complete");
                return ResponseCode.TURN_COMPLETE;
            }
        }

        // Try to load onto blocking unit
        ResponseCode rc = resolveLoad(u, blocking);
        if (rc != null) {
            Log.warn(u, "resolve load problem 2! " + rc + " " + blocking);
            return rc;
        }

        // Check if blocked by friendly unit
        Player blocker = blocking.getOwner();
        if (blocker.equals(u.getOwner())) {
            if (!u.turn().isKnownObstruction(dest)) {
                u.turn().addObstruction(dest);
                return ResponseCode.YIELD_PASS;
            }

            if (blocking.turn().isDone()) {
                Log.debug(
                    u,
                    "Destination blocked by unit that has already moved. Cancelling:" +
                        blocking
                );
                return ResponseCode.TURN_COMPLETE;
            } else {
                blocker.pushPendingPlay(blocking);
                Log.debug(
                    u,
                    "Destination blocked by unit that has yet to move. Yielding:" +
                        blocking
                );
                return ResponseCode.YIELD_PASS;
            }
        }

        // Enemy unit - combat!
        while (true) {
            Log.debug(u, "Attacking " + blocking);
            boolean attackRes = combatResolver.resolveUnitAttack(u, blocking);
            if (attackRes) {
                Log.debug(u, "We killed them " + blocking);
                unitManager.killUnit(blocking);
                u.move(dest);
                return ResponseCode.STEP_COMPLETE;
            } else {
                Log.debug(u, "We died while attacking " + blocking);
                unitManager.killUnit(u);
                return ResponseCode.DIED;
            }
        }
    }
}
