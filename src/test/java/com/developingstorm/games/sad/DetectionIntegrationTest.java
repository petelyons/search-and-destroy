package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.brain.RobotBrain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests covering Player.scanForContacts — the two-ring radial
 * detection pass introduced in Task 5 of the visibility overhaul.
 */
public class DetectionIntegrationTest {

    private static final int MAP_W = 50;
    private static final int MAP_H = 50;

    private HexBoardMap map;
    private HexBoardContext ctx;
    private Robot playerA; // "us"
    private Robot playerB; // "them"
    private Game game;

    /**
     * Mock implementation of HexBoardContext for testing (copied from
     * GameStateSerializerTest).
     */
    private static class MockSaDBoardContext implements HexBoardContext {

        @Override
        public int getWidth() {
            return MAP_W;
        }

        @Override
        public int getHeight() {
            return MAP_H;
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
        LocationMap.init(MAP_W, MAP_H);
        // Use the real map so that Game construction (which assigns coastal
        // cities to players) succeeds. createUnit does not validate terrain,
        // so we can place our test units on any in-bounds hex afterwards.
        map = HexBoardMap.loadMapAsResource(this, "MedMap.sdm");
        ctx = new MockSaDBoardContext();

        playerA = new Robot("A", 1);
        playerA.setBrain(new RobotBrain(playerA));
        playerB = new Robot("B", 2);
        playerB.setBrain(new RobotBrain(playerB));

        game = new Game(new Player[] { playerA, playerB }, map, ctx);
    }

    private Unit spawn(Type type, Player owner, Location loc) {
        Unit u = game.createUnit(type, owner, loc);
        owner.addUnit(u);
        return u;
    }

    private Contact contactFor(Player viewer, Unit enemy) {
        for (Contact c : viewer.getAllContacts()) {
            if (c.unitId == enemy.id) return c;
        }
        return null;
    }

    /**
     * Destroyer (COMPLETE vision, detectionRange=3, identificationRange=3)
     * at distance 3 from an enemy submarine should detect AND identify the
     * sub (auto-ID under COMPLETE vision).
     */
    @Test
    public void testDestroyerDetectsSubmarineAtRange3() {
        Location destLoc = Location.get(10, 10);
        Location subLoc = Location.get(13, 10);
        assertEquals(3, destLoc.distance(subLoc), "setup: distance must be 3");

        spawn(Type.DESTROYER, playerA, destLoc);
        Unit sub = spawn(Type.SUBMARINE, playerB, subLoc);

        playerA.startNewTurn();

        Contact c = contactFor(playerA, sub);
        assertNotNull(c, "destroyer should detect submarine at range 3");
        assertTrue(c.identified, "submarine should be auto-ID'd under COMPLETE vision");
        assertEquals(Type.SUBMARINE, c.type);
    }

    /**
     * Fighter (detectionRange=4, identificationRange=2) at distance 3 from
     * an enemy infantry: contact exists but is NOT identified. Travel class
     * should still be known.
     */
    @Test
    public void testFighterDetectsFarIdentifiesClose() {
        Location fighterLoc = Location.get(10, 10);
        Location infLoc = Location.get(13, 10);
        assertEquals(3, fighterLoc.distance(infLoc), "setup: distance must be 3");

        spawn(Type.FIGHTER, playerA, fighterLoc);
        Unit inf = spawn(Type.INFANTRY, playerB, infLoc);

        playerA.startNewTurn();

        Contact c = contactFor(playerA, inf);
        assertNotNull(c, "fighter should detect infantry at range 3");
        assertFalse(c.identified, "infantry at range 3 should NOT be identified (idRange=2)");
        assertEquals(Travel.LAND, c.travelType, "travel class should be known even when unidentified");
    }

    /**
     * After the sub is destroyed, the next refresh should drop the contact
     * (dead-unit removal during refreshContacts).
     */
    @Test
    public void testContactRemovedAfterEnemyDies() {
        Location destLoc = Location.get(10, 10);
        Location subLoc = Location.get(12, 10);
        assertEquals(2, destLoc.distance(subLoc), "setup: distance must be 2");

        spawn(Type.DESTROYER, playerA, destLoc);
        Unit enemySub = spawn(Type.SUBMARINE, playerB, subLoc);

        playerA.startNewTurn();
        assertNotNull(contactFor(playerA, enemySub), "baseline: sub should be detected");

        game.killUnit(enemySub);
        playerA.startNewTurn();

        assertNull(contactFor(playerA, enemySub), "contact should be removed after enemy dies");
    }

    /**
     * An infantry unit adjacent to an enemy surface ship must detect it
     * (SURFACE vision + detectionRange=1 means range-1 sightings succeed).
     */
    @Test
    public void testInfantryDetectsAdjacentDestroyer() {
        Location infLoc = Location.get(10, 10);
        Location destLoc = Location.get(11, 10);
        assertEquals(1, infLoc.distance(destLoc), "setup: distance must be 1");

        spawn(Type.INFANTRY, playerA, infLoc);
        Unit dest = spawn(Type.DESTROYER, playerB, destLoc);

        playerA.startNewTurn();

        Contact c = contactFor(playerA, dest);
        assertNotNull(c, "infantry should detect adjacent enemy destroyer");
    }

    /**
     * Ghost state must refresh after every unit move, not only at turn start.
     * Starts with the destroyer out of detection range from the sub (so the
     * initial scan records nothing), then steps the destroyer one hex closer
     * into range and expects the contact to appear as current, not a ghost.
     */
    @Test
    public void testContactRefreshesAfterUnitMove() {
        Location destStart = Location.get(10, 10);
        Location destStep = Location.get(11, 10);
        Location subLoc = Location.get(14, 10);
        assertEquals(4, destStart.distance(subLoc), "setup: initial distance 4 (out of destroyer range 3)");
        assertEquals(3, destStep.distance(subLoc), "setup: post-move distance 3 (in range)");

        Unit dest = spawn(Type.DESTROYER, playerA, destStart);
        Unit sub = spawn(Type.SUBMARINE, playerB, subLoc);

        playerA.startNewTurn();
        assertNull(contactFor(playerA, sub), "baseline: sub should be out of detection range");

        dest.move(destStep);

        Contact c = contactFor(playerA, sub);
        assertNotNull(c, "sub should be detected mid-turn after destroyer moves into range");
        assertTrue(c.isCurrent(game.getTurn()), "contact should be current, not a ghost");
    }
}
