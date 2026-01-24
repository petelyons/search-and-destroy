package com.developingstorm.games.sad.brain;

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
import com.developingstorm.util.RandomUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The UnitCaptain is a base class for type specific 'captains'.  The Captain
 * must analyze the context of the game provided in the BattlePlan and issue a recommendation
 * for the order to assign to the unit.
 *
 *@param <T>
 */
public abstract class UnitCaptain<T extends Unit> {

    protected Battleplan plan;
    protected General general;

    protected UnitCaptain(General general, Battleplan plan) {
        this.general = general;
        this.plan = plan;
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
                } else if (cityOwner == null) {
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
        Set<Location> loadingPoints = this.plan.getLoadingPoints();
        Set<Location> validLoadingPoints = loadingPoints;

        if (u.isCarried()) {
            return null;
        }

        if (u.getTravel().equals(Travel.LAND)) {
            validLoadingPoints = new HashSet<Location>();
            Continent cont = u.getContinent();
            if (cont == null) {
                throw new SaDException(
                    "Land units must be on a continent! " + u.getLocation()
                );
            }
            for (Location loc : loadingPoints) {
                Continent cont2 = this.plan.getBoard().getContinent(loc);
                if (cont.equals(cont2)) {
                    validLoadingPoints.add(loc);
                }
            }
        }

        Location loc = u.getClosestLocation(validLoadingPoints);
        if (loc != null) {
            Log.info(u, "Going to load point");
            return u.newMoveOrder(loc);
        } else {
            return null;
        }
    }

    protected Order patrol(Unit u) {
        Location loc;

        int patrolDist = u.getType().getDist();

        List<Location> ring = u.getLocation().getRing(patrolDist);
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
        } while (choice == null && rando.size() > 0);
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
        Location loc = u.getClosestLocation(this.general.getUnloadingZone());
        if (loc != null) {
            Log.info(u, "Going to unloading point");
            return u.newMoveOrder(loc);
        } else {
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
        return this.plan.getLoadingPoints().contains(u.getLocation());
    }

    /**
     * Is the unit at an unloading point
     * @param u
     * @return
     */
    protected boolean atUnloadPoint(Unit u) {
        return (
            this.plan.getDefenseUnloadingPoints().contains(u.getLocation()) ||
            this.plan.getExpandUnloadingPoints().contains(u.getLocation())
        );
    }

    /**
     * Build an order specifying the unit proceed to the area of the unloading point and patrol
     * @param u
     * @return
     */
    protected Order patrolUnloadingZones(Unit u) {
        Location loc = u.getClosestLocation(
            this.plan.getDefenseUnloadingPoints()
        );
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
                    Location knownClosestEnemyLocation =
                        closestEnemy.getLocation();
                    Location enemyLocation = enemy.getLocation();
                    Path pathToEnemy = u.getPath(enemyLocation);
                    if (pathToEnemy != null && !pathToEnemy.isEmpty()) {
                        if (
                            unitLocation.distance(enemyLocation) <
                            unitLocation.distance(knownClosestEnemyLocation)
                        ) {
                            closestEnemy = enemy;
                        }
                    }
                }
            }

            if (closestEnemy != null) {
                Log.info(
                    u,
                    "Moving to attack unit at " + closestEnemy.getLocation()
                );
                return u.newMoveOrder(closestEnemy.getLocation());
            }
        }
        return null;
    }

    protected Order attackShipStrategy(
        Unit u,
        Set<Type> primary,
        Set<Type> secondary
    ) {
        Order order = planAttack(u, primary);
        if (order == null) {
            order = planAttack(u, secondary);
        }

        List<Order> list = new ArrayList<Order>();
        order = patrolUnloadingZones(u);
        if (order != null) {
            list.add(order);
        }
        order = explore(u);
        if (order != null) {
            list.add(order);
        }
        order = patrol(u);
        if (order != null) {
            list.add(order);
        }

        if (list.isEmpty()) {
            return null;
        }

        order = RandomUtil.randomValue(list);
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

    /**
     * Check if this unit should be defending based on threat assessment
     * @param u the unit to check
     * @return true if the unit should prioritize defense
     */
    protected boolean shouldDefend(Unit u) {
        ThreatMap threatMap = plan.getThreatMap();
        StrategyMemory memory = plan.getStrategyMemory();

        // Check if unit is assigned as a defender
        StrategyMemory.UnitRole role = memory.getUnitRole(u);
        if (role == StrategyMemory.UnitRole.DEFENDER) {
            return true;
        }

        // Check if unit is on a threatened continent
        Continent cont = u.getContinent();
        if (cont != null && threatMap.getContinentThreatLevel(cont) > 2.0) {
            // Assign as defender if not already assigned
            if (role == null || role == StrategyMemory.UnitRole.SCOUT) {
                memory.setUnitRole(u, StrategyMemory.UnitRole.DEFENDER);
            }
            return true;
        }

        return false;
    }

    /**
     * Find the best defensive position for this unit
     * @param u the unit to position
     * @return an order to move to a defensive position, or null if already positioned
     */
    protected Order moveToDefensivePosition(Unit u) {
        ThreatMap threatMap = plan.getThreatMap();

        // Find the closest threatened city
        Set<Location> threatenedCitiesSet = threatMap.getThreatenedCities();
        if (threatenedCitiesSet.isEmpty()) {
            return null;
        }
        ArrayList<Location> threatenedCities = new ArrayList<>(
            threatenedCitiesSet
        );

        Location unitLoc = u.getLocation();
        Location closestThreat = unitLoc.closest(threatenedCities);

        if (closestThreat == null) {
            return null;
        }

        // If we're already very close to the threatened city, stay put or patrol nearby
        if (unitLoc.distance(closestThreat) <= 2) {
            Log.info(u, "Defending city at " + closestThreat);
            return patrol(u); // Patrol near the city
        }

        // Move toward the threatened city
        Log.info(u, "Moving to defensive position near " + closestThreat);
        return u.newMoveOrder(closestThreat);
    }

    /**
     * Respond to nearby enemies with defensive tactics
     * @param u the unit
     * @return an order to engage enemies, or null if no threats nearby
     */
    protected Order defendAgainstNearbyThreats(Unit u) {
        ThreatMap threatMap = plan.getThreatMap();
        Unit closestEnemy = threatMap.getClosestEnemy(u.getLocation());

        if (closestEnemy == null) {
            return null;
        }

        Location unitLoc = u.getLocation();
        Location enemyLoc = closestEnemy.getLocation();
        int distance = unitLoc.distance(enemyLoc);

        // If enemy is very close (within 3 hexes), engage
        if (distance <= 3) {
            Log.info(u, "Engaging nearby enemy at " + enemyLoc);
            return u.newMoveOrder(enemyLoc);
        }

        return null;
    }

    /**
     * Full defensive strategy: check for threats and respond appropriately
     * @param u the unit
     * @return a defensive order, or null if no defense needed
     */
    protected Order executeDefensiveStrategy(Unit u) {
        if (!shouldDefend(u)) {
            return null;
        }

        // Priority 1: Engage nearby threats
        Order order = defendAgainstNearbyThreats(u);
        if (order != null) {
            return order;
        }

        // Priority 2: Move to defensive position
        order = moveToDefensivePosition(u);
        if (order != null) {
            return order;
        }

        // Priority 3: Patrol current area
        return patrol(u);
    }
}
