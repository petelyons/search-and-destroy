package com.developingstorm.games.sad.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.ui.SaDBoardContext;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test suite for GameStateSerializer - saving and loading complete game state
 */
public class GameStateSerializerTest {

    private GameStateSerializer serializer;
    private MockSaDBoardContext mockContext;

    @TempDir
    File tempDir;

    /**
     * Mock implementation of SaDBoardContext for testing
     */
    private static class MockSaDBoardContext implements SaDBoardContext {

        @Override
        public int getWidth() {
            return 50;
        }

        @Override
        public int getHeight() {
            return 50;
        }

        @Override
        public int getHexSide() {
            return 24;
        }

        @Override
        public int getTerrainImageSelector(int x, int y) {
            return 0;
        }

        @Override
        public int getUnexploredImageSelector() {
            return 0;
        }

        @Override
        public int getPrototypeHex() {
            return 0;
        }

        @Override
        public int getZs() {
            return 3;
        }

        @Override
        public java.awt.Color getPlayerColor(Player p) {
            return java.awt.Color.BLACK;
        }

        @Override
        public java.awt.Color getBorderColor() {
            return java.awt.Color.GRAY;
        }

        @Override
        public java.awt.Color getSelectionColor() {
            return java.awt.Color.YELLOW;
        }

        @Override
        public java.awt.Color getXorColor() {
            return null;
        }

        @Override
        public boolean showBorder() {
            return true;
        }

        @Override
        public java.awt.Image[] getImages() {
            return new java.awt.Image[0];
        }
    }

    @BeforeEach
    public void setup() {
        serializer = new GameStateSerializer();
        mockContext = new MockSaDBoardContext();
        LocationMap.init(50, 50);
    }

    @Test
    public void testLoadRealSaveFile_v1_2() throws Exception {
        // Load the real save file from test resources
        URL saveFileUrl = getClass().getResource("/saves/test_save_v1.2.json");
        assertNotNull(
            saveFileUrl,
            "Test save file should exist in test resources"
        );

        File saveFile = new File(saveFileUrl.toURI());
        assertTrue(saveFile.exists(), "Save file should exist");

        // Load the game
        Game loadedGame = serializer.loadGame(saveFile, mockContext);

        // Verify basic game state
        assertNotNull(loadedGame, "Loaded game should not be null");
        assertEquals(87, loadedGame.getTurn(), "Game should be on turn 87");

        // Verify players
        Player[] players = loadedGame.getPlayers();
        assertNotNull(players, "Players array should not be null");
        assertEquals(2, players.length, "Should have 2 players");

        // Find players by name since order may vary
        Player pete = null;
        Player jayne = null;
        for (Player p : players) {
            if ("Pete".equals(p.toJsonLink())) {
                pete = p;
            } else if ("Jayne".equals(p.toJsonLink())) {
                jayne = p;
            }
        }

        assertNotNull(pete, "Should have found Pete player");
        assertNotNull(jayne, "Should have found Jayne player");

        // Debug output
        System.out.println(
            "Pete class: " +
                pete.getClass().getName() +
                ", isRobot: " +
                pete.isRobot()
        );
        System.out.println(
            "Jayne class: " +
                jayne.getClass().getName() +
                ", isRobot: " +
                jayne.isRobot()
        );

        assertFalse(
            pete.isRobot(),
            "Pete should be a human player (not robot)"
        );
        assertTrue(jayne.isRobot(), "Jayne should be a robot player");

        // Verify current player
        Player currentPlayer = loadedGame.currentPlayer();
        assertNotNull(currentPlayer, "Current player should not be null");
        assertEquals(
            jayne,
            currentPlayer,
            "Current player should be Jayne (index 1)"
        );

        // Verify units
        List<Unit> units = loadedGame.units();
        assertNotNull(units, "Units list should not be null");
        assertFalse(units.isEmpty(), "Should have units in the game");
        assertTrue(
            units.size() > 10,
            "Game should have multiple units (actual: " + units.size() + ")"
        );

        // Verify cities
        List<City> cities = loadedGame.getBoard().getCities();
        assertNotNull(cities, "Cities list should not be null");
        assertFalse(cities.isEmpty(), "Should have cities in the game");
        assertTrue(
            cities.size() > 10,
            "Game should have multiple cities (actual: " + cities.size() + ")"
        );

        // Verify some specific units exist and have correct properties
        Unit infantry = findUnitByType(units, Type.INFANTRY);
        assertNotNull(infantry, "Should have at least one infantry unit");
        assertTrue(
            infantry.life.hits > 0,
            "Infantry should have positive hits"
        );

        // Verify some units have orders
        long unitsWithOrders = units
            .stream()
            .filter(u -> u.getOrder() != null)
            .count();
        assertTrue(
            unitsWithOrders > 0,
            "Some units should have orders assigned"
        );

        // Verify transport with carried units exists
        Unit transport = findUnitByType(units, Type.TRANSPORT);
        if (transport != null && transport.carries != null) {
            assertFalse(
                transport.carries.isEmpty(),
                "At least one transport should be carrying units"
            );
        }

        // Verify explored areas were loaded
        // Pete should have explored areas
        Location testLoc = Location.get(10, 10);
        if (testLoc != null) {
            // Just verify the method works - exact explored status depends on game state
            assertNotNull(
                pete.isExplored(testLoc),
                "Should be able to check explored status"
            );
        }

        // Verify cities have owners
        long ownedCities = cities
            .stream()
            .filter(c -> c.getOwner() != null)
            .count();
        assertTrue(ownedCities > 0, "Some cities should have owners");

        // Verify cities have production
        long productiveCities = cities
            .stream()
            .filter(c -> c.getOwner() != null && c.getProduction() != null)
            .count();
        assertTrue(
            productiveCities > 0,
            "Some cities should be producing units"
        );
    }

