package com.developingstorm.games.sad.mapgen;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.util.RandomUtil;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class MapGeneratorTest {

    private static HexBoardMap generate(MapSize size, long seed) {
        RandomUtil.setSeed(seed);
        return MapGenerator.generate(size);
    }

    @Test
    public void sizesHaveTheAgreedDimensions() {
        assertEquals(25, MapSize.SMALL.dimension());
        assertEquals(50, MapSize.MEDIUM.dimension());
        assertEquals(75, MapSize.LARGE.dimension());
        assertEquals(100, MapSize.XLARGE.dimension());
        HexBoardMap map = generate(MapSize.LARGE, 1);
        assertEquals(75, map.getWidth());
        assertEquals(75, map.getHeight());
    }

    @Test
    public void oceanIsOneConnectedRegion() {
        for (MapSize size : MapSize.values()) {
            for (long seed = 1; seed <= 30; seed++) {
                HexBoardMap map = generate(size, seed);
                assertEquals(1, MapRegions.waterRegions(map).size(), size + " seed " + seed);
            }
        }
    }

    @Test
    public void edgesAreWater() {
        for (MapSize size : MapSize.values()) {
            for (long seed = 1; seed <= 30; seed++) {
                HexBoardMap map = generate(size, seed);
                for (int x = 0; x < map.getWidth(); x++) {
                    for (int y = 0; y < map.getHeight(); y++) {
                        if (MapRegions.isEdge(map, Location.get(x, y))) {
                            assertEquals(0, map.getData(x, y), size + " seed " + seed + " at " + x + "," + y);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void landShareInRange() {
        for (MapSize size : MapSize.values()) {
            for (long seed = 1; seed <= 30; seed++) {
                double share = MapRegions.landShare(generate(size, seed));
                assertTrue(share >= 0.28 && share <= 0.42, size + " seed " + seed + " share " + share);
            }
        }
    }

    @Test
    public void atLeastTwoContinents() {
        for (MapSize size : MapSize.values()) {
            for (long seed = 1; seed <= 30; seed++) {
                assertTrue(MapRegions.landRegions(generate(size, seed)).size() >= 2, size + " seed " + seed);
            }
        }
    }

    @Test
    public void continentCountVaries() {
        Set<Integer> counts = new HashSet<>();
        for (long seed = 1; seed <= 20; seed++) {
            counts.add(MapRegions.landRegions(generate(MapSize.MEDIUM, seed)).size());
        }
        assertTrue(counts.size() >= 3, "continent counts seen: " + counts);
    }

    @Test
    public void sameSeedSameMap() {
        int[][] first = generate(MapSize.MEDIUM, 42).getData();
        int[][] second = generate(MapSize.MEDIUM, 42).getData();
        assertArrayEquals(first, second);
    }

    @Test
    public void xLargeIsFast() {
        RandomUtil.setSeed(7);
        long start = System.nanoTime();
        MapGenerator.generate(MapSize.XLARGE);
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertTrue(ms < 1000, "took " + ms + " ms");
    }

    @Test
    public void mapSurvivesSaveAndLoad() throws Exception {
        for (MapSize size : MapSize.values()) {
            HexBoardMap map = generate(size, 3);
            StringWriter out = new StringWriter();
            map.saveMap(out);
            HexBoardMap back = HexBoardMap.loadMap(new StringReader(out.toString()));
            assertEquals(map.getWidth(), back.getWidth());
            assertEquals(map.getHeight(), back.getHeight());
            assertArrayEquals(map.getData(), back.getData(), size.toString());
        }
    }
}
