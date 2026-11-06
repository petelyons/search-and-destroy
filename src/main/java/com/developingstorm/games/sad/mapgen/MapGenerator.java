package com.developingstorm.games.sad.mapgen;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.util.RandomUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Generates a land/water map: separate continents of varied size in one connected ocean.
 * All randomness comes from {@link RandomUtil}, so a seed reproduces a map.
 */
public final class MapGenerator {

    static final int MAX_ATTEMPTS = 20;
    static final double MIN_LAND = 0.30;
    static final double MAX_LAND = 0.40;

    /** A map is accepted if its land share lands in this wider band (lake filling adds land). */
    private static final double ACCEPT_MIN_LAND = 0.28;
    private static final double ACCEPT_MAX_LAND = 0.42;

    private static final int WATER = 0;
    private static final int LAND = 1;

    private MapGenerator() {}

    public static HexBoardMap generate(MapSize size) {
        HexBoardMap map = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            map = attempt(size);
            double share = MapRegions.landShare(map);
            if (MapRegions.landRegions(map).size() >= 2 && share >= ACCEPT_MIN_LAND && share <= ACCEPT_MAX_LAND) {
                return map;
            }
        }
        return map;
    }

    private static HexBoardMap attempt(MapSize size) {
        int n = size.dimension();
        HexBoardMap map = new HexBoardMap(n, n);
        int[][] data = map.getData();
        int[][] owner = new int[n][n]; // continent id + 1, 0 for water

        double landTarget = MIN_LAND + (MAX_LAND - MIN_LAND) * RandomUtil.getInt(1001) / 1000.0;
        int targetCells = (int) Math.round(landTarget * n * n);
        int continents = size.minContinents()
            + RandomUtil.getInt(size.maxContinents() - size.minContinents() + 1);

        // Seed each continent with a random weight so their sizes differ
        List<List<Location>> frontiers = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        int land = 0;
        for (int c = 0; c < continents; c++) {
            Location seed = randomSeed(map, owner);
            if (seed == null) {
                break;
            }
            int id = frontiers.size() + 1;
            setLand(data, owner, seed, id);
            land++;
            List<Location> frontier = new ArrayList<>(seed.getRing(1));
            frontiers.add(frontier);
            weights.add(1 + RandomUtil.getInt(10));
        }

        // Grow continents one hex at a time, picking a continent in proportion to its weight
        while (land < targetCells && !frontiers.isEmpty()) {
            int pick = weightedPick(weights);
            List<Location> frontier = frontiers.get(pick);
            int id = pick + 1;
            Location next = null;
            while (!frontier.isEmpty() && next == null) {
                Location cand = frontier.remove(RandomUtil.getInt(frontier.size()));
                if (canGrowInto(map, owner, cand, id)) {
                    next = cand;
                }
            }
            if (next == null) {
                weights.set(pick, 0); // this continent is boxed in
                if (weights.stream().allMatch(w -> w == 0)) {
                    break;
                }
                continue;
            }
            setLand(data, owner, next, id);
            land++;
            frontier.addAll(next.getRing(1));
        }

        forceWaterEdges(map);
        fillLakes(map);
        return map;
    }

    /**
     * Edge hexes are always water, so the ocean surrounds every continent.
     * Pete isn't sure he likes this; drop this call (and the edge check in canGrowInto) to
     * let land reach the edge. fillLakes keeps the one-ocean rule either way.
     */
    private static void forceWaterEdges(HexBoardMap map) {
        int[][] data = map.getData();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if (MapRegions.isEdge(map, Location.get(x, y))) {
                    data[x][y] = WATER;
                }
            }
        }
    }

    /** Turn every body of water except the largest into land, leaving one connected ocean. */
    private static void fillLakes(HexBoardMap map) {
        List<Set<Location>> water = MapRegions.waterRegions(map);
        Set<Location> ocean = null;
        for (Set<Location> region : water) {
            if (ocean == null || region.size() > ocean.size()) {
                ocean = region;
            }
        }
        int[][] data = map.getData();
        for (Set<Location> region : water) {
            if (region != ocean) {
                for (Location l : region) {
                    data[l.x][l.y] = LAND;
                }
            }
        }
    }

    private static Location randomSeed(HexBoardMap map, int[][] owner) {
        for (int tries = 0; tries < 200; tries++) {
            Location l = Location.get(
                1 + RandomUtil.getInt(map.getWidth() - 2),
                1 + RandomUtil.getInt(map.getHeight() - 2)
            );
            if (owner[l.x][l.y] == 0 && canGrowInto(map, owner, l, -1)) {
                return l;
            }
        }
        return null;
    }

    /** Water, off the edge, and not touching another continent (continents stay separate). */
    private static boolean canGrowInto(HexBoardMap map, int[][] owner, Location l, int id) {
        if (owner[l.x][l.y] != 0 || MapRegions.isEdge(map, l)) {
            return false;
        }
        for (Location n : l.getRing(1)) {
            int o = owner[n.x][n.y];
            if (o != 0 && o != id) {
                return false;
            }
        }
        return true;
    }

    private static void setLand(int[][] data, int[][] owner, Location l, int id) {
        data[l.x][l.y] = LAND;
        owner[l.x][l.y] = id;
    }

    private static int weightedPick(List<Integer> weights) {
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        int r = RandomUtil.getInt(total);
        for (int i = 0; i < weights.size(); i++) {
            r -= weights.get(i);
            if (r < 0) {
                return i;
            }
        }
        return weights.size() - 1;
    }
}
