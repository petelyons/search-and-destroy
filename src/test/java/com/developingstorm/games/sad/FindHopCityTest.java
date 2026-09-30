package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.LocationMap;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FindHopCityTest {

    private static final int HOP = 8;

    @Test
    public void findsFirstHopWithoutRecursingForever() {
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        HexBoardContext ctx = new MockSaDBoardContext();
        Player human = new Player("Human", 1);
        Robot robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        Game game = new Game(new Player[] {human, robot}, map, ctx);

        // Own every city so cities chain together within hop range
        List<City> cities = game.getBoard().getCities();
        for (City c : cities) {
            if (c.getOwner() != human) {
                c.setOwner(human);
            }
        }

        for (City start : cities) {
            City hop = human.findHopCity(start, HOP);
            if (hop != null) {
                assertNotSame(start, hop, "hop should move somewhere");
                assertTrue(hop.isNear(start, HOP), "first hop must be within range");
                assertSame(human, hop.getOwner());
            }
        }
    }

    @Test
    public void nullStartReturnsNull() {
        Player human = new Player("Human", 1);
        assertNull(human.findHopCity(null, HOP));
    }
}
