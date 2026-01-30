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
    protected OperationsCoordinator coordinator;

    protected UnitCaptain(General general, Battleplan plan) {
        this(general, plan, null);
    }

    protected UnitCaptain(
        General general,
        Battleplan plan,
        OperationsCoordinator coordinator
    ) {
        this.general = general;
        this.plan = plan;
        this.coordinator = coordinator;
    }

    /**
     * Suggest an order for the unit
     * @param u
     * @return
     */
    public abstract Order plan(T u);

    /**
     * Build an order to find best unoccupied city and move toward it.
     * Prioritizes unoccupied cities over enemy cities using the TargetPrioritizer.
     * @param u
     * @return null if the order could not be constructed
     */
    protected Order occupyUnownedCity(Unit u) {
        // Use TargetPrioritizer to get the best city target for this unit
        City bestCity = plan.getBestCityTarget(u);

        if (bestCity != null) {
            Player cityOwner = bestCity.getOwner();
            if (cityOwner == null) {
                Log.info(
                    u,
                    "Moving to prioritized unoccupied city: " + bestCity
                );
            } else {
                Log.info(u, "Moving to prioritized enemy city: " + bestCity);
            }
            return u.newMoveOrder(bestCity.getLocation());
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
    /**
     * Move strategically toward the best city target, even if not reachable this turn.
     * This helps units make progress toward distant unoccupied cities.
     * For land units, if the best cities are on other continents, directs them to loading points.
     * @param u
     * @return null if no strategic target exists
     */
    protected Order moveTowardBestCity(Unit u) {
        // Get all prioritized cities (not just reachable ones)
        List<TargetPrioritizer.CityTarget> cityTargets =
            plan.getPrioritizedCities();

        if (cityTargets.isEmpty()) {
            return null;
        }

        // Check if we're a land unit
        boolean isLandUnit = u.getTravel() == Travel.LAND;
        Continent ourContinent = isLandUnit ? u.getContinent() : null;

        // Find the best city we don't own (prioritizing unoccupied)
        City bestUnreachableCity = null;
        for (TargetPrioritizer.CityTarget target : cityTargets) {
            City city = target.city;
            Location cityLoc = city.getLocation();

            // Try to get a path toward this city
            Path path = u.getPath(cityLoc);
            if (path != null && path.length() > 0) {
                // Move as far as we can toward this city
                Log.info(
                    u,
                    "Moving toward strategic city target: " +
                        city +
                        " (score: " +
                        String.format("%.1f", target.score) +
                        ")"
                );
                return u.newMoveOrder(cityLoc);
            }

            // For land units: track best city on a different continent
            if (
                isLandUnit &&
                bestUnreachableCity == null &&
                ourContinent != null
            ) {
                Continent cityContinent = plan.getBoard().getContinent(cityLoc);
                if (
                    cityContinent != null && !cityContinent.equals(ourContinent)
                ) {
                    bestUnreachableCity = city;
                }
            }
        }

        // If we're a land unit and there's a high-value city on another continent,
        // head to a loading point for transport
        if (isLandUnit && bestUnreachableCity != null) {
            Log.info(
                u,
                "Best city targets on other continent. Heading to transport loading point."
            );
            return goToLoadingPoint(u);
        }

        return null;
    }

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
        // If on transport, wait in sentry mode - transport will move us off when ready
        if (u.isCarried()) {
            return sentry(u);
        }

        // First try to reach a city this turn
        Order order = occupyUnownedCity(u);

        // If no city reachable this turn, move toward the best strategic city target
        if (order == null) {
            order = moveTowardBestCity(u);
        }

        // If no strategic city target, explore the frontier
        if (order == null) {
            order = explore(u);
        }

        // If can't explore, go to loading point for transport
        if (order == null) {
            order = goToLoadingPoint(u);
        }

        // Last resort: skip turn
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

    /**
     * Check if unit needs to disembark from transport
     * @param u the unit
     * @return true if unit is on a transport that is unloading along a coast
     */
    protected boolean needsToDisembark(Unit u) {
        if (u.onboard == null) {
            return false;
        }

        return u.onboard.isUnloadingMode() && u.onboard.isAlongCoast();
    }

    /**
     * Find an adjacent land hex and disembark from transport
     * @param u the unit to disembark
     * @return a move order to disembark, or null if no valid hex found
     */
    protected Order disembarkFromTransport(Unit u) {
        Location transportLoc = u.getLocation();
        List<Location> neighbors = transportLoc.getRing(1);

        // Try to find valid disembark locations
        List<Location> validHexes = new ArrayList<>();

        for (Location adjacent : neighbors) {
            if (isValidDisembarkHex(adjacent, u)) {
                validHexes.add(adjacent);
            }
        }

        if (validHexes.isEmpty()) {
            Log.warn(u, "Cannot disembark - no valid adjacent land hex");
            return u.newSkipTurn();
        }

        // Pick the first valid hex (could be improved with priority logic)
        Location disembarkHex = validHexes.get(0);
        Log.info(u, "Disembarking from transport to " + disembarkHex);
        return u.newMoveOrder(disembarkHex);
    }

    /**
     * Check if a hex is valid for disembarking
     * @param loc the location to check
     * @param u the unit that wants to disembark
     * @return true if the location is valid for disembarking
     */
    protected boolean isValidDisembarkHex(Location loc, Unit u) {
        // Must be on board
        if (!plan.getBoard().onBoard(loc)) {
            return false;
        }

        // Must be land (or coastal city)
        if (plan.getBoard().isWater(loc)) {
            // Water is only OK if there's a city there (coastal city)
            if (!plan.getBoard().isCity(loc)) {
                return false;
            }
        }

        // Check if hex has space (max 3 units per hex)
        int unitsAtLocation = plan.getGame().unitsAtLocation(loc).size();
        if (unitsAtLocation >= 3) {
            return false;
        }

        return true;
    }
}
