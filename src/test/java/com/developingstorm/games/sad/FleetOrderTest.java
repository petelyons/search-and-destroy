package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.orders.FleetMember;
import org.junit.jupiter.api.Test;

public class FleetOrderTest {

    @Test
    public void fleetMemberOrderTypeIsFleetMember() {
        FleetHarness h = FleetHarness.withFlagshipAndTransport();
        FleetMember order = new FleetMember(h.game, h.transport, h.fleet);
        assertEquals(OrderType.FLEET_MEMBER, order.getType());
        assertSame(h.fleet, order.getFleet());
    }

    @Test
    public void transportStepsTowardFlagship() {
        FleetHarness h = FleetHarness.withFlagshipAndTransport();
        int before = h.transport.getLocation().distance(h.flagship.getLocation());
        h.transport.assignOrder(new FleetMember(h.game, h.transport, h.fleet));
        h.transport.execOrder(null);
        int after = h.transport.getLocation().distance(h.flagship.getLocation());
        assertTrue(after < before, "transport should close distance to flagship");
    }

    @Test
    public void followersStepAfterFlagshipStep() {
        FleetHarness h = FleetHarness.withFlagshipAndTransport();
        int before = h.transport.getLocation().distance(h.flagship.getLocation());
        h.transport.assignOrder(new FleetMember(h.game, h.transport, h.fleet));
        // Pick an adjacent water hex AWAY from the transport so distance grows
        // without the follower hook; with the hook, the follower closes by one.
        Location flagStart = h.flagship.getLocation();
        Location trLoc = h.transport.getLocation();
        Location target = null;
        int bestDist = -1;
        for (Location adj : flagStart.getRing(1)) {
            if (!h.game.getBoard().isWater(adj)) continue;
            int d = adj.distance(trLoc);
            if (d > bestDist) { bestDist = d; target = adj; }
        }
        assertNotNull(target, "need a water adjacency");
        h.flagship.move(target);
        int after = h.transport.getLocation().distance(h.flagship.getLocation());
        assertTrue(after <= before, "follower should not fall further behind");
    }
}
