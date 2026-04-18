package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.commands.LeaveFleetCommand;
import com.developingstorm.games.sad.orders.FleetMember;
import org.junit.jupiter.api.Test;

public class FleetCombatIntegrationTest {

    @Test
    public void flagshipDeathPromotesCarrier() {
        FleetHarness h = FleetHarness.withFlagshipAndTransport();
        Location cvLoc = pickEmptyWater(h);
        Unit cv = h.game.createUnit(Type.CARRIER, h.owner, cvLoc);
        h.owner.addUnit(cv);
        h.game.getFleetRegistry().add(h.fleet, cv);

        h.game.killUnit(h.flagship);

        assertTrue(h.flagship.isDead());
        assertNull(h.flagship.getFleet());
        assertEquals(cv, h.fleet.getFlagship());
        assertEquals(FleetRole.FLAGSHIP, h.fleet.roleOf(cv));
    }

    @Test
    public void lastCapitalDeathDisbandsFleetAndWakesMembers() {
        FleetHarness h = FleetHarness.withFlagshipAndTransport();
        h.transport.clearOrders();
        h.transport.assignOrder(new FleetMember(h.game, h.transport, h.fleet));
        Unit tr = h.transport;

        h.game.killUnit(h.flagship);

        assertTrue(h.game.getFleetRegistry().allFleets().isEmpty());
        assertNull(tr.getFleet());
        assertFalse(tr.getOrder() instanceof FleetMember,
            "surviving member should not retain FleetMember order after disband");
    }

    @Test
    public void leaveFleetClearsOrderAndWakesUnit() {
        FleetHarness h = FleetHarness.withFlagshipAndTransport();
        h.transport.clearOrders();
        h.transport.assignOrder(new FleetMember(h.game, h.transport, h.fleet));
        assertTrue(h.transport.getOrder() instanceof FleetMember);

        new LeaveFleetCommand(h.transport).execute(h.game);

        assertNull(h.transport.getFleet());
        assertFalse(h.transport.getOrder() instanceof FleetMember);
    }

    private static Location pickEmptyWater(FleetHarness h) {
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 50; y++) {
                Location l = Location.get(x, y);
                if (!h.game.getBoard().isWater(l)) continue;
                if (h.game.unitAtLocation(l) != null) continue;
                return l;
            }
        }
        throw new SaDException("no empty water hex available");
    }
}
