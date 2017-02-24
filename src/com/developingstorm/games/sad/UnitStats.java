package com.developingstorm.games.sad;

import java.util.List;

/**
 */
public class UnitStats {

  public int infantry;
  public int armor;
  public int fighters;
  public int bombers;
  public int cargos;
  public int destroyers;
  public int submarines;
  public int cruisers;
  public int carriers;
  public int battleships;
  public int transports;

  public double percent_infantry;
  public double percent_armor;
  public double percent_fighters;
  public double percent_bombers;
  public double percent_cargos;
  public double percent_destroyers;
  public double percent_submarines;
  public double percent_cruisers;
  public double percent_carriers;
  public double percent_battleships;
  public double percent_transports;

  public int prod_infantry;
  public int prod_armor;
  public int prod_fighters;
  public int prod_bombers;
  public int prod_cargos;
  public int prod_destroyers;
  public int prod_submarines;
  public int prod_cruisers;
  public int prod_carriers;
  public int prod_battleships;
  public int prod_transports;
  
  private int totalCities;
  private int totalUnits;
  private int unitsWithOrders;
  private int unitsReady;

  public void recalc(List<Unit> units, List<City> cities) {
    
    totalUnits = units.size();
    totalCities = cities.size();
    
    unitsWithOrders = 0;
    for (Unit u : units) {
      if (u.hasOrders()) {
        unitsWithOrders++;
      }
    }
    
    unitsReady = 0;
    for (Unit u : units) {
      if (u.turn().isReady()) {
        unitsReady++;
      }
    }

    infantry = 0;
    fighters = 0;
    destroyers = 0;
    submarines = 0;
    cruisers = 0;
    carriers = 0;
    battleships = 0;
    transports = 0;

    double total = 0.0;

    for (Unit u : units) {
      Type t = u.getType();
      total++;
      if (t == Type.INFANTRY)
        infantry++;
      else if (t == Type.ARMOR)
        armor++;
      else if (t == Type.FIGHTER)
        fighters++;
      else if (t == Type.BOMBER)
        bombers++;
      else if (t == Type.CARGO)
        cargos++;
      else if (t == Type.DESTROYER)
        destroyers++;
      else if (t == Type.SUBMARINE)
        submarines++;
      else if (t == Type.CRUISER)
        cruisers++;
      else if (t == Type.CARRIER)
        carriers++;
      else if (t == Type.BATTLESHIP)
        battleships++;
      else if (t == Type.TRANSPORT)
        transports++;
    }

    double d = infantry;
    percent_infantry = d / total;
    d = armor;
    percent_armor = d / total;
    d = fighters;
    percent_fighters = d / total;
    d = bombers;
    percent_bombers = d / total;
    d = cargos;
    percent_cargos = d / total;
    d = destroyers;
    percent_destroyers = d / total;
    d = submarines;
    percent_submarines = d / total;
    d = cruisers;
    percent_cruisers = d / total;
    d = carriers;
    percent_carriers = d / total;
    d = battleships;
    percent_battleships = d / total;
    d = transports;
    percent_transports = d / total;

    prod_infantry = 0;
    prod_armor = 0;
    prod_fighters = 0;
    prod_bombers = 0;
    prod_cargos = 0;
    prod_destroyers = 0;
    prod_submarines = 0;
    prod_cruisers = 0;
    prod_carriers = 0;
    prod_battleships = 0;
    prod_transports = 0;

    for(City c : cities) {
      Type t = c.getProduction();
      total++;
      if (t == Type.INFANTRY)
        prod_infantry++;
      else if (t == Type.ARMOR)
        prod_armor++;
      else if (t == Type.FIGHTER)
        prod_fighters++;
      else if (t == Type.BOMBER)
        prod_bombers++;
      else if (t == Type.CARGO)
        prod_cargos++;
      else if (t == Type.DESTROYER)
        prod_destroyers++;
      else if (t == Type.SUBMARINE)
        prod_submarines++;
      else if (t == Type.CRUISER)
        prod_cruisers++;
      else if (t == Type.CARRIER)
        prod_carriers++;
      else if (t == Type.BATTLESHIP)
        prod_battleships++;
      else if (t == Type.TRANSPORT)
        prod_transports++;
    }

  }

  private Type rawProductionChoice(City c) {
    if (percent_infantry < 0.15)
      return Type.INFANTRY;
    if (percent_fighters < 0.15)
      return Type.FIGHTER;
    if (percent_armor < 0.15)
      return Type.ARMOR;

    if (c.isCoastal()) {
      if (percent_transports < 0.10)
        return Type.TRANSPORT;
      if (percent_destroyers < 0.075)
        return Type.DESTROYER;
      if (percent_submarines < 0.075)
        return Type.SUBMARINE;
      if (percent_cruisers < 0.05)
        return Type.CRUISER;
      if (percent_carriers < 0.05)
        return Type.CARRIER;
      if (percent_battleships < 0.05)
        return Type.BATTLESHIP;
    } else {
      if (percent_bombers < 0.10)
        return Type.BOMBER;
      if (percent_transports < 0.05)
        return Type.CARGO;
      if (percent_armor < percent_infantry)
        return Type.ARMOR;
    }
    return Type.BOMBER;
  }

  public Type productionChoice(City c) {
    Type t = rawProductionChoice(c);

    if (t == Type.INFANTRY && prod_infantry < 1)
      return t;
    else if (t == Type.FIGHTER && fighters < 1)
      return t;
    else if (c.isCoastal() && transports < 1 && prod_transports == 0)
      return Type.TRANSPORT;

    return t;
  }
  
  private static void buildLine(StringBuilder sb, String desc, int count, double percent, int beingProduced) {
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
    sb.append(percent);
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
    
    buildLine(sb, "Infanty", infantry, percent_infantry, prod_infantry);
    buildLine(sb, "Armor", armor, percent_armor, prod_armor);
    buildLine(sb, "Fighters", fighters, percent_fighters, prod_fighters);
    buildLine(sb, "Bombers", bombers, percent_bombers, prod_bombers);
    buildLine(sb, "Cargo", cargos, percent_cargos, prod_cargos);
    buildLine(sb, "Destroyers", destroyers, percent_destroyers, prod_destroyers);
    buildLine(sb, "Submarines", submarines, percent_submarines, prod_submarines);
    buildLine(sb, "Cruisers", cruisers, percent_cruisers, prod_cruisers);
    buildLine(sb, "Battleships", battleships, percent_battleships, prod_battleships);
    buildLine(sb, "Carriers", carriers, percent_carriers, prod_carriers);
    buildLine(sb, "Transports", transports, percent_transports, prod_transports);
    return sb.toString();
  }

}
