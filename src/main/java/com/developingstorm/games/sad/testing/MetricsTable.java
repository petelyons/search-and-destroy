package com.developingstorm.games.sad.testing;

import java.util.Set;
import java.util.TreeSet;

/**
 * Renders a {@link BehaviorReport} as a text table, optionally against a baseline.
 * A row is flagged with "!" when it moved in the bad direction by more than two
 * combined standard errors.
 */
public final class MetricsTable {

    private static final Set<String> LOWER_IS_WORSE = Set.of(
        "seaLoad", "landings", "overseasCaptures", "navalAttacksPer100"
    );

    private MetricsTable() {}

    public static String render(BehaviorReport current, BehaviorReport baseline) {
        Set<String> keys = new TreeSet<>(current.keys());
        if (baseline != null) {
            keys.addAll(baseline.keys());
        }
        StringBuilder sb = new StringBuilder();
        if (baseline == null) {
            sb.append(String.format("%-22s %10s %8s%n", "metric", "mean", "±se"));
        } else {
            sb.append(String.format("%-22s %10s %8s %10s %9s  %s%n", "metric", "mean", "±se", "baseline", "Δ%", ""));
        }
        for (String k : keys) {
            double m = current.get(k);
            double se = current.stdErr(k);
            if (baseline == null) {
                sb.append(String.format("%-22s %10.3f %8.3f%n", k, m, se));
                continue;
            }
            double b = baseline.get(k);
            double noise = 2 * Math.sqrt(se * se + baseline.stdErr(k) * baseline.stdErr(k));
            double delta = m - b;
            boolean worse = LOWER_IS_WORSE.contains(k) ? delta < 0 : delta > 0;
            String flag = (worse && Math.abs(delta) > noise) ? "!" : "";
            String pct = b == 0 ? "n/a" : String.format("%+.0f%%", 100 * delta / b);
            sb.append(String.format("%-22s %10.3f %8.3f %10.3f %9s  %s%n", k, m, se, b, pct, flag));
        }
        return sb.toString();
    }
}
