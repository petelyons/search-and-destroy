package com.developingstorm.games.sad.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.CollectionUtil;

public abstract class UnitCaptain<T extends Unit>  {
  
  protected Battleplan _plan;
  protected General _general;

  protected UnitCaptain(General general, Battleplan plan) {
    _general = general;
    _plan = plan;
  }
  
  public abstract Order plan(T u);

  
  
  protected Order occupyUnownedCity(Unit u) {
    List<City> cities = u.getOwner().reachableCities(u);
    for (City c : cities) {
      if (c != null) {
        Player cityOwner = c.getOwner();
        if (cityOwner != null && !cityOwner.equals(u.getOwner())) {
          Log.debug(this, "Moving to enemy city:" + c);
          return u.newMoveOrder(c.getLocation());
        }
        else if (cityOwner == null) {
          Log.debug(this, "Moving to unoccupied city:" + c);
          return u.newMoveOrder(c.getLocation());
        }
      }
    }
    return null;
  }
  
  protected Order goToLoadingPoint(Unit u) {
    Location loc = u.getClosestLocation(_plan.getLoadingPoints());
    if (loc != null) {
      return u.newMoveOrder(loc);
    }
    else {
      return null;
    }
  }
    
  protected Order explore(Unit u) {
    ArrayList<Location> frontierLocations = u.getOwner().getFrontier(u);
    Location ul = u.getLocation();
    Location loc = ul.closest(frontierLocations);
    if (loc != null) { 
      return u.newExploreOrder();
    }
    return null;
  }
  
  protected Order unload(Unit u) {
    return u.newUnloadOrder();
  }

  
  protected Order goToUnloadingPoint(Unit u) {
    Location loc = u.getClosestLocation(_plan.getUnloadingPoints());
    if (loc != null) {
      return u.newMoveOrder(loc);
    }
    else {
      return null;
    }
  }
  
  protected Order sentry(Unit u) {
    return u.newSentryOrder();    
  }

  protected boolean atLoadingPoint(Unit u) {
    return _plan.getLoadingPoints().contains(u.getLocation());
  }

  protected boolean atUnloadPoint(Unit u) {
    return _plan.getUnloadingPoints().contains(u.getLocation());
  }
  
  
  protected Order patrolUnloadingZones(Unit u) {
    Location loc = u.getClosestLocation(_plan.getUnloadingPoints());
    if (loc.distance(u.getLocation()) > 10) {
      return u.newMoveOrder(loc);
    } else {
      List<Location> ring = u.getLocation().getRing(u.getMaxTravel());
      List<Location> rando = CollectionUtil.shuffle(ring);
      Location choice = null;
      do {
        loc = rando.remove(rando.size() - 1);
        if (loc == null) {
          break;
        }
        if (u.canTravel(loc)) {
          choice = loc;
        }
      }
      while (choice == null);
      if (choice != null) {
        return u.newMoveOrder(choice);
      }
    }
    return null;
  }
  
  
  
  protected Order planAttack(Unit u, Set<Type> targetable) {
    List<Unit> enemies = u.getOwner().reachableEnemies(u);
    if (!enemies.isEmpty()) {

      Unit closestEnemy = null;
      Location unitLocation = u.getLocation();
      for (Unit enemy : enemies) {
        if (!targetable.contains(enemy.getType())) {
          continue;
        }
        
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
      
      if (closestEnemy != null) {
        return u.newMoveOrder(closestEnemy.getLocation());
      }
    }
    return null;
  }
  
  
  protected Order attackShipStrategy(Unit u, Set<Type> primary, Set<Type> secondary) {
    Order order = planAttack(u, primary);
    if (order == null) {
      order = planAttack(u, secondary);
    }
    
    if (order == null) {
      order = patrolUnloadingZones(u);
    }
    
    if (order == null) {
      order = explore(u);
    }
    
    return order;
  }
  
  
  protected Order occupyLandStrategy(Unit u) {
    Order order = occupyUnownedCity(u);
    
    if (order == null) {
      order = explore(u);
    }
    if (order == null) {
      order = goToLoadingPoint(u);
    }
    if (order == null) {
      order = u.newSkipTurn();
    }
    return order;

  }
  
}
