package com.developingstorm.games.sad.brain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.CollectionUtil;




/**
 * The UnitCaptain is a base class for type specific 'captains'.  The Captain 
 * must analyze the context of the game provided in the BattlePlan and issue a recomendation
 * for the order to assign to the unit.
 * 
 *@param <T>
 */
public abstract class UnitCaptain<T extends Unit>  {
  
  protected Battleplan _plan;
  protected General _general;

  protected UnitCaptain(General general, Battleplan plan) {
    _general = general;
    _plan = plan;
  }
  
  /**
   * Suggest an order for the unit
   * @param u
   * @return
   */
  public abstract Order plan(T u);

  
  /**
   * Build an order to find nearest unoccupied city and attack it
   * @param u
   * @return null if the order could not be constructed 
   */
  protected Order occupyUnownedCity(Unit u) {
    List<City> cities = u.getOwner().reachableCities(u);
    for (City c : cities) {
      if (c != null) {
        Player cityOwner = c.getOwner();
        if (cityOwner != null && !cityOwner.equals(u.getOwner())) {
          Log.info(u, "Moving to enemy city:" + c);
          return u.newMoveOrder(c.getLocation());
        }
        else if (cityOwner == null) {
          Log.info(u, "Moving to unoccupied city:" + c);
          return u.newMoveOrder(c.getLocation());
        }
      }
    }
    return null;
  }
  
  /**
   * Build an order to find a reachable loading point, go to it
   * @param u
   * @return null if the order could not be constructed 
   */
  protected Order goToLoadingPoint(Unit u) {
    Set<Location> loadingPoints = _plan.getLoadingPoints();
    Set<Location> validLoadingPoints = loadingPoints;
    
    if (u.isCarried()) {
      return null;
    }
    
    
    if (u.getTravel().equals(Travel.LAND)) {
      
      validLoadingPoints = new HashSet<Location>();
      Continent cont = u.getContinent();
      if (cont == null) {
        throw new SaDException("Land units must be on a continent! " + u.getLocation());
      }
      for (Location loc : loadingPoints) {
        Continent cont2 = _plan.getBoard().getContinent(loc);
        if (cont.equals(cont2)) {
          validLoadingPoints.add(loc);
        }
      }
    } 
    
    
    Location loc = u.getClosestLocation(validLoadingPoints);
    if (loc != null) {
      Log.info(u, "Going to load point");
      return u.newMoveOrder(loc);
    }
    else {
      return null;
    }
  }
  
  protected Order patrol(Unit u) {
    Location loc;
    List<Location> ring = u.getLocation().getRing(u.getMaxTravel());
    List<Location> rando = CollectionUtil.shuffle(ring);
    Location choice = null;
    do {
      if (rando.size() == 0) {
        break;
      }
      loc = rando.remove(rando.size() - 1);
      if (loc == null) {
        break;
      }
      if (u.canTravel(loc)) {
        choice = loc;
      }
    }
    while (choice == null && rando.size() > 0);
    if (choice != null) {
      Log.info(u, "Patrolling");
      return u.newMoveOrder(choice);
    }
    return null;
  }
  
    
  /**
   * Build an order to reach the frontier, go to it.
   * @param u
   * @return null if the order could not be constructed 
   */
  protected Order explore(Unit u) {
    ArrayList<Location> frontierLocations = u.getOwner().getFrontier(u);
    Location ul = u.getLocation();
    Location loc = ul.closest(frontierLocations);
    if (loc != null) { 
      Log.info(u, "Going exploring");
      return u.newExploreOrder();
    }
    return null;
  }
  
  /**
   * Build an order telling the unit to unload
   * @param u
   * @return
   */
  protected Order unload(Unit u) {
    Log.info(u, "Unloading");
    return u.newUnloadOrder();
  }

  
  /**
   * Build an order telling unit to go to a loading point
   * @param u
   * @return
   */
  protected Order goToUnloadingPoint(Unit u) {
    Location loc = u.getClosestLocation(_plan.getUnloadingPoints());
    if (loc != null) {
      Log.info(u, "Going to unloading point");
      return u.newMoveOrder(loc);
    }
    else {
      return null;
    }
  }
  
  /**
   *  Build an order telling the unit to sleep
   * @param u
   * @return
   */
  protected Order sentry(Unit u) {
    Log.info(u, "Waiting for units!!!!");
    return u.newSentryOrder();    
  }

  
  /**
   * Is the unit at a loading point
   * @param u
   * @return
   */
  protected boolean atLoadingPoint(Unit u) {
    return _plan.getLoadingPoints().contains(u.getLocation());
  }

  /**
   * Is the unit at an unloading point
   * @param u
   * @return
   */
  protected boolean atUnloadPoint(Unit u) {
    return _plan.getUnloadingPoints().contains(u.getLocation());
  }
  
  /**
   * Build an order specifying the unit proceed to the area of the unloading point and patrol
   * @param u
   * @return
   */
  protected Order patrolUnloadingZones(Unit u) {
    Location loc = u.getClosestLocation(_plan.getUnloadingPoints());
    if (loc == null) {
      return null;
    }
    if (loc.distance(u.getLocation()) > 10) {
      Log.info(u, "Moving to unload zone");
      return u.newMoveOrder(loc);
    } else {
      return patrol(u);
    }
  }
  
  
  /**
   * Build an order telling the unit to attack any units of the targetable types in the vicinity
   * @param u
   * @param targetable
   * @return
   */
  @SuppressWarnings("static-method")
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
        Log.info(u, "Moving to attack unit at " + closestEnemy.getLocation());
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

    if (order == null) {
      order = patrol(u);
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
      Log.info(u, "Nothing to do");
      order = u.newSkipTurn();
    }
    return order;

  }
  
}
