package com.developingstorm.games.sad;

import java.text.DecimalFormat;
import java.util.List;

/**
 */
public class UnitStats {

    private final List<Unit> units;
    private final List<City> cities;

    private int[] counts;
    private double[] percents;
    private int[] inProduction;

    public int totalCities;
    public int totalUnits;
    public int unitsWithOrders;
    public int unitsReady;

    private final DecimalFormat percentFormater = new DecimalFormat(" 00.00");

    public UnitStats(List<Unit> units, List<City> cities) {
        this.units = units;
        this.cities = cities;
        recalc();
    }

    public void recalc() {
        totalUnits = this.units.size();
        totalCities = this.cities.size();
        counts = new int[Type.classItems()];
        percents = new double[Type.classItems()];
        inProduction = new int[Type.classItems()];

        unitsWithOrders = 0;
        unitsReady = 0;

        for (int i = 0; i < Type.classItems(); i++) {
            this.counts[i] = 0;
            this.inProduction[i] = 0;
        }

        for (Unit u : this.units) {
            if (u.hasOrders()) {
                unitsWithOrders++;
            }
            if (u.turn().isReady()) {
                unitsReady++;
            }
            Type t = u.getType();
            increment(t);
        }

        for (City c : this.cities) {
            Type t = c.getProduction();
            if (t != null) {
                incrementProduction(t);
            }
        }
        updatePercentages();
    }

    public void increment(Type t) {
        this.counts[t.getId()]++;
    }

    public void decrement(Type t) {
        this.counts[t.getId()]--;
    }

    public void incrementProduction(Type t) {
        this.inProduction[t.getId()]++;
    }

    public void decrementProduction(Type t) {
        this.inProduction[t.getId()]--;
    }

    public void updatePercentages() {
        double total = totalUnits;
        for (int i = 0; i < Type.classItems(); i++) {
            double d = this.counts[i];
            this.percents[i] = (d / total) * 100;
        }
    }

    private void buildLine(
        StringBuilder sb,
        String desc,
        int count,
        double percent,
        int beingProduced
    ) {
        sb.append(desc);
        sb.append(":");
        int len = desc.length();
        int pad = 16 - len;
        for (int x = 0; x < pad; x++) {
            sb.append(' ');
        }

        sb.append("Count=");
        sb.append(count);
        sb.append(": Percent=");
        sb.append(this.percentFormater.format(percent));
        sb.append(": In Production=");
        sb.append(beingProduced);
        sb.append("\r\n");
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Cities:");
        sb.append(totalCities);
        sb.append(" Units:");
        sb.append(totalUnits);
        sb.append(" With Order:");
        sb.append(unitsWithOrders);
        sb.append(" Ready:");
        sb.append(unitsReady);
        sb.append("\r\n");

        for (int i = 0; i < Type.classItems(); i++) {
            Type t = Type.get(i);
            buildLine(
                sb,
                t.getName(),
                this.counts[i],
                this.percents[i],
                this.inProduction[i]
            );
        }
        return sb.toString();
    }

    public int getCount(Type t) {
        return this.counts[t.getId()];
    }

    public int getProduction(Type t) {
        return this.inProduction[t.getId()];
    }

    public double getPercentage(Type t) {
        return this.percents[t.getId()];
    }
}
