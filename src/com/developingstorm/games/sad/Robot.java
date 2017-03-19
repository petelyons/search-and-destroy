package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.List;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;

/**
 * Class information
 */
public class Robot extends Player {

  public Robot(String name, int id) {
    super(name, id);
  }

  @Override
  public boolean isRobot() {
    return true;
  }

  public void loseCity(City c) {
    super.loseCity(c);
  }

  public void captureCity(City c) {
    super.captureCity(c);
  }

  @Override
  protected void initProduction(City c) {
    UnitStats us = new UnitStats();
    us.recalc(_units, _cities);
    Type t = us.productionChoice(c);
    Log.debug(this, "Setting production to: " + t);
    c.produce(t);

  }

  public void addUnit(Unit u) {
    super.addUnit(u);
  }

  public void removeUnit(Unit u) {
    super.removeUnit(u);
  }

  private List<City> getOwnedCities() {
    List<City> list = new ArrayList<City>();
    for (City c : _cities) {
      if (ownsCity(c)) {
        list.add(c);
      }
    }
    return list;
  }

  private boolean isUnitInOrAdjascentToCityProducingLoadableUnits(Unit u) {
    List<City> cities = getOwnedCities();

    Location unitLoc = u.getLocation();
    for (City c : cities) {
      boolean match = false;

      Location loc = c.getLocation();
      if (loc.equals(unitLoc)) {
        match = true;
      } else {
        List<Location> list = loc.getRing(1);
        if (list.contains(unitLoc)) {
          match = true;
        }
      }
      if (match && u.canCarry(c.getProduction())) {
        return true;
      }
    }
    return false;
  }

  public Order getOrders(Unit u) {

    if (u.getType() == Type.INFANTRY) {

      List<City> cities = reachableCities(u);
      for (City c : cities) {
        if (c != null && c.getOwner() != this) {
          Log.debug(this, "Moving to city:" + c);
          return u.newMoveOrder(c.getLocation());
        }
      }
    }

    if (u.getType() == Type.TRANSPORT || u.getType() == Type.CARGO) {
      if (u.carriedWeight() < u.carriableWeight()) {
        if (isUnitInOrAdjascentToCityProducingLoadableUnits(u)) {
          return u.newSentryOrder();
        }
        if (isUnitNextToTargetLand(u)) {
          return u.newUnloadOrder();
        }
      } else {
      }
    }

    List<Unit> enemies = reachableEnemies(u);
    if (!enemies.isEmpty()) {

      Unit closestEnemy = null;
      Location unitLocation = u.getLocation();
      for (Unit enemy : enemies) {
        if (closestEnemy == null) {
          closestEnemy = enemy;
        } else {
          Location knownClosestEnemyLocation = closestEnemy.getLocation();
          Location enemyLocation = enemy.getLocation();
          Path pathToEnemy = u.getPath(enemyLocation);
          if (pathToEnemy != null && !pathToEnemy.isEmpty()) {
            if (unitLocation.distance(enemyLocation) < unitLocation
              .distance(knownClosestEnemyLocation)) {
              closestEnemy = enemy;
            }
          }
        }
      }
      Log.debug(u, "Moving to attack:" + closestEnemy);
      return u.newMoveOrder(closestEnemy.getLocation());
    }

 
    return u.newExploreOrder();
  }

  private static boolean isUnitNextToTargetLand(Unit u) {
    return false;
  }
  

  @Override
  public void startNewTurn() {
    
    super.startNewTurn();
    for (City c : _cities) {
      if (c.productionCompleted()) {
        
        Type t = _unitStats.productionChoice(c);
        Log.debug(this, "Resetting production of " + c + " to: " + t);
        c.produce(t);
      }
    }
  }
  
  
  @Override
  public String toString() {
    return "Robot: N=" + _name + " I=" + _id;
  }

  
  @Override
  public void unitsNeedOrders() {
    for (Unit u : _units) {
      if (!u.hasOrders()) {
        u.assignOrder(getOrders(u));
      }
    }
    Log.debug(this, "Generated orders");
  }
  
}
