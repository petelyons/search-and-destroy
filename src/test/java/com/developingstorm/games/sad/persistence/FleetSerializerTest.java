package com.developingstorm.games.sad.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Fleet;
import com.developingstorm.games.sad.FleetHarness;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.orders.FleetMember;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FleetSerializerTest {

    @TempDir
    File tempDir;

    @Test
    public void roundTripPreservesFleetAndMemberOrders() throws Exception {
        FleetHarness h = FleetHarness.withFlagshipAndTransport();
        h.transport.clearOrders();
        h.transport.assignOrder(new FleetMember(h.game, h.transport, h.fleet));

        long originalFleetId = h.fleet.getId();

        GameStateSerializer ser = new GameStateSerializer();
        File saveFile = new File(tempDir, "fleet_rt.sav");
        ser.saveGameToFile(h.game, saveFile);

        Game loaded = ser.loadGame(saveFile, new MockSaDBoardContext());

        assertEquals(1, loaded.getFleetRegistry().allFleets().size());
        Fleet f = loaded.getFleetRegistry().allFleets().iterator().next();
        assertEquals(originalFleetId, f.getId());
        assertNotNull(f.getFlagship());
        assertEquals(Type.BATTLESHIP, f.getFlagship().getType());

        Unit restoredTransport = null;
        for (Unit u : f.membersView().keySet()) {
            if (u.getType() == Type.TRANSPORT) {
                restoredTransport = u;
            }
        }
        assertNotNull(restoredTransport);
        assertSame(f, restoredTransport.getFleet());
        assertTrue(restoredTransport.getOrder() instanceof FleetMember);
        assertSame(f, ((FleetMember) restoredTransport.getOrder()).getFleet());
    }

    @Test
    public void roundTripPreservesFleetTransportWithCargo() throws Exception {
        FleetHarness h = FleetHarness.withFlagshipAndTransport();
        Location infLoc = null;
        outer:
        for (int x = 0; x < 50 && infLoc == null; x++) {
            for (int y = 0; y < 50; y++) {
                Location l = Location.get(x, y);
                if (!h.game.getBoard().isWater(l) && h.game.unitAtLocation(l) == null) {
                    infLoc = l;
                    break outer;
                }
            }
        }
        assertNotNull(infLoc, "need a land hex for infantry");
        Unit infantry = h.game.createUnit(Type.INFANTRY, h.owner, infLoc);
        h.owner.addUnit(infantry);
        h.transport.addCarried(infantry, false);

        h.transport.clearOrders();
        h.transport.assignOrder(new FleetMember(h.game, h.transport, h.fleet));

        GameStateSerializer ser = new GameStateSerializer();
        File saveFile = new File(tempDir, "fleet_cargo.sav");
        ser.saveGameToFile(h.game, saveFile);

        Game loaded = ser.loadGame(saveFile, new MockSaDBoardContext());

        Fleet f = loaded.getFleetRegistry().allFleets().iterator().next();
        Unit restoredTransport = null;
        for (Unit u : f.membersView().keySet()) {
            if (u.getType() == Type.TRANSPORT) restoredTransport = u;
        }
        assertNotNull(restoredTransport);
        assertNotNull(restoredTransport.carries);
        assertEquals(1, restoredTransport.carries.size());
        assertEquals(Type.INFANTRY, restoredTransport.carries.get(0).getType());
        assertTrue(restoredTransport.carries.get(0).isCarried());
    }
}
