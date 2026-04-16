package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContactMapTest {

    private ContactMap map;

    @BeforeEach
    public void setup() {
        LocationMap.init(20, 20);
        map = new ContactMap();
    }

    @Test
    public void testUpdateInsertsNewContact() {
        Location loc = Location.get(5, 5);
        map.updateContact(
            /* unitId= */ 1L, loc, Type.DESTROYER, Travel.SEA, /* owner= */ null,
            /* identified= */ true, /* spotterQuality= */ 1, /* currentTurn= */ 5
        );
        Contact c = map.getContactAt(loc);
        assertNotNull(c);
        assertEquals(Type.DESTROYER, c.type);
        assertTrue(c.identified);
        assertEquals(1, c.spotterQuality);
    }

    @Test
    public void testUpdateUpgradesToIdentified() {
        Location loc = Location.get(5, 5);
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 0, 5);
        map.updateContact(1L, loc, Type.CRUISER, Travel.SEA, null, true, 2, 5);
        Contact c = map.getContactAt(loc);
        assertTrue(c.identified);
        assertEquals(Type.CRUISER, c.type);
        assertEquals(2, c.spotterQuality);
    }

    @Test
    public void testUpdateDoesNotDowngradeIdentified() {
        Location loc = Location.get(5, 5);
        // First: identified by a cruiser
        map.updateContact(1L, loc, Type.SUBMARINE, Travel.SEA, null, true, 2, 5);
        // Second: a distant fighter sees it unidentified
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 1, 5);
        Contact c = map.getContactAt(loc);
        assertTrue(c.identified);
        assertEquals(Type.SUBMARINE, c.type);
        // spotterQuality keeps the higher value seen (2, not overwritten by 1)
        assertEquals(2, c.spotterQuality);
    }

    @Test
    public void testUpdateKeepsHighestSpotterQuality() {
        Location loc = Location.get(5, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 0, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 2, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 1, 5);
        Contact c = map.getContactAt(loc);
        assertEquals(2, c.spotterQuality);
    }

    @Test
    public void testRefreshRemovesExpiredGhosts() {
        Location loc = Location.get(5, 5);
        // Low-quality ghost: maxAge 2
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 0, 5);

        // Age 2: still alive
        map.refreshContacts(7, id -> false);
        assertNotNull(map.getContactAt(loc));

        // Age 3: expired
        map.refreshContacts(8, id -> false);
        assertNull(map.getContactAt(loc));
    }

    @Test
    public void testRefreshRemovesContactsForDeadUnits() {
        Location loc = Location.get(5, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 1, 5);
        // Caller signals unit 1 is dead
        map.refreshContacts(5, id -> id == 1L);
        assertNull(map.getContactAt(loc));
    }

    @Test
    public void testGetContactAtPrefersIdentified() {
        Location loc = Location.get(5, 5);
        // Unit 1 unidentified sighting + Unit 2 identified sighting at same hex.
        // (Multiple enemy units at same hex is rare but possible — e.g. carrier
        // with fighter on board via different detection events.)
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 0, 5);
        map.updateContact(2L, loc, Type.DESTROYER, Travel.SEA, null, true, 1, 5);
        Contact c = map.getContactAt(loc);
        assertEquals(2L, c.unitId, "identified contact wins");
    }

    @Test
    public void testGetContactAtTiebreakByRemainingLife() {
        Location loc = Location.get(5, 5);
        // Two unidentified ghosts, different spotterQuality => different maxAge
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 0, 5); // maxAge 2
        map.updateContact(2L, loc, null, Travel.SEA, null, false, 2, 5); // maxAge 4
        Contact c = map.getContactAt(loc);
        assertEquals(2L, c.unitId, "longer remaining life wins");
    }

    @Test
    public void testGetAllAndGetGhostsAndGetCurrent() {
        Location l1 = Location.get(1, 1);
        Location l2 = Location.get(2, 2);
        map.updateContact(1L, l1, Type.INFANTRY, Travel.LAND, null, true, 0, 10);
        map.updateContact(2L, l2, Type.DESTROYER, Travel.SEA, null, true, 1, 9);

        Collection<Contact> all = map.getAllContacts();
        assertEquals(2, all.size());

        Collection<Contact> current = map.getCurrentContacts(10);
        assertEquals(1, current.size());
        assertEquals(1L, current.iterator().next().unitId);

        Collection<Contact> ghosts = map.getGhostContacts(10);
        assertEquals(1, ghosts.size());
        assertEquals(2L, ghosts.iterator().next().unitId);
    }

    @Test
    public void testRemoveContact() {
        Location loc = Location.get(5, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 1, 5);
        map.removeContact(1L);
        assertNull(map.getContactAt(loc));
    }
}