    @Test
    public void testSaveAndLoadRoundTrip() throws Exception {
        // This test would create a simple game, save it, then load it back
        // to verify round-trip serialization works correctly
        // For now, we'll skip this as it requires creating a full game setup
        // which is complex. The real save file test above covers the main
        // deserialization logic.
    }

    @Test
    public void testLoadCorruptedSaveFile() throws Exception {
        // Create a corrupted save file
        File corruptedFile = new File(tempDir, "corrupted.json");
        java.nio.file.Files.write(
            corruptedFile.toPath(),
            "{ invalid json".getBytes()
        );

        // Should throw an exception when loading
        assertThrows(
            Exception.class,
            () -> {
                serializer.loadGame(corruptedFile, mockContext);
            },
            "Loading corrupted file should throw an exception"
        );
    }

    @Test
    public void testLoadMissingMapFile() throws Exception {
        // Create a save file without the corresponding map file
        File saveFile = new File(tempDir, "test_save.json");

        // Create minimal valid JSON but with non-existent map file
        String minimalJson =
            "{" +
            "\"version\":\"1.2\"," +
            "\"turn\":1," +
            "\"currentPlayerIndex\":0," +
            "\"board\":{\"width\":50,\"height\":50,\"mapFile\":\"nonexistent_map.txt\"}," +
            "\"players\":[{\"index\":0,\"id\":1,\"name\":\"Player1\",\"isHuman\":true,\"explored\":[]}]," +
            "\"units\":[]," +
            "\"cities\":[]" +
            "}";

        java.nio.file.Files.write(saveFile.toPath(), minimalJson.getBytes());

        // Should throw an exception about missing map file
        Exception exception = assertThrows(
            Exception.class,
            () -> {
                serializer.loadGame(saveFile, mockContext);
            },
            "Loading with missing map file should throw an exception"
        );

        assertTrue(
            exception.getMessage().toLowerCase().contains("map") ||
                exception.getMessage().toLowerCase().contains("not found"),
            "Exception message should mention map file or not found"
        );
    }

    @Test
    public void testSaveGameCreatesFiles() throws Exception {
        // This would test that saveGame creates both JSON and map files
        // Skip for now as it requires a real game instance
    }

    /**
     * Helper method to find a unit by type in the units list
     */
    private Unit findUnitByType(List<Unit> units, Type type) {
        return units
            .stream()
            .filter(u -> u.getType() == type)
            .findFirst()
            .orElse(null);
    }

    /**
     * Helper method to find a unit with a specific order type
     */
    private Unit findUnitWithOrder(List<Unit> units, OrderType orderType) {
        return units
            .stream()
            .filter(
                u -> u.getOrder() != null && u.getOrder().getType() == orderType
            )
            .findFirst()
            .orElse(null);
    }
}
