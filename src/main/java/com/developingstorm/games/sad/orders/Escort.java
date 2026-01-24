package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.games.sad.util.json.JsonObj;

/**
 * Escort order - a ship follows and stays within 1 move of another ship.
 * The escort attempts to maintain proximity to the escorted ship.
 *
 * Escort ends when:
 * - The escorted ship is killed
 * - The escort order is cleared
 * - The escorted ship is no longer owned by the same player
 */
public class Escort extends Order {

    private Unit escortedUnit;
    private Location lastKnownLocation;

    /**
     * Creates a new escort order.
     *
     * @param g The game
     * @param u The escorting unit
     * @param escorted The unit to escort
     */
    public Escort(Game g, Unit u, Unit escorted) {
        super(g, u, OrderType.ESCORT);
        if (escorted == null) {
            throw new SaDException("Cannot escort null unit");
        }

        if (u.equals(escorted)) {
            throw new SaDException("Unit cannot escort itself");
        }

        // Only sea units can escort
        if (u.getTravel() != Travel.SEA) {
            throw new SaDException("Only sea units can escort");
        }

        // Can only escort friendly units
        if (!u.getOwner().equals(escorted.getOwner())) {
            throw new SaDException("Can only escort friendly units");
        }

        this.escortedUnit = escorted;
        this.lastKnownLocation = escorted.getLocation();
    }

    public Unit getEscortedUnit() {
        return escortedUnit;
    }

    @Override
    public OrderResponse executeInternal() {
        // Check if escorted unit is still alive
        if (escortedUnit.isDead()) {
            Log.info(this.unit, "Escorted unit is dead, ending escort");
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }

        // Check if escorted unit is still owned by same player
        if (!this.unit.getOwner().equals(escortedUnit.getOwner())) {
            Log.info(
                this.unit,
                "Escorted unit changed ownership, ending escort"
            );
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }

        Location escortedLocation = escortedUnit.getLocation();
        Location myLocation = this.unit.getLocation();
        int distance = myLocation.distance(escortedLocation);

        // If within 1 hex, we're in good position
        if (distance <= 1) {
            // Update last known location
            lastKnownLocation = escortedLocation;

            // If we have moves left and escorted unit moved, we might need to follow
            if (
                this.unit.life().movesLeft() > 0 &&
                !escortedLocation.equals(lastKnownLocation)
            ) {
                // Escorted unit moved this turn, try to follow
                Log.debug(this.unit, "Escorted unit moved, following");
                return followEscortedUnit(escortedLocation);
            }

            // Stay in position
            Log.debug(this.unit, "In escort position, distance=" + distance);
            return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }

        // Need to move closer
        if (this.unit.life().movesLeft() <= 0) {
            // No moves left this turn
            lastKnownLocation = escortedLocation;
            Log.debug(
                this.unit,
                "No moves left, escort will catch up next turn"
            );
            return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }

        // Move toward escorted unit
        Log.debug(
            this.unit,
            "Moving to catch up with escorted unit, distance=" + distance
        );
        return followEscortedUnit(escortedLocation);
    }

    private OrderResponse followEscortedUnit(Location targetLocation) {
        // Find adjacent location that gets us closer
        Location myLocation = this.unit.getLocation();

        // Get path to escorted unit
        com.developingstorm.games.sad.Path path = this.unit.getPath(
            targetLocation
        );

        if (path == null || path.isEmpty()) {
            Log.warn(this.unit, "Cannot find path to escorted unit");
            return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }

        // Move one step toward escorted unit
        Location nextStep = path.next(myLocation);
        if (nextStep == null) {
            // Already at destination or adjacent
            lastKnownLocation = targetLocation;
            return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }

        ResponseCode moveResult = this.game.resolveMove(
            this.unit,
            nextStep,
            targetLocation
        );

        switch (moveResult) {
            case STEP_COMPLETE:
                // Continue moving if we have moves left and still too far
                if (
                    this.unit.life().movesLeft() > 0 &&
                    this.unit.getLocation().distance(targetLocation) > 1
                ) {
                    return executeInternal(); // Recursive call to continue moving
                }
                lastKnownLocation = targetLocation;
                return new OrderResponse(
                    ResponseCode.TURN_COMPLETE,
                    this,
                    null
                );
            case TURN_COMPLETE:
                lastKnownLocation = targetLocation;
                return new OrderResponse(
                    ResponseCode.TURN_COMPLETE,
                    this,
                    null
                );
            case DIED:
                return new OrderResponse(ResponseCode.DIED, this, null);
            case YIELD_PASS:
                // Path blocked, try again later
                Log.debug(this.unit, "Path blocked, yielding");
                return new OrderResponse(ResponseCode.YIELD_PASS, this, null);
            default:
                Log.warn(this.unit, "Unexpected move result: " + moveResult);
                return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }
    }

    public JsonObj toJson() {
        JsonObj json = new JsonObj();
        json.put("escortedUnitId", escortedUnit.id);
        json.put("lastKnownX", lastKnownLocation.x);
        json.put("lastKnownY", lastKnownLocation.y);
        return json;
    }

    @Override
    public String toString() {
        return "Escort " + escortedUnit;
    }
}
