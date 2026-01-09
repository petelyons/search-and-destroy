package com.developingstorm.games.hexboard;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for hex Direction enum
 */
public class DirectionTest {

    @Test
    public void testAllDirectionsExist() {
        // Hex grids have exactly 6 directions
        Direction[] directions = Direction.values();
        assertEquals(6, directions.length, "Hex grid should have exactly 6 directions");
    }

    @Test
    public void testDirectionNames() {
        // Verify all expected directions exist
        assertNotNull(Direction.valueOf("NORTH_WEST"));
        assertNotNull(Direction.valueOf("NORTH_EAST"));
        assertNotNull(Direction.valueOf("EAST"));
        assertNotNull(Direction.valueOf("SOUTH_EAST"));
        assertNotNull(Direction.valueOf("SOUTH_WEST"));
        assertNotNull(Direction.valueOf("WEST"));
    }

    @Test
    public void testToString() {
        assertEquals("NORTH_WEST", Direction.NORTH_WEST.toString());
        assertEquals("NORTH_EAST", Direction.NORTH_EAST.toString());
        assertEquals("EAST", Direction.EAST.toString());
        assertEquals("SOUTH_EAST", Direction.SOUTH_EAST.toString());
        assertEquals("SOUTH_WEST", Direction.SOUTH_WEST.toString());
        assertEquals("WEST", Direction.WEST.toString());
    }

    @Test
    public void testEnumOrdering() {
        // Verify the order of directions (clockwise from NW)
        Direction[] directions = Direction.values();
        assertEquals(Direction.NORTH_WEST, directions[0]);
        assertEquals(Direction.NORTH_EAST, directions[1]);
        assertEquals(Direction.EAST, directions[2]);
        assertEquals(Direction.SOUTH_EAST, directions[3]);
        assertEquals(Direction.SOUTH_WEST, directions[4]);
        assertEquals(Direction.WEST, directions[5]);
    }

    @Test
    public void testOppositeDirections() {
        // In a hex grid, opposite directions should be 3 positions apart
        Direction[] directions = Direction.values();

        // NORTH_WEST (0) opposite is SOUTH_EAST (3)
        assertEquals(Direction.SOUTH_EAST, directions[(0 + 3) % 6]);

        // NORTH_EAST (1) opposite is SOUTH_WEST (4)
        assertEquals(Direction.SOUTH_WEST, directions[(1 + 3) % 6]);

        // EAST (2) opposite is WEST (5)
        assertEquals(Direction.WEST, directions[(2 + 3) % 6]);
    }
}
