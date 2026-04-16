package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContactTest {

    @BeforeEach
    public void setup() {
        LocationMap.init(20, 20);
    }

    @Test
    public void testCurrentContactNotExpired() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, Type.DESTROYER, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ true, /* spotterQuality= */ 1
        );
        assertTrue(c.isCurrent(10));
        assertFalse(c.isExpired(10));
        assertEquals(0, c.age(10));
    }

    @Test
    public void testGhostOneTurnOld() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, null, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ false, /* spotterQuality= */ 0
        );
        // Base duration 2 + spotter quality 0 = maxAge 2
        assertEquals(2, c.maxAge());
        assertFalse(c.isCurrent(11));
        assertFalse(c.isExpired(11));
        assertEquals(1, c.age(11));
    }

    @Test
    public void testGhostExpiresAfterMaxAge() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, null, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ false, /* spotterQuality= */ 0
        );
        // maxAge=2. age 2 is still alive, age 3 is expired.
        assertFalse(c.isExpired(12));
        assertTrue(c.isExpired(13));
    }

    @Test
    public void testHighQualitySpotterExtendsGhostLife() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, Type.CRUISER, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ true, /* spotterQuality= */ 2
        );
        // Base 2 + quality 2 = maxAge 4
        assertEquals(4, c.maxAge());
        assertFalse(c.isExpired(14));
        assertTrue(c.isExpired(15));
    }

    @Test
    public void testRemainingLife() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, null, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ false, /* spotterQuality= */ 1
        );
        // maxAge 3. At turn 10 remaining=3, at turn 12 remaining=1.
        assertEquals(3, c.remainingLife(10));
        assertEquals(1, c.remainingLife(12));
        assertEquals(0, c.remainingLife(13));
    }
}
