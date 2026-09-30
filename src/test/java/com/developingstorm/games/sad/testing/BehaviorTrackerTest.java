package com.developingstorm.games.sad.testing;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Travel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BehaviorTrackerTest {

    private static final double EPS = 1e-9;

    @BeforeAll
    public static void initMap() {
        LocationMap.init(50, 50);
    }

    private static Location at(int x, int y) {
        return Location.get(x, y);
    }

    /** Land unit on continent {@code cont}, not carried, not exempt. */
    private static UnitSnapshot land(long id, int x, int y, Location dest, int cont) {
        return new UnitSnapshot(id, Travel.LAND, at(x, y), false, false, false, 0, false, dest, cont);
    }

    private static UnitSnapshot carried(long id, int x, int y) {
        return new UnitSnapshot(id, Travel.LAND, at(x, y), true, true, false, 0, false, null, -1);
    }

    private static UnitSnapshot transport(long id, int x, int y, int cargo, boolean openSea) {
        return new UnitSnapshot(id, Travel.SEA, at(x, y), false, false, true, cargo, openSea, null, -1);
    }

    private static void turn(BehaviorTracker t, UnitSnapshot... units) {
        t.onTurnEnd(1, List.of(units), Map.of());
    }

    @Test
    public void oscillationCountsReturnToRecentHex() {
        BehaviorTracker t = new BehaviorTracker();
        turn(t, land(1, 5, 5, null, 1));
        turn(t, land(1, 6, 5, null, 1));
        turn(t, land(1, 5, 5, null, 1));
        assertEquals(0.5, t.report().get("LAND.oscillation"), EPS);
    }

    @Test
    public void retargetCountsDestinationChangeBeforeArrival() {
        BehaviorTracker t = new BehaviorTracker();
        turn(t, land(1, 5, 5, at(20, 20), 1));
        turn(t, land(1, 6, 5, at(30, 30), 1));
        assertEquals(10.0, t.report().get("LAND.retarget"), EPS);
    }

    @Test
    public void arrivalIsNotRetarget() {
        BehaviorTracker t = new BehaviorTracker();
        turn(t, land(1, 5, 5, at(6, 5), 1));
        turn(t, land(1, 6, 5, null, 1));
        assertEquals(0.0, t.report().get("LAND.retarget"), EPS);
    }

    @Test
    public void idleExcludesExemptUnits() {
        BehaviorTracker t = new BehaviorTracker();
        UnitSnapshot sentry = new UnitSnapshot(2, Travel.LAND, at(9, 9), false, true, false, 0, false, null, 1);
        turn(t, land(1, 5, 5, null, 1), sentry);
        turn(t, land(1, 5, 5, null, 1), sentry);
        assertEquals(1.0, t.report().get("LAND.idle"), EPS);
    }

    @Test
    public void landingOnNewContinent() {
        BehaviorTracker t = new BehaviorTracker();
        turn(t, land(1, 5, 5, null, 1));
        turn(t, carried(1, 7, 5));
        turn(t, land(1, 9, 5, null, 2));
        assertEquals(1.0, t.report().get("landings"), EPS);
    }

    @Test
    public void landingOnSameContinentIsNotCounted() {
        BehaviorTracker t = new BehaviorTracker();
        turn(t, land(1, 5, 5, null, 1));
        turn(t, carried(1, 7, 5));
        turn(t, land(1, 9, 5, null, 1));
        assertEquals(0.0, t.report().get("landings"), EPS);
    }

    @Test
    public void seaLoadAndEmptySailing() {
        BehaviorTracker t = new BehaviorTracker();
        t.onTurnEnd(1, List.of(transport(3, 10, 10, 0, true)), Map.of());
        t.onTurnEnd(1, List.of(transport(3, 11, 10, 4, true)), Map.of());
        t.onTurnEnd(1, List.of(transport(3, 12, 10, 0, true)), Map.of());
        assertEquals(2.0, t.report().get("seaLoad"), EPS);
        assertEquals(0.5, t.report().get("emptySailing"), EPS);
    }

    @Test
    public void fuelLossCounted() {
        BehaviorTracker t = new BehaviorTracker();
        turn(t);
        t.onUnitKilled(1, 7, Travel.AIR, true);
        t.onUnitKilled(1, 8, Travel.AIR, false);
        assertEquals(1.0, t.report().get("fuelLosses"), EPS);
    }

    @Test
    public void overseasCaptureCounted() {
        BehaviorTracker t = new BehaviorTracker();
        Map<Location, Integer> owned = new HashMap<>();
        owned.put(at(1, 1), 1);
        t.onTurnEnd(1, List.of(), new HashMap<>(owned));
        owned.put(at(20, 20), 2);
        t.onTurnEnd(1, List.of(), new HashMap<>(owned));
        owned.put(at(22, 22), 2);
        t.onTurnEnd(1, List.of(), new HashMap<>(owned));
        assertEquals(1.0, t.report().get("overseasCaptures"), EPS);
    }

    @Test
    public void navalAttacksPer100SeaMoves() {
        BehaviorTracker t = new BehaviorTracker();
        t.onTurnEnd(1, List.of(transport(3, 10, 10, 0, false)), Map.of());
        t.onTurnEnd(1, List.of(transport(3, 11, 10, 0, false)), Map.of());
        t.onCombat(1, Travel.SEA);
        t.onCombat(1, Travel.LAND);
        assertEquals(100.0, t.report().get("navalAttacksPer100"), EPS);
    }

    @Test
    public void deadUnitHistoryDropped() {
        BehaviorTracker t = new BehaviorTracker();
        turn(t, land(1, 5, 5, at(20, 20), 1));
        t.onUnitKilled(1, 1, Travel.LAND, false);
        turn(t);
        turn(t, land(1, 30, 30, null, 3)); // id reused: treated as new, no retarget/landing
        BehaviorReport r = t.report();
        assertEquals(0.0, r.get("LAND.retarget"), EPS);
        assertEquals(0.0, r.get("landings"), EPS);
    }

    @Test
    public void emptyPlayerReportsZeros() {
        BehaviorTracker t = new BehaviorTracker();
        turn(t);
        turn(t);
        BehaviorReport r = t.report();
        assertFalse(r.keys().isEmpty());
        for (String k : r.keys()) {
            assertEquals(0.0, r.get(k), EPS, k);
        }
    }

    @Test
    public void metricsAverageOverPlayers() {
        BehaviorTracker t = new BehaviorTracker();
        t.onUnitKilled(1, 7, Travel.AIR, true);
        t.onUnitKilled(1, 8, Travel.AIR, true);
        t.onTurnEnd(1, List.of(), Map.of());
        t.onTurnEnd(2, List.of(), Map.of());
        assertEquals(1.0, t.report().get("fuelLosses"), EPS);
    }

    @Test
    public void averageComputesMeanAndStdErr() {
        BehaviorReport a = BehaviorReport.of(Map.of("LAND.idle", 0.2));
        BehaviorReport b = BehaviorReport.of(Map.of("LAND.idle", 0.4));
        List<BehaviorReport> list = new ArrayList<>(List.of(a, b));
        BehaviorReport avg = BehaviorReport.average(list);
        assertEquals(0.3, avg.get("LAND.idle"), EPS);
        assertEquals(0.1, avg.stdErr("LAND.idle"), 1e-6);
    }

    @Test
    public void jsonRoundTrip() {
        BehaviorReport a = BehaviorReport.of(Map.of("LAND.idle", 0.2));
        BehaviorReport b = BehaviorReport.of(Map.of("LAND.idle", 0.4));
        BehaviorReport avg = BehaviorReport.average(List.of(a, b));
        BehaviorReport back = BehaviorReport.fromJson(avg.toJson());
        assertEquals(avg.get("LAND.idle"), back.get("LAND.idle"), EPS);
        assertEquals(avg.stdErr("LAND.idle"), back.stdErr("LAND.idle"), EPS);
    }
}
