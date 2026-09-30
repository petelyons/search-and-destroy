package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.commit.Commitment;
import com.developingstorm.games.sad.brain.commit.CommitmentKind;
import com.developingstorm.games.sad.util.Log;

/**
 * Plans infantry and armor: amphibious operation first, then local defense, then expansion.
 * Every goal chosen here is recorded as a commitment so the unit keeps it across turns.
 */
public class LandCaptain extends UnitCaptain<Unit> {

    /** Enemy distance at which a land unit drops what it is doing to defend. */
    private static final int ENGAGE_RADIUS = 3;

    public LandCaptain(General gen, Battleplan plan) {
        super(gen, plan);
    }

    public LandCaptain(
        General gen,
        Battleplan plan,
        OperationsCoordinator coordinator
    ) {
        super(gen, plan, coordinator);
    }

    @Override
    public Order plan(Unit u) {
        // Check if assigned to an amphibious operation
        if (coordinator != null && coordinator.isAssigned(u)) {
            Order operationOrder = coordinator.getOperationOrder(u);
            if (operationOrder != null) {
                return committed(u, CommitmentKind.OPERATION, null, operationOrder);
            }
        }

        // Land units can defend, but not if:
        // - Being carried on a transport (in transit for amphibious ops)
        // - In a city producing them (just spawned, need to get to front)
        if (canDefend(u)) {
            Order defensiveOrder = executeDefensiveStrategy(u);
            if (defensiveOrder != null) {
                City home = nearestOwnedCity(u);
                return home == null
                    ? defensiveOrder
                    : committed(u, CommitmentKind.DEFEND, home.getLocation(), defensiveOrder);
            }
        }

        // Otherwise, focus on expansion/occupation
        return occupyLandStrategy(u);
    }

    private Order occupyLandStrategy(Unit u) {
        // If on transport and ready to disembark, move to adjacent land
        if (u.isCarried()) {
            if (needsToDisembark(u)) {
                return disembarkFromTransport(u);
            }
            return sentry(u);
        }

        // First try to reach a city this turn
        Order order = occupyUnownedCity(u);
        if (order != null) {
            return committed(u, CommitmentKind.CAPTURE_CITY, lastTargetCity, order);
        }

        // If no city reachable this turn, move toward the best strategic city target
        order = moveTowardBestCity(u);
        if (order != null && lastTargetCity != null) {
            return committed(u, CommitmentKind.CAPTURE_CITY, lastTargetCity, order);
        }

        // Best targets are overseas (or there are none): finish exploring home before shipping out
        Order exploreOrder = explore(u);
        if (exploreOrder != null) {
            return committed(u, CommitmentKind.EXPLORE, null, exploreOrder);
        }

        if (order != null) {
            return lastLoadingPoint != null ? stage(u, lastLoadingPoint) : order;
        }

        // If can't explore, go to loading point for transport
        order = goToLoadingPoint(u);
        if (order != null && lastLoadingPoint != null) {
            return stage(u, lastLoadingPoint);
        }
        if (order != null) {
            return order;
        }

        // Last resort: skip turn
        Log.info(u, "Nothing to do");
        return u.newSkipTurn();
    }

    /** Commit to waiting at a loading point: move there, then sentry so a transport picks us up. */
    private Order stage(Unit u, Location point) {
        Order o = u.getLocation().equals(point) ? u.newSentryOrder() : u.newMoveOrder(point);
        return committed(u, CommitmentKind.STAGE, point, o);
    }

    @Override
    public Order continueCommitment(Unit u, Commitment c) {
        switch (c.kind) {
            case OPERATION:
                return coordinator == null ? null : coordinator.getOperationOrder(u);
            case DEFEND: {
                Order o = executeDefensiveStrategy(u);
                return o != null ? o : patrol(u);
            }
            case CAPTURE_CITY:
                return u.getLocation().equals(c.target) ? null : u.newMoveOrder(c.target);
            case STAGE:
                return u.getLocation().equals(c.target) ? u.newSentryOrder() : u.newMoveOrder(c.target);
            case EXPLORE:
                return explore(u);
            default:
                return null;
        }
    }

    @Override
    public String preemption(Unit u, Commitment c) {
        if (c.kind != CommitmentKind.OPERATION && hasOperationOrder(u)) {
            return "operation assignment";
        }
        if (CommitmentKind.DEFEND.outranks(c.kind) && canDefend(u) && shouldDefend(u)) {
            Unit enemy = plan.getThreatMap().getClosestEnemy(u.getLocation());
            if (enemy != null && u.getLocation().distance(enemy.getLocation()) <= ENGAGE_RADIUS) {
                return "defense";
            }
        }
        if ((c.kind == CommitmentKind.EXPLORE || c.kind == CommitmentKind.STAGE) && !u.isCarried()) {
            City best = plan.getBestCityTarget(u);
            if (best != null && best.getContinent() == u.getContinent()) {
                Path p = u.getPath(best.getLocation());
                if (p != null && p.length() > 0) {
                    return "city opportunity";
                }
            }
        }
        return null;
    }

    private boolean canDefend(Unit u) {
        return !u.isCarried() && !isInProducingCity(u);
    }

    private City nearestOwnedCity(Unit u) {
        City best = null;
        int bestDist = Integer.MAX_VALUE;
        for (City c : plan.getPlayer().getCities()) {
            int d = u.getLocation().distance(c.getLocation());
            if (d < bestDist) {
                best = c;
                bestDist = d;
            }
        }
        return best;
    }

    /**
     * Check if unit is in a city that just produced it
     */
    private boolean isInProducingCity(Unit u) {
        return isJustProduced(
            u,
            plan.getGame().getTurn(),
            plan.getBoard().getCity(u.getLocation()) != null
        );
    }

    static boolean isJustProduced(Unit u, int currentTurn, boolean inCity) {
        return inCity && u.getCreatedTurn() == currentTurn;
    }
}
