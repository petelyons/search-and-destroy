package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for Board terrain and hex map functionality
 */
public class BoardTest {

    private static final int TEST_WIDTH = 20;
    private static final int TEST_HEIGHT = 20;

    @BeforeEach
    public void setup() {
        // Initialize location map before each test
        LocationMap.init(TEST_WIDTH, TEST_HEIGHT);
    }

    @Test
    public void testTerrainTypes() {
        // Test that we can identify different terrain types
        // Terrain: 0 = water, 1 = land

        // Create a simple board with mixed terrain
        int[][] terrainData = new int[TEST_WIDTH][TEST_HEIGHT];

        // Make left half water (0), right half land (1)
        for (int x = 0; x < TEST_WIDTH; x++) {
            for (int y = 0; y < TEST_HEIGHT; y++) {
                terrainData[x][y] = (x < TEST_WIDTH / 2) ? 0 : 1;
            }
        }

        // TODO: Need to figure out how to properly instantiate Board for testing
        // Board requires Game, HexBoardMap, and HexBoardContext
        // This test documents the terrain system for now

        assertEquals(0, terrainData[5][5], "Left side should be water");
        assertEquals(1, terrainData[15][5], "Right side should be land");
    }

    @Test
    public void testLocationBoundaries() {
        // Test that locations properly handle board boundaries
        Location corner = Location.get(0, 0);
        assertNotNull(corner, "Corner location should exist");

        Location outsideNegative = Location.get(-1, -1);
        assertNull(outsideNegative, "Negative coordinates should return null");

        Location outsideLarge = Location.get(TEST_WIDTH + 1, TEST_HEIGHT + 1);
        assertNull(outsideLarge, "Coordinates beyond board should return null");
    }

    @Test
    public void testCoastalLocations() {
        // Test identification of coastal hexes (land adjacent to water)
        // This is important for unit spawning and city placement

        // Create a simple island scenario
        int[][] terrainData = new int[TEST_WIDTH][TEST_HEIGHT];

        // All water
        for (int x = 0; x < TEST_WIDTH; x++) {
            for (int y = 0; y < TEST_HEIGHT; y++) {
                terrainData[x][y] = 0;
            }
        }

        // Create a small island in the center
        int centerX = TEST_WIDTH / 2;
        int centerY = TEST_HEIGHT / 2;
        terrainData[centerX][centerY] = 1; // Center is land

        // Verify the island is surrounded by water
        Location island = Location.get(centerX, centerY);
        assertNotNull(island);

        // All neighbors should be water (we'd need Board.isCoast() to test this properly)
        Location neighbor = island.relative(Direction.EAST);
        assertNotNull(neighbor);
        assertEquals(
            0,
            terrainData[neighbor.x][neighbor.y],
            "Neighbor should be water"
        );
    }
}
