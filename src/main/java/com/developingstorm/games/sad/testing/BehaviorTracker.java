package com.developingstorm.games.sad.testing;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Travel;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Computes AI-agnostic behavior metrics from per-player end-of-turn unit snapshots.
 * Knows nothing about the game model, so it can be fed synthetic data in tests.
 */
public class BehaviorTracker {

    private static final Travel[] TRAVELS = { Travel.LAND, Travel.SEA, Travel.AIR };

    private static class TravelStats {
        int unitTurns;
        int moved;
        int oscillations;
        int retargets;
        int idleEligible;
        int idle;
    }

    private static class UnitHistory {
        final List<Location> recent = new ArrayList<>(); // [t-1, t-2, t-3]
        Location dest;
        boolean carried;
        int lastLandContinent = -1;
    }

    private static class PlayerStats {
        final Map<Travel, TravelStats> travel = new EnumMap<>(Travel.class);
        final Map<Long, UnitHistory> units = new HashMap<>();
        Map<Location, Integer> ownedCities;
        int seaSailings;
        long cargoSum;
        int emptySailings;
        int landings;
        int overseasCaptures;
        int fuelLosses;
        int navalAttacks;

        PlayerStats() {
            for (Travel t : TRAVELS) {
                this.travel.put(t, new TravelStats());
            }
        }
    }

    private final Map<Integer, PlayerStats> players = new TreeMap<>();

    private PlayerStats player(int playerId) {
        return this.players.computeIfAbsent(playerId, id -> new PlayerStats());
    }

    public void onTurnEnd(int playerId, List<UnitSnapshot> units, Map<Location, Integer> ownedCityContinents) {
        PlayerStats ps = player(playerId);
        Set<Long> seen = new HashSet<>();

        for (UnitSnapshot s : units) {
            seen.add(s.id());
            UnitHistory h = ps.units.get(s.id());
            if (h == null) {
                h = new UnitHistory();
                ps.units.put(s.id(), h);
            } else {
                recordUnitTurn(ps, h, s);
            }
            h.recent.add(0, s.loc());
            if (h.recent.size() > 3) {
                h.recent.remove(3);
            }
            h.dest = s.moveDest();
            h.carried = s.carried();
            if (!s.carried() && s.landContinentId() >= 0) {
                h.lastLandContinent = s.landContinentId();
            }
        }
        // Units missing from this snapshot are gone
        ps.units.keySet().retainAll(seen);

        if (ps.ownedCities != null) {
            Set<Integer> prevContinents = new HashSet<>(ps.ownedCities.values());
            for (Map.Entry<Location, Integer> e : ownedCityContinents.entrySet()) {
                if (!ps.ownedCities.containsKey(e.getKey()) && !prevContinents.contains(e.getValue())) {
                    ps.overseasCaptures++;
                }
            }
        }
        ps.ownedCities = new HashMap<>(ownedCityContinents);
    }

    private static void recordUnitTurn(PlayerStats ps, UnitHistory h, UnitSnapshot s) {
        TravelStats ts = ps.travel.get(s.travel());
        if (ts == null) {
            return;
        }
        Location prev = h.recent.get(0);
        boolean moved = !s.loc().equals(prev);
        ts.unitTurns++;

        if (moved) {
            ts.moved++;
            for (int i = 1; i < h.recent.size(); i++) {
                if (s.loc().equals(h.recent.get(i))) {
                    ts.oscillations++;
                    break;
                }
            }
        }

        Location d = h.dest;
        if (d != null && !prev.equals(d) && !s.loc().equals(d) && !d.equals(s.moveDest())) {
            ts.retargets++;
        }

        if (!s.idleExempt()) {
            ts.idleEligible++;
            if (!moved) {
                ts.idle++;
            }
        }

        if (s.transport() && moved && s.openSea()) {
            ps.seaSailings++;
            ps.cargoSum += s.cargo();
            if (s.cargo() == 0) {
                ps.emptySailings++;
            }
        }

        if (h.carried && !s.carried() && s.landContinentId() >= 0
                && h.lastLandContinent >= 0 && s.landContinentId() != h.lastLandContinent) {
            ps.landings++;
        }
    }

    public void onUnitKilled(int playerId, long unitId, Travel travel, boolean outOfFuel) {
        PlayerStats ps = player(playerId);
        ps.units.remove(unitId);
        if (outOfFuel) {
            ps.fuelLosses++;
        }
    }

    public void onCombat(int attackerPlayerId, Travel attackerTravel) {
        if (attackerTravel == Travel.SEA) {
            player(attackerPlayerId).navalAttacks++;
        }
    }

    /** Metrics for each player, averaged across players. */
    public BehaviorReport report() {
        Map<String, Double> sum = new TreeMap<>();
        for (String k : keys()) {
            sum.put(k, 0.0);
        }
        if (this.players.isEmpty()) {
            return BehaviorReport.of(sum);
        }
        for (PlayerStats ps : this.players.values()) {
            for (Map.Entry<String, Double> e : playerMetrics(ps).entrySet()) {
                sum.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }
        int n = this.players.size();
        sum.replaceAll((k, v) -> v / n);
        return BehaviorReport.of(sum);
    }

    private static Map<String, Double> playerMetrics(PlayerStats ps) {
        Map<String, Double> m = new TreeMap<>();
        for (Travel t : TRAVELS) {
            TravelStats ts = ps.travel.get(t);
            m.put(t.name() + ".oscillation", ratio(ts.oscillations, ts.moved));
            m.put(t.name() + ".retarget", 10.0 * ratio(ts.retargets, ts.unitTurns));
            m.put(t.name() + ".idle", ratio(ts.idle, ts.idleEligible));
        }
        m.put("seaLoad", ps.seaSailings == 0 ? 0.0 : (double) ps.cargoSum / ps.seaSailings);
        m.put("emptySailing", ratio(ps.emptySailings, ps.seaSailings));
        m.put("landings", (double) ps.landings);
        m.put("overseasCaptures", (double) ps.overseasCaptures);
        m.put("fuelLosses", (double) ps.fuelLosses);
        m.put("navalAttacksPer100", 100.0 * ratio(ps.navalAttacks, ps.travel.get(Travel.SEA).moved));
        return m;
    }

    private static double ratio(double num, double den) {
        return den == 0 ? 0.0 : num / den;
    }

    static Set<String> keys() {
        return new TreeSet<>(playerMetrics(new PlayerStats()).keySet());
    }
}
