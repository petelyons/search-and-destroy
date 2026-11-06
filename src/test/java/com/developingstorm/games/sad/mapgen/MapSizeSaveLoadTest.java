package com.developingstorm.games.sad.mapgen;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.persistence.GameStateSerializer;
import com.developingstorm.util.RandomUtil;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/** The app's board context reports the size of whatever map is open, not the one being built or loaded. */
public class MapSizeSaveLoadTest {

    private static Game game(HexBoardMap map) {
        Robot a = new Robot("A", 1);
        a.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(a));
        Robot b = new Robot("B", 2);
        b.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(b));
        // MockSaDBoardContext always reports 50×50
        return new Game(new Player[] {a, b}, map, new MockSaDBoardContext());
    }

    @Test
    public void boardIsSizedFromItsMapNotTheContext() {
        RandomUtil.setSeed(11);
        HexBoardMap map = MapGenerator.generate(MapSize.LARGE);
        Game g = game(map);
        assertEquals(75, g.getBoard().getWidth());
        assertEquals(75, g.getBoard().getHeight());
    }

    @Test
    public void everySizeSavesAndLoadsIntoAnyContext() throws Exception {
        for (MapSize size : MapSize.values()) {
            RandomUtil.setSeed(21);
            HexBoardMap map = MapGenerator.generate(size);
            int[][] grid = copy(map.getData());
            Game g = game(map);
            File f = Files.createTempFile("mapsize-" + size.name(), ".sav").toFile();
            f.deleteOnExit();
            new GameStateSerializer().saveGameToFile(g, f);

            Game loaded = new GameStateSerializer().loadGame(f, new MockSaDBoardContext());

            assertEquals(size.dimension(), loaded.getBoard().getWidth(), size.toString());
            assertArrayEquals(grid, loaded.getBoard().map.getData(), size.toString());
            assertEquals(g.getBoard().getCities().size(), loaded.getBoard().getCities().size(), size.toString());
        }
    }

    private static int[][] copy(int[][] a) {
        int[][] c = new int[a.length][];
        for (int i = 0; i < a.length; i++) {
            c[i] = a[i].clone();
        }
        return c;
    }
}
