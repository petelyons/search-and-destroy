package com.developingstorm.games.sad.testing;

import com.developingstorm.games.sad.util.json.JsonObj;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Named behavior metric values, optionally with standard errors (when produced by
 * {@link #average(List)}).
 */
public class BehaviorReport {

    private final Map<String, Double> values;
    private final Map<String, Double> stdErrs;

    private BehaviorReport(Map<String, Double> values, Map<String, Double> stdErrs) {
        this.values = new TreeMap<>(values);
        this.stdErrs = new TreeMap<>(stdErrs);
    }

    public static BehaviorReport of(Map<String, Double> values) {
        return new BehaviorReport(values, Map.of());
    }

    public double get(String key) {
        return this.values.getOrDefault(key, 0.0);
    }

    public double stdErr(String key) {
        return this.stdErrs.getOrDefault(key, 0.0);
    }

    public Set<String> keys() {
        return new TreeSet<>(this.values.keySet());
    }

    /** Mean of each metric across reports, with standard error of the mean. */
    public static BehaviorReport average(List<BehaviorReport> reports) {
        Map<String, Double> mean = new TreeMap<>();
        Map<String, Double> err = new TreeMap<>();
        if (reports.isEmpty()) {
            return new BehaviorReport(mean, err);
        }
        Set<String> keys = new TreeSet<>();
        for (BehaviorReport r : reports) {
            keys.addAll(r.values.keySet());
        }
        int n = reports.size();
        for (String k : keys) {
            double sum = 0;
            for (BehaviorReport r : reports) {
                sum += r.get(k);
            }
            double m = sum / n;
            double sq = 0;
            for (BehaviorReport r : reports) {
                double d = r.get(k) - m;
                sq += d * d;
            }
            double sampleStd = n > 1 ? Math.sqrt(sq / (n - 1)) : 0.0;
            mean.put(k, m);
            err.put(k, sampleStd / Math.sqrt(n));
        }
        return new BehaviorReport(mean, err);
    }

    public JsonObj toJson() {
        JsonObj json = new JsonObj();
        JsonObj vals = new JsonObj();
        JsonObj errs = new JsonObj();
        for (Map.Entry<String, Double> e : this.values.entrySet()) {
            vals.put(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Double> e : this.stdErrs.entrySet()) {
            errs.put(e.getKey(), e.getValue());
        }
        json.put("values", vals);
        json.put("stdErr", errs);
        return json;
    }

    public static BehaviorReport fromJson(JsonObj json) {
        return new BehaviorReport(readDoubles(json.getObj("values")), readDoubles(json.getObj("stdErr")));
    }

    private static Map<String, Double> readDoubles(JsonObj obj) {
        Map<String, Double> map = new TreeMap<>();
        if (obj == null) {
            return map;
        }
        // The parser may hand back Long for integral values, so accept any Number
        for (Map.Entry<String, Object> e : obj.entrySet()) {
            map.put(e.getKey(), ((Number) e.getValue()).doubleValue());
        }
        return map;
    }
}
