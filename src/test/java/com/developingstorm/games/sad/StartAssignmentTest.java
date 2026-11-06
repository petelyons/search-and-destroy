package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.mapgen.MapGenerator;
import com.developingstorm.games.sad.mapgen.MapSize;
import com.developingstorm.util.RandomUtil;
import org.junit.jupiter.api.Test;

public class StartAssignmentTest {

    static Game twoRobotGame(HexBoardMap map) {
        Robot a = new Robot("A", 1);
        a.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(a));
        Robot b = new Robot("B", 2);
        b.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(b));
        return new Game(new Player[] {a, b}, map, new MockSaDBoardContext());
    }

    @Test
    public void playersStartOnDifferentContinents() {
        for (long seed = 1; seed <= 30; seed++) {
            RandomUtil.setSeed(seed);
            Game game = twoRobotGame(MapGenerator.generate(MapSize.MEDIUM));
            City a = game.getPlayers()[0].getCities().get(0);
            City b = game.getPlayers()[1].getCities().get(0);
            assertTrue(a.isCoastal() && b.isCoastal(), "seed " + seed);
            assertNotEquals(a.getContinent(), b.getContinent(), "seed " + seed);
        }
    }

    @Test
    public void singleContinentFallsBack() {
        RandomUtil.setSeed(5);
        HexBoardMap map = new HexBoardMap(25, 25);
        int[][] data = map.getData();
        Location centre = Location.get(12, 12);
        for (Location l : centre.getRing(7)) {
            data[l.x][l.y] = 1;
        }
        data[12][12] = 1;

        Game game = twoRobotGame(map);

        for (Player p : game.getPlayers()) {
            assertEquals(1, p.getCities().size());
            assertTrue(p.getCities().get(0).isCoastal());
        }
    }
}
