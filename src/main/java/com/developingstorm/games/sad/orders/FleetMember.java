package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Fleet;
import com.developingstorm.games.sad.FleetRole;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.ScreenFleetBehavior;
import com.developingstorm.games.sad.util.Log;

public class FleetMember extends Order {

    private final Fleet fleet;

    public FleetMember(Game g, Unit u, Fleet fleet) {
        super(g, u, OrderType.FLEET_MEMBER);
        if (fleet == null) throw new SaDException("FleetMember requires fleet");
        if (!fleet.contains(u)) {
            throw new SaDException("unit is not a member of the given fleet");
        }
        this.fleet = fleet;
    }

    public Fleet getFleet() { return fleet; }

    @Override
    protected OrderResponse executeInternal() {
        if (this.unit.getFleet() != fleet) {
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }
        Unit flagship = fleet.getFlagship();
        if (flagship == null || flagship.isDead()) {
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }
        FleetRole role = fleet.roleOf(this.unit);
        if (role == FleetRole.FLAGSHIP) {
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }
        Location target;
        if (role == FleetRole.SCREEN) {
            target = ScreenFleetBehavior.chooseMove(this.unit, fleet, this.game);
            if (target == null) target = flagship.getLocation();
        } else {
            target = flagship.getLocation();
        }
        return stepToward(target, flagship.getLocation());
    }

    private OrderResponse stepToward(Location target, Location flagshipLoc) {
        if (this.unit.getLocation().distance(flagshipLoc) <= 2
            && target.equals(flagshipLoc)) {
            return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }
        while (this.unit.life().movesLeft() > 0 && !this.unit.isDead()) {
            Path path = this.unit.getPath(target);
            if (path == null || path.isEmpty()) {
                return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
            }
            Location next = path.next(this.unit.getLocation());
            if (next == null) break;
            ResponseCode rc = this.game.resolveMove(this.unit, next, target);
            switch (rc) {
                case STEP_COMPLETE:
                    if (target.equals(flagshipLoc)
                        && this.unit.getLocation().distance(flagshipLoc) <= 2) {
                        return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
                    }
                    break;
                case TURN_COMPLETE:
                    return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
                case DIED:
                    return new OrderResponse(ResponseCode.DIED, this, null);
                case YIELD_PASS:
                    return new OrderResponse(ResponseCode.YIELD_PASS, this, null);
                default:
                    Log.warn(this.unit, "FleetMember unexpected rc: " + rc);
                    return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
            }
        }
        return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
    }
}
