package com.developingstorm.games.hexboard;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test suite for hex Location coordinate system.
 * Tests the offset coordinate system where odd and even rows behave differently.
 */
public class LocationTest {

    @BeforeAll
    public static void setup() {
        // Initialize the location map for testing with a reasonable size
        LocationMap.init(50, 50);
    }

    @Test
    public void testLocationCreation() {
        Location loc = Location.get(5, 5);
        assertNotNull(loc, "Location should be created");
        assertEquals(5, loc.x, "X coordinate should match");
        assertEquals(5, loc.y, "Y coordinate should match");
    }

    @Test
    public void testLocationSingleton() {
        // Location uses flyweight pattern - same coordinates should return same instance
        Location loc1 = Location.get(3, 4);
        Location loc2 = Location.get(3, 4);
        assertSame(
            loc1,
            loc2,
            "Same coordinates should return same Location instance"
        );
    }

    @Test
    public void testEvenRowNeighbors() {
        // In even rows (y=0, 2, 4, ...), the hex neighbors work differently
        Location center = Location.get(5, 2); // Even row

        // Test EAST and WEST (these work the same for all rows)
        Location east = center.relative(Direction.EAST);
        assertNotNull(east);
        assertEquals(6, east.x);
        assertEquals(2, east.y);

        Location west = center.relative(Direction.WEST);
        assertNotNull(west);
        assertEquals(4, west.x);
        assertEquals(2, west.y);

        // Test diagonal directions for even rows
        // In even rows, NW and SW shift left (x-1), NE and SE stay same x
        Location nw = center.relative(Direction.NORTH_WEST);
        assertNotNull(nw);
        assertEquals(4, nw.x, "NW in even row should be x-1");
        assertEquals(1, nw.y, "NW should be y-1");

        Location ne = center.relative(Direction.NORTH_EAST);
        assertNotNull(ne);
        assertEquals(5, ne.x, "NE in even row should be same x");
        assertEquals(1, ne.y, "NE should be y-1");

        Location sw = center.relative(Direction.SOUTH_WEST);
        assertNotNull(sw);
        assertEquals(4, sw.x, "SW in even row should be x-1");
        assertEquals(3, sw.y, "SW should be y+1");

        Location se = center.relative(Direction.SOUTH_EAST);
        assertNotNull(se);
        assertEquals(5, se.x, "SE in even row should be same x");
        assertEquals(3, se.y, "SE should be y+1");
    }

    @Test
    public void testOddRowNeighbors() {
        // In odd rows (y=1, 3, 5, ...), the hex neighbors work differently
        Location center = Location.get(5, 3); // Odd row

        // Test EAST and WEST (same as even rows)
        Location east = center.relative(Direction.EAST);
        assertNotNull(east);
        assertEquals(6, east.x);
        assertEquals(3, east.y);

        Location west = center.relative(Direction.WEST);
        assertNotNull(west);
        assertEquals(4, west.x);
        assertEquals(3, west.y);

        // Test diagonal directions for odd rows
        // In odd rows, NW and SW stay same x, NE and SE shift right (x+1)
        Location nw = center.relative(Direction.NORTH_WEST);
        assertNotNull(nw);
        assertEquals(5, nw.x, "NW in odd row should be same x");
        assertEquals(2, nw.y, "NW should be y-1");

        Location ne = center.relative(Direction.NORTH_EAST);
        assertNotNull(ne);
        assertEquals(6, ne.x, "NE in odd row should be x+1");
        assertEquals(2, ne.y, "NE should be y-1");

        Location sw = center.relative(Direction.SOUTH_WEST);
        assertNotNull(sw);
        assertEquals(5, sw.x, "SW in odd row should be same x");
        assertEquals(4, sw.y, "SW should be y+1");

        Location se = center.relative(Direction.SOUTH_EAST);
        assertNotNull(se);
        assertEquals(6, se.x, "SE in odd row should be x+1");
        assertEquals(4, se.y, "SE should be y+1");
    }

    @Test
    public void testMultipleSteps() {
        Location start = Location.get(5, 5);

        // Move 3 steps east
        Location result = start.relative(Direction.EAST, 3);
        assertNotNull(result);
        assertEquals(8, result.x);
        assertEquals(5, result.y);

        // Move 2 steps west
        result = start.relative(Direction.WEST, 2);
        assertNotNull(result);
        assertEquals(3, result.x);
        assertEquals(5, result.y);
    }

    @Test
    public void testEquality() {
        Location loc1 = Location.get(3, 4);
        Location loc2 = Location.get(3, 4);
        Location loc3 = Location.get(3, 5);

        assertEquals(loc1, loc2, "Same coordinates should be equal");
        assertNotEquals(
            loc1,
            loc3,
            "Different coordinates should not be equal"
        );
    }

    @Test
    public void testHashCode() {
        Location loc1 = Location.get(3, 4);
        Location loc2 = Location.get(3, 4);

        assertEquals(
            loc1.hashCode(),
            loc2.hashCode(),
            "Equal locations should have same hash code"
        );
    }

    @Test
    public void testHexDistance() {
        Location start = Location.get(0, 0);
        Location end = Location.get(3, 0);

        // Direct east movement - distance should be 3
        int distance = start.distance(end);
        assertEquals(3, distance, "Horizontal distance should be correct");
    }

    @Test
    public void testNeighborSymmetry() {
        // Test that moving in opposite directions cancels out
        Location start = Location.get(10, 10);

        // Go east then west
        Location east = start.relative(Direction.EAST);
        Location back = east.relative(Direction.WEST);
        assertEquals(start, back, "East then west should return to start");

        // Go NE then SW (should work for odd row)
        Location ne = start.relative(Direction.NORTH_EAST);
        assertNotNull(ne);
        Location backToStart = ne.relative(Direction.SOUTH_WEST);
        assertEquals(start, backToStart, "NE then SW should return to start");
    }

    @Test
    public void testInvalidDirectionReturnsNull() {
        Location loc = Location.get(5, 5);

        // relative(Direction) returns null for invalid/null direction
        Location result = loc.relative(null);
        assertNull(result, "Null direction should return null");
    }

    @Test
    public void testInvalidDirectionThrowsException() {
        Location loc = Location.get(5, 5);

        // relative(Direction, int) throws exception for null direction
        assertThrows(
            IllegalArgumentException.class,
            () -> {
                loc.relative(null, 1);
            },
            "Null direction with distance should throw IllegalArgumentException"
        );
    }

    @Test
    public void testInvalidDistanceThrowsException() {
        Location loc = Location.get(5, 5);

        assertThrows(
            IllegalArgumentException.class,
            () -> {
                loc.relative(Direction.EAST, 0);
            },
            "Zero distance should throw IllegalArgumentException"
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> {
                loc.relative(Direction.EAST, -1);
            },
            "Negative distance should throw IllegalArgumentException"
        );
    }
}
