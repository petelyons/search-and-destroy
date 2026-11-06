package com.developingstorm.games.sad.mapgen;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.util.RandomUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CityPlacerTest {

    private static HexBoardMap map(MapSize size, long seed) {
        RandomUtil.setSeed(seed);
        return MapGenerator.generate(size);
    }

    private static int target(HexBoardMap m) {
        return (m.getWidth() + m.getHeight()) / 2;
    }

    private static void assertSpaced(List<Location> cities, String what) {
        for (int i = 0; i < cities.size(); i++) {
            for (int j = i + 1; j < cities.size(); j++) {
                int d = cities.get(i).distance(cities.get(j));
                assertTrue(d >= 3, what + ": " + cities.get(i) + " and " + cities.get(j) + " are " + d + " apart");
            }
        }
    }

    @Test
    public void citiesAreSpaced() {
        for (MapSize size : MapSize.values()) {
            for (long seed = 1; seed <= 30; seed++) {
                HexBoardMap m = map(size, seed);
                assertSpaced(CityPlacer.place(m, target(m)), size + " seed " + seed);
            }
        }
    }

    @Test
    public void continentsGetCoastalAndInland() {
        for (MapSize size : MapSize.values()) {
            for (long seed = 1; seed <= 30; seed++) {
                HexBoardMap m = map(size, seed);
                List<Location> cities = CityPlacer.place(m, target(m));
                for (Set<Location> continent : MapRegions.landRegions(m)) {
                    boolean hasInlandHex = continent.stream().anyMatch(l -> !MapRegions.isCoastal(m, l));
                    List<Location> here = cities.stream().filter(continent::contains).toList();
                    if (!hasInlandHex || here.size() < 2) {
                        continue;
                    }
                    assertTrue(here.stream().anyMatch(l -> MapRegions.isCoastal(m, l)), size + " seed " + seed + " no coastal city");
                    assertTrue(here.stream().anyMatch(l -> !MapRegions.isCoastal(m, l)), size + " seed " + seed + " no inland city");
                }
            }
        }
    }

    @Test
    public void countNearTarget() {
        for (MapSize size : MapSize.values()) {
            double floor = size == MapSize.SMALL ? 0.5 : 0.8;
            for (long seed = 1; seed <= 30; seed++) {
                HexBoardMap m = map(size, seed);
                int t = target(m);
                int placed = CityPlacer.place(m, t).size();
                assertTrue(placed >= floor * t, size + " seed " + seed + " placed " + placed + " of " + t);
                assertTrue(placed <= t);
            }
        }
    }

    @Test
    public void handMadeMapObeysSpacing() {
        RandomUtil.setSeed(3);
        HexBoardMap m = HexBoardMap.loadMapAsResource(this, "MedMap.sdm");
        assertSpaced(CityPlacer.place(m, target(m)), "MedMap");
    }

    @Test
    public void boardUsesPlacer() {
        for (long seed = 1; seed <= 15; seed++) {
            HexBoardMap m = map(MapSize.MEDIUM, seed);
            Robot a = new Robot("A", 1);
            a.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(a));
            Robot b = new Robot("B", 2);
            b.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(b));
            Game game = new Game(new Player[] {a, b}, m, new MockSaDBoardContext());
            List<Location> locs = new ArrayList<>();
            for (City c : game.getBoard().getCities()) {
                locs.add(c.getLocation());
            }
            assertSpaced(locs, "board seed " + seed);
        }
    }
}
