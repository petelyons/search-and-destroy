package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
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
}
