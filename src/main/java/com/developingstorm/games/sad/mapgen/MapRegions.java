package com.developingstorm.games.sad.mapgen;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Connected land and water regions of a map (0 = water, anything else = land).
 */
public final class MapRegions {

    private MapRegions() {}

    public static boolean isLand(HexBoardMap map, Location l) {
        return map.getData(l.x, l.y) > 0;
    }

    /** Continents: connected groups of land hexes. */
    public static List<Set<Location>> landRegions(HexBoardMap map) {
        return regions(map, true);
    }

    /** Bodies of water: connected groups of water hexes. */
    public static List<Set<Location>> waterRegions(HexBoardMap map) {
        return regions(map, false);
    }

    public static boolean isEdge(HexBoardMap map, Location l) {
        return l.x == 0 || l.y == 0 || l.x == map.getWidth() - 1 || l.y == map.getHeight() - 1;
    }

    /** Land with at least one water neighbour. */
    public static boolean isCoastal(HexBoardMap map, Location l) {
        if (!isLand(map, l)) {
            return false;
        }
        for (Location n : l.getRing(1)) {
            if (!isLand(map, n)) {
                return true;
            }
        }
        return false;
    }

    public static double landShare(HexBoardMap map) {
        int land = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (map.getData(x, y) > 0) {
                    land++;
                }
            }
        }
        return (double) land / (map.getWidth() * map.getHeight());
    }

    private static List<Set<Location>> regions(HexBoardMap map, boolean land) {
        List<Set<Location>> result = new ArrayList<>();
        boolean[][] seen = new boolean[map.getWidth()][map.getHeight()];
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Location start = Location.get(x, y);
                if (seen[x][y] || isLand(map, start) != land) {
                    continue;
                }
                Set<Location> region = new HashSet<>();
                ArrayDeque<Location> pending = new ArrayDeque<>();
                seen[x][y] = true;
                pending.add(start);
                while (!pending.isEmpty()) {
                    Location cur = pending.poll();
                    region.add(cur);
                    for (Location n : cur.getRing(1)) {
                        if (!seen[n.x][n.y] && isLand(map, n) == land) {
                            seen[n.x][n.y] = true;
                            pending.add(n);
                        }
                    }
                }
                result.add(region);
            }
        }
        return result;
    }
}
