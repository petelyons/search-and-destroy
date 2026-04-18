package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class FleetTest {

    @Test
    public void flagshipIsTaggedFlagshipRole() {
        Player owner = new Robot("A", 1);
        Unit flagship = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Fleet fleet = new Fleet(42L, owner, flagship);
        assertEquals(flagship, fleet.getFlagship());
        assertEquals(FleetRole.FLAGSHIP, fleet.roleOf(flagship));
        assertEquals(1, fleet.size());
    }

    @Test
    public void capitalShipHelperDetectsBattleshipAndCarrier() {
        assertTrue(Type.BATTLESHIP.isCapitalShip());
        assertTrue(Type.CARRIER.isCapitalShip());
        assertFalse(Type.DESTROYER.isCapitalShip());
        assertFalse(Type.INFANTRY.isCapitalShip());
    }

    @Test
    public void rejectsNonCapitalFlagship() {
        Player owner = new Robot("A", 1);
        Unit dd = TestUnits.stubUnit(Type.DESTROYER, owner);
        assertThrows(SaDException.class, () -> new Fleet(1L, owner, dd));
    }
}
