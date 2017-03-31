package com.developingstorm.games.sad;

import java.text.DecimalFormat;
import java.util.List;

/**
 */
public class UnitStats {
  
  private final List<Unit> _units;
  private final List<City> _cities;

  private int[] _counts;
  private double[] _percents;
  private int[] _inProduction;
  
  public int totalCities;
  public int totalUnits;
  public int unitsWithOrders;
  public int unitsReady;

  private final DecimalFormat _percentFormater = new DecimalFormat(" 00.00");

  
  public UnitStats(List<Unit> units, List<City> cities) {
    _units = units;
    _cities = cities;
    recalc();
  }
    
  public void recalc() {
    totalUnits = _units.size();
    totalCities = _cities.size();
    _counts = new int[Type.classItems()];
    _percents = new double[Type.classItems()];
    _inProduction = new int[Type.classItems()];
    
    unitsWithOrders = 0;
    unitsReady = 0;
    
    for (int i = 0; i < Type.classItems(); i++) {
      _counts[i] =  0;
      _inProduction[i] = 0;
    }
    
    for (Unit u : _units) {
      if (u.hasOrders()) {
        unitsWithOrders++;
      }
      if (u.turn().isReady()) {
        unitsReady++;
      }
      Type t = u.getType();
      increment(t);
    }
    
    for(City c : _cities) {
      Type t = c.getProduction();
      if (t != null) {
        incrementProduction(t);
      }
    }
    updatePercentages();
  }
  
  
  public void increment(Type t) {
    _counts[t.getId()]++;
  }
  public void decrement(Type t) {
    _counts[t.getId()]--;
  }
  
  public void incrementProduction(Type t) {
    _inProduction[t.getId()]++;
  }
  public void decrementProduction(Type t) {
    _inProduction[t.getId()]--;
  }
  
  public void updatePercentages() {
    double total = totalUnits;
    for (int i = 0; i < Type.classItems(); i++) {
      double d = _counts[i];
      _percents[i] =  (d / total) * 100;
    }
  }

  
  private void buildLine(StringBuilder sb, String desc, int count, double percent, int beingProduced) {
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
    sb.append(_percentFormater.format(percent));
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
      buildLine(sb, t.getName(), _counts[i], _percents[i], _inProduction[i]);  
    }
    return sb.toString();
  }
  
  
  public int getCount(Type t) {
    return _counts[t.getId()];
  }
  
  
  public int getProduction(Type t) {
    return _inProduction[t.getId()];

  }

  public double getPercentage(Type t) {
    return _percents[t.getId()];

  }
}
