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

    @Test
    public void removingFlagshipClearsFlagshipReference() {
        Player owner = new Robot("A", 1);
        Unit flagship = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Fleet fleet = new Fleet(1L, owner, flagship);
        fleet.removeMember(flagship);
        assertNull(fleet.getFlagship());
        assertFalse(fleet.contains(flagship));
    }

    @Test
    public void addMemberRejectsFlagshipRole() {
        Player owner = new Robot("A", 1);
        Unit flagship = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Unit dd = TestUnits.stubUnit(Type.DESTROYER, owner);
        Fleet fleet = new Fleet(1L, owner, flagship);
        assertThrows(SaDException.class, () -> fleet.addMember(dd, FleetRole.FLAGSHIP));
    }
}
