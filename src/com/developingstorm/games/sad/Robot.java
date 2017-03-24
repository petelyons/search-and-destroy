package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.List;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.brain.RobotBrain;
import com.developingstorm.games.sad.util.Log;

/**
 * Class information
 */
public class Robot extends Player {

  private IBrain _brain;


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
  public void setGame(Game g) {
    super.setGame(g);
    _brain = new RobotBrain(this);
  }

  @Override
  protected void initProduction(City c) {
    Type t = _brain.getProduction(c);
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

  public boolean isUnitInOrAdjascentToCityProducingLoadableUnits(Unit u) {
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

    return _brain.getOrders(u);
  }

 
  @Override
  public void startNewTurn() {
    
    super.startNewTurn();
    _brain.startNewTurn();
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
  
  public UnitStats getStats() {
    UnitStats us = new UnitStats();
    us.recalc(_units, _cities);
    return us;
  }
  
}
