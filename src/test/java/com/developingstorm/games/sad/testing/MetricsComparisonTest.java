package com.developingstorm.games.sad.testing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MetricsComparisonTest {

    private static BehaviorReport avg(String key, double a, double b) {
        return BehaviorReport.average(List.of(
            BehaviorReport.of(Map.of(key, a)),
            BehaviorReport.of(Map.of(key, b))
        ));
    }

    private static String line(String table, String key) {
        for (String l : table.split("\n")) {
            if (l.trim().startsWith(key + " ")) {
                return l;
            }
        }
        fail("no row for " + key + " in\n" + table);
        return null;
    }

    @Test
    public void flagsOscillationIncreaseBeyondNoise() {
        BehaviorReport base = avg("LAND.oscillation", 0.10, 0.12);
        BehaviorReport cur = avg("LAND.oscillation", 0.40, 0.42);
        assertTrue(line(MetricsTable.render(cur, base), "LAND.oscillation").contains("!"));
    }

    @Test
    public void doesNotFlagOscillationDecrease() {
        BehaviorReport base = avg("LAND.oscillation", 0.40, 0.42);
        BehaviorReport cur = avg("LAND.oscillation", 0.10, 0.12);
        assertFalse(line(MetricsTable.render(cur, base), "LAND.oscillation").contains("!"));
    }

    @Test
    public void flagsLandingsDecrease() {
        BehaviorReport base = avg("landings", 10, 12);
        BehaviorReport cur = avg("landings", 1, 2);
        assertTrue(line(MetricsTable.render(cur, base), "landings").contains("!"));
    }

    @Test
    public void doesNotFlagChangeWithinNoise() {
        BehaviorReport base = avg("LAND.idle", 0.2, 0.6);
        BehaviorReport cur = avg("LAND.idle", 0.3, 0.7);
        assertFalse(line(MetricsTable.render(cur, base), "LAND.idle").contains("!"));
    }

    @Test
    public void rendersWithoutBaseline() {
        String t = MetricsTable.render(avg("landings", 1, 3), null);
        assertTrue(line(t, "landings").contains("2.000"));
    }
}
