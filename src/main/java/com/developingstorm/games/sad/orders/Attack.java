package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import java.util.List;

/**
 * Attack order for naval bombardment - allows battleships and cruisers
 * to attack enemy land units in adjacent hexes without moving.
 */
public class Attack extends Order {

    private Location targetLocation;

    public Attack(Game g, Unit u, Location targetLocation) {
        super(g, u, OrderType.ATTACK);
        this.targetLocation = targetLocation;
    }

    @Override
    protected OrderResponse executeInternal() {
        // Validate the attack is possible
        if (targetLocation == null) {
            throw new SaDException("No target location for attack");
        }

        // Check if unit can attack (must be battleship or cruiser)
        if (!this.unit.isBattleship() && !this.unit.isCruiser()) {
            Log.warn(this.unit, "Only battleships and cruisers can perform bombardment attacks");
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }

        // Check if target is adjacent
        int distance = this.unit.getLocation().distance(targetLocation);
        if (distance != 1) {
            Log.warn(this.unit, "Target must be in an adjacent hex for bombardment");
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }

        // Check if there are enemy units at the target location
        List<Unit> targetUnits = this.game.unitsAtLocation(targetLocation);
        if (targetUnits == null || targetUnits.isEmpty()) {
            Log.warn(this.unit, "No units at target location");
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }

        // Find an enemy land unit to attack
        Unit targetUnit = null;
        for (Unit u : targetUnits) {
            if (u.getOwner() != this.unit.getOwner() && u.getTravel() == com.developingstorm.games.sad.Travel.LAND) {
                targetUnit = u;
                break;
            }
        }

        if (targetUnit == null) {
            Log.warn(this.unit, "No enemy land units at target location");
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }

        // Perform the bombardment attack
        Log.info(this.unit, "Bombarding " + targetUnit + " at " + targetLocation);

        boolean attackWon = this.game.getCombatResolver().resolveUnitAttack(this.unit, targetUnit);

        if (attackWon) {
            Log.info(this.unit, "Bombardment successful - destroyed " + targetUnit);
            this.game.killUnit(targetUnit);
        } else {
            Log.info(this.unit, "Bombardment failed - unit destroyed");
            this.game.killUnit(this.unit);
            return new OrderResponse(ResponseCode.DIED, this, null);
        }

        // Attack completes the unit's turn
        return new OrderResponse(ResponseCode.ORDER_AND_TURN_COMPLETE, this, null);
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    @Override
    public String toString() {
        return "Attack(" + targetLocation + ")";
    }
}
