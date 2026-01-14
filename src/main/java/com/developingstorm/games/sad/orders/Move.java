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
import com.developingstorm.games.sad.util.Log;

/**

 *
 */
public class Move extends Order {

    protected Path lastPath = null;
    protected Location loc;

    public Move(Game g, Unit u, Location loc) {
        super(g, u, OrderType.MOVE);
        this.loc = loc;
    }

    protected Move(Game g, Unit u, OrderType t, Location loc) {
        super(g, u, t);
        this.loc = loc;
    }

    public OrderResponse executeInternal() {
        ResponseCode resp = ResponseCode.CANCEL_ORDER;

        if (loc == null) {
            throw new SaDException("No Move Order");
        }

        Location dest = loc;

        if (this.unit.getLocation().equals(dest)) {
            return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
        }

        if (this.unit.getLocation().distance(this.loc) > 1) {
            if (lastPath == null) {
                lastPath = this.unit.getPath(this.loc);
                if (lastPath == null || this.lastPath.isEmpty()) {
                    Log.error("No path available!!!");

                    if (MapState.isBlocked(this.loc)) {
                        Log.error("The destination is blocked. Invalid move!");
                    } else if (MapState.isBlocked(this.unit.getLocation())) {
                        Log.error(
                            "The starting location is blocked. Invalid move!"
                        );
                    }
                    resp = ResponseCode.CANCEL_ORDER;
                    return new OrderResponse(resp, this, null);
                }
            }

            while (this.unit.life().movesLeft() > 0 && !this.unit.isDead()) {
                dest = this.lastPath.next(this.unit.getLocation());
                if (dest == null) {
                    int finalMove = this.unit.getLocation().distance(this.loc);
                    if (finalMove == 1) {
                        dest = loc;
                    } else {
                        Log.error(this.unit, "Cannot find next move");
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
                    Log.debug("Converting response to CANCEL_ORDER:" + resp);
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
                Log.info(this.unit, "DIED DURING MOVE!");
            } else if (resp != ResponseCode.STEP_COMPLETE) {
                Log.info(this.unit, "Bad move:" + resp);
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
