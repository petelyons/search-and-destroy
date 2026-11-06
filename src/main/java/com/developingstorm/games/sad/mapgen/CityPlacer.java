package com.developingstorm.games.sad.mapgen;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.util.RandomUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Chooses city locations on any map. Cities are never closer than {@link #MIN_SPACING};
 * continents get cities in proportion to their area, and a continent with inland ground and
 * room for two or more gets both a coastal and an inland city.
 */
public final class CityPlacer {

    public static final int MIN_SPACING = 3;

    /** Continents smaller than this (in hexes) get no city. */
    public static final int MIN_CONTINENT_FOR_CITY = 4;

    private CityPlacer() {}

    public static List<Location> place(HexBoardMap map, int target) {
        List<List<Location>> continents = new ArrayList<>();
        for (Set<Location> region : MapRegions.landRegions(map)) {
            if (region.size() >= MIN_CONTINENT_FOR_CITY) {
                List<Location> hexes = new ArrayList<>(region);
                hexes.sort(Comparator.comparingInt((Location l) -> l.y).thenComparingInt(l -> l.x));
                continents.add(hexes);
            }
        }
        continents.sort(Comparator.comparingInt((List<Location> c) -> c.size()).reversed());

        List<Location> placed = new ArrayList<>();
        List<Continent> all = new ArrayList<>();
        for (List<Location> hexes : continents) {
            all.add(new Continent(map, hexes));
        }
        int[] quotas = quotas(continents, target);
        for (int i = 0; i < all.size(); i++) {
            while (all.get(i).cities < quotas[i] && all.get(i).placeOne(placed)) {
                // keep placing up to quota
            }
        }

        // Quota a crowded continent couldn't use goes to any continent with room left
        for (Continent c : all) {
            while (placed.size() < target && c.placeOne(placed)) {
                // keep filling
            }
        }
        return placed;
    }

    /** One continent's hexes, split coastal/inland, and the cities placed on it so far. */
    private static final class Continent {
        final List<Location> hexes;
        final List<Location> coastal = new ArrayList<>();
        final List<Location> inland = new ArrayList<>();
        int cities;
        int coastalCities;

        Continent(HexBoardMap map, List<Location> hexes) {
            this.hexes = hexes;
            for (Location l : hexes) {
                (MapRegions.isCoastal(map, l) ? coastal : inland).add(l);
            }
        }

        /** Coastal hexes that still leave some inland hex far enough away for a second city. */
        List<Location> coastalLeavingInlandRoom() {
            List<Location> room = new ArrayList<>();
            for (Location c : coastal) {
                for (Location i : inland) {
                    if (c.distance(i) >= MIN_SPACING) {
                        room.add(c);
                        break;
                    }
                }
            }
            return room;
        }

        /**
         * Place one city, choosing the kind the continent still lacks: its first city is
         * coastal, its second inland when it has inland ground. Returns false if nothing fits.
         */
        boolean placeOne(List<Location> placed) {
            boolean ok;
            if (coastalCities == 0 && (tryPlace(coastalLeavingInlandRoom(), placed) == 1 || tryPlace(coastal, placed) == 1)) {
                coastalCities++;
                ok = true;
            } else if (cities - coastalCities == 0 && cities > 0 && tryPlace(inland, placed) == 1) {
                ok = true;
            } else {
                int before = placed.size();
                ok = tryPlace(hexes, placed) == 1;
                if (ok && coastal.contains(placed.get(before))) {
                    coastalCities++;
                }
            }
            if (ok) {
                cities++;
            }
            return ok;
        }
    }

    /** Share the target across continents by area; every continent gets at least one while there are enough. */
    private static int[] quotas(List<List<Location>> continents, int target) {
        int[] quotas = new int[continents.size()];
        int total = 0;
        for (List<Location> c : continents) {
            total += c.size();
        }
        int assigned = 0;
        for (int i = 0; i < continents.size(); i++) {
            quotas[i] = Math.max(1, (int) Math.round((double) target * continents.get(i).size() / total));
            assigned += quotas[i];
        }
        // Trim from the largest continents first, then from the smallest, until on target
        for (int i = 0; assigned > target && i < quotas.length; ) {
            if (quotas[i] > 1) {
                quotas[i]--;
                assigned--;
            } else {
                i++;
            }
        }
        for (int i = quotas.length - 1; assigned > target && i >= 0; i--) {
            assigned -= quotas[i];
            quotas[i] = 0;
        }
        return quotas;
    }

    /** Place one city at a random hex from {@code candidates} that keeps the spacing. Returns 1 if placed. */
    private static int tryPlace(List<Location> candidates, List<Location> placed) {
        List<Location> shuffled = new ArrayList<>(candidates);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = RandomUtil.getInt(i + 1);
            Location tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        for (Location l : shuffled) {
            if (spaced(l, placed)) {
                placed.add(l);
                return 1;
            }
        }
        return 0;
    }

    private static boolean spaced(Location l, List<Location> placed) {
        for (Location p : placed) {
            if (l.distance(p) < MIN_SPACING) {
                return false;
            }
        }
        return true;
    }
}
