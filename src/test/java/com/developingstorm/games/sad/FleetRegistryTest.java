package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FleetRegistryTest {

    private Player owner;
    private FleetRegistry registry;

    @BeforeEach
    public void setup() {
        owner = new Robot("A", 1);
        registry = new FleetRegistry();
    }

    @Test
    public void formAssignsRolesAndBacklinksUnits() {
        Unit bb = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Unit dd = TestUnits.stubUnit(Type.DESTROYER, owner);
        Unit tr = TestUnits.stubUnit(Type.TRANSPORT, owner);
        Fleet f = registry.form(bb, List.of(dd, tr));
        assertEquals(FleetRole.FLAGSHIP, f.roleOf(bb));
        assertEquals(FleetRole.SCREEN,   f.roleOf(dd));
        assertEquals(FleetRole.TRANSPORT, f.roleOf(tr));
        assertSame(f, bb.getFleet());
        assertSame(f, dd.getFleet());
        assertSame(f, tr.getFleet());
    }

    @Test
    public void cannotFormWithWrongFlagshipType() {
        Unit dd = TestUnits.stubUnit(Type.DESTROYER, owner);
        assertThrows(SaDException.class, () -> registry.form(dd, List.of()));
    }

    @Test
    public void cannotAddUnitAlreadyInAnotherFleet() {
        Unit bb1 = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Unit bb2 = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Unit dd = TestUnits.stubUnit(Type.DESTROYER, owner);
        Fleet f1 = registry.form(bb1, List.of(dd));
        Fleet f2 = registry.form(bb2, List.of());
        assertThrows(SaDException.class, () -> registry.add(f2, dd));
    }

    @Test
    public void flagTransfersToNextCapitalWhenFlagshipLeaves() {
        Unit bb = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Unit cv = TestUnits.stubUnit(Type.CARRIER, owner);
        Unit dd = TestUnits.stubUnit(Type.DESTROYER, owner);
        Fleet f = registry.form(bb, List.of(cv, dd));
        registry.leave(bb);
        assertEquals(cv, f.getFlagship());
        assertEquals(FleetRole.FLAGSHIP, f.roleOf(cv));
        assertEquals(FleetRole.SCREEN, f.roleOf(dd));
        assertNull(bb.getFleet());
    }

    @Test
    public void disbandsWhenNoCapitalRemains() {
        Unit bb = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Unit dd = TestUnits.stubUnit(Type.DESTROYER, owner);
        Fleet f = registry.form(bb, List.of(dd));
        registry.leave(bb);
        assertNull(f.getFlagship());
        assertNull(dd.getFleet());
        assertFalse(registry.allFleets().contains(f));
    }

    @Test
    public void onUnitDiedRemovesAndTransfersFlag() {
        Unit bb = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Unit cv = TestUnits.stubUnit(Type.CARRIER, owner);
        Fleet f = registry.form(bb, List.of(cv));
        registry.onUnitDied(bb);
        assertEquals(cv, f.getFlagship());
        assertNull(bb.getFleet());
    }
}
