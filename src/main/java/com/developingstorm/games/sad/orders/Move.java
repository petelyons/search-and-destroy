package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MapState;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.combat.EnemyDetector;
import com.developingstorm.games.sad.util.Log;

/**

 *
 */
public class Move extends Order {

    protected Path lastPath = null;
    protected Location loc;
    private final boolean avoidEnemies; // Flag to enable enemy avoidance behavior

    public Move(Game g, Unit u, Location loc) {
        this(g, u, loc, false); // Default: no avoidance for backward compatibility
    }

    public Move(Game g, Unit u, Location loc, boolean avoidEnemies) {
        super(g, u, OrderType.MOVE);
        this.loc = loc;
        this.avoidEnemies = avoidEnemies;
    }

    protected Move(Game g, Unit u, OrderType t, Location loc) {
        this(g, u, t, loc, false);
    }

    protected Move(
        Game g,
        Unit u,
        OrderType t,
        Location loc,
        boolean avoidEnemies
    ) {
        super(g, u, t);
        this.loc = loc;
        this.avoidEnemies = avoidEnemies;
    }

    public OrderResponse executeInternal() {
        ResponseCode resp = ResponseCode.CANCEL_ORDER;

        if (loc == null) {
            throw new SaDException("No Move Order");
        }

        // Check if unit is still on a transport
        if (this.unit.isCarried()) {
            Log.info(
                this.unit,
                "Unit is on transport " +
                    this.unit.onboard +
                    ", executing move will unload"
            );
        }

        Location dest = loc;

        if (this.unit.getLocation().equals(dest)) {
            return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
        }

        if (this.unit.getLocation().distance(this.loc) > 1) {
            if (lastPath == null) {
                lastPath = this.unit.getPath(this.loc);
                if (lastPath == null || this.lastPath.isEmpty()) {
                    // Log specific reason for path failure
                    if (MapState.isBlocked(this.loc)) {
                        Log.error(
                            this.unit,
                            "Move CANCELLED: destination " +
                                this.loc +
                                " is blocked"
                        );
                    } else if (MapState.isBlocked(this.unit.getLocation())) {
                        Log.error(
                            this.unit,
                            "Move CANCELLED: starting location " +
                                this.unit.getLocation() +
                                " is blocked"
                        );
                    } else {
                        Log.error(
                            this.unit,
                            "Move CANCELLED: no path available from " +
                                this.unit.getLocation() +
                                " to " +
                                this.loc
                        );
                    }
                    resp = ResponseCode.CANCEL_ORDER;
                    return new OrderResponse(resp, this, null);
                }
            }

            while (this.unit.life().movesLeft() > 0 && !this.unit.isDead()) {
                // ENEMY DETECTION: Check for threats before each step
                if (this.avoidEnemies) {
                    int detectionRange = this.unit.getType().getDist() * 2;
                    Unit enemyThreat = EnemyDetector.detectNearbyEnemy(
                        this.unit,
                        this.game,
                        detectionRange
                    );

                    if (enemyThreat != null) {
                        // Check if we should engage or avoid
                        boolean shouldEngage = EnemyDetector.shouldEngageEnemy(
                            this.unit,
                            enemyThreat,
                            this.game,
                            0.7
                        );

                        if (!shouldEngage) {
                            // Avoid: Clear path to force recalculation around threat
                            Log.info(
                                this.unit,
                                "Move: avoiding enemy " +
                                    enemyThreat +
                                    " at " +
                                    enemyThreat.getLocation() +
                                    " - ending turn to avoid infinite loop"
                            );
                            this.lastPath = null;

                            // Complete turn instead of yielding to avoid infinite loop
                            // The unit will try again next turn when the tactical situation may have changed
                            return new OrderResponse(
                                ResponseCode.TURN_COMPLETE,
                                this,
                                null
                            );
                        } else {
                            // Favorable matchup: continue toward destination
                            Log.info(
                                this.unit,
                                "Move: willing to engage enemy " +
                                    enemyThreat +
                                    " (favorable odds)"
                            );
                        }
                    }
                }

                dest = this.lastPath.next(this.unit.getLocation());
                if (dest == null) {
                    int finalMove = this.unit.getLocation().distance(this.loc);
                    if (finalMove == 1) {
                        dest = loc;
                    } else {
                        Log.error(
                            this.unit,
                            "Move CANCELLED: cannot find next step in path from " +
                                this.unit.getLocation() +
                                " to " +
                                this.loc +
                                " (distance: " +
                                finalMove +
                                ")"
                        );
                        return new OrderResponse(
                            ResponseCode.CANCEL_ORDER,
                            this,
                            null
                        );
                    }
                }
                Log.info(
                    this,
                    " Attempting move from " +
                        this.unit.getLocation() +
                        " to " +
                        dest +
                        " along path " +
                        this.lastPath +
                        " to " +
                        this.loc
                );
                resp = this.game.resolveMove(this.unit, dest);
                if (resp == ResponseCode.STEP_COMPLETE) {
                    continue;
                } else if (resp == ResponseCode.TURN_COMPLETE) {
                    return new OrderResponse(resp, this, null);
                } else if (resp == ResponseCode.DIED) {
                    return new OrderResponse(resp, this, null);
                } else if (resp == ResponseCode.YIELD_PASS) {
                    // Mark this location as obstructed so we try alternate path next time
                    this.unit.turn().addObstruction(dest);
                    this.lastPath = null; // Clear path to force recalculation
                    return new OrderResponse(resp, this, null);
                } else {
                    Log.warn(
                        this.unit,
                        "Move CANCELLED: unexpected response code " +
                            resp +
                            " while moving to " +
                            dest
                    );
                    resp = ResponseCode.CANCEL_ORDER;
                    return new OrderResponse(resp, this, null);
                }
            }

            if (dest == this.loc) {
                resp = ResponseCode.ORDER_AND_TURN_COMPLETE;
                // Auto-sleep if destination reached (for land units without further orders)
                autoSleepIfAppropriate();
            } else {
                resp = ResponseCode.TURN_COMPLETE;
            }
            return new OrderResponse(resp, this, null);
        } else {
            Log.info(
                this.unit,
                "Attempting move from " +
                    this.unit.getLocation() +
                    " to " +
                    dest
            );
            resp = this.game.resolveMove(this.unit, dest);
            if (resp == ResponseCode.TURN_COMPLETE) {
                Log.info(this.unit, "Unit reports turn complete");
            } else if (resp == ResponseCode.DIED) {
                Log.info(this.unit, "Unit died during move");
            } else if (resp == ResponseCode.CANCEL_ORDER) {
                Log.warn(this.unit, "Move CANCELLED by resolveMove");
            } else if (resp != ResponseCode.STEP_COMPLETE) {
                Log.warn(
                    this.unit,
                    "Move returned unexpected response: " + resp
                );
            }
            if (this.unit.life().movesLeft() > 0) {
                resp = ResponseCode.ORDER_COMPLETE;
                // Auto-sleep if destination reached (for land units without further orders)
                autoSleepIfAppropriate();
            } else {
                resp = ResponseCode.ORDER_AND_TURN_COMPLETE;
                // Auto-sleep if destination reached (for land units without further orders)
                autoSleepIfAppropriate();
            }

            return new OrderResponse(resp, this, null);
        }
    }

    /**
     * Automatically put the unit to sleep if appropriate:
     * Only when a unit reaches an owned city
     */
    private void autoSleepIfAppropriate() {
        // Only apply to land units
        if (this.unit.getTravel() != Travel.LAND) {
            return;
        }

        // Check if at destination
        if (!this.unit.getLocation().equals(this.loc)) {
            return;
        }

        // Check if at an owned city - only sleep if at a city
        City city = this.game.getBoard().getCity(this.unit.getLocation());
        if (city != null && city.getOwner() == this.unit.getOwner()) {
            Log.info(
                this.unit,
                "Auto-sleeping: reached owned city at destination"
            );
            this.unit.orderSentry();
        }
    }

    //  public boolean complete() {
    //    return this.unit.getLocation().equals(this.loc);
    //  }
}
