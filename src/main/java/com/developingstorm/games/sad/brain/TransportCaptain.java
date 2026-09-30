package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.types.Transport;
import com.developingstorm.games.sad.util.Log;
import java.util.List;

public class TransportCaptain extends UnitCaptain<Transport> {

    public TransportCaptain(General gen, Battleplan plan) {
        super(gen, plan);
    }

    public TransportCaptain(
        General gen,
        Battleplan plan,
        OperationsCoordinator coordinator
    ) {
        super(gen, plan, coordinator);
    }

    @Override
    public Order plan(Transport u) {
        // If we have cargo and are at an unloading point, unload immediately
        // This takes priority even for operations (unloading is the goal!)
        if (u.hasCargo() && atUnloadPoint(u)) {
            return unload(u);
        }

        // Check if assigned to an amphibious operation
        if (coordinator != null && coordinator.isAssigned(u)) {
            Order operationOrder = coordinator.getOperationOrder(u);
            if (operationOrder != null) {
                return operationOrder;
            }
        }

        Order order = null;

        // If we have cargo, head to expansion unloading point (prioritize areas with unoccupied cities)
        if (u.hasCargo()) {
            // Check if we're in dangerous waters and need an escort
            if (isInDanger(u) && !hasNearbyEscort(u)) {
                Log.info(u, "Waiting for escort in dangerous waters");
                return sentry(u); // Wait for escort
            }

            // Prioritize expansion zones with unoccupied cities
            order = goToExpansionUnloadingPoint(u);
            if (order == null) {
                order = goToUnloadingPoint(u);
            }
            if (order == null) {
                order = explore(u);
            }
            return order;
        }

        // If we're at a loading point, wait to pick up cargo
        if (atLoadingPoint(u)) {
            return sentry(u);
        }

        // Head to a loading point
        order = goToLoadingPoint(u);
        if (order == null) {
            order = explore(u);
        }
        return order;
    }

    /**
     * Route transport to expansion zones (areas near unoccupied cities).
     * This prioritizes delivering troops to continents with unoccupied cities.
     */
    private Order goToExpansionUnloadingPoint(Transport u) {
        Location closestExpansion = u.getClosestLocation(
            plan.getExpandUnloadingPoints()
        );
        if (closestExpansion != null) {
            Log.info(
                u,
                "Going to expansion unloading point (near unoccupied cities)"
            );
            return u.newMoveOrder(closestExpansion);
        }
        return null;
    }

    /**
     * Check if this transport is in dangerous waters (nearby enemies)
     */
    private boolean isInDanger(Transport u) {
        ThreatMap threatMap = plan.getThreatMap();
        double threatLevel = threatMap.getThreatLevel(u.getLocation());

        // If there are nearby enemies, we're in danger
        List<Unit> nearbyEnemies = threatMap.getNearbyEnemies(
            u.getLocation(),
            3
        );
        return !nearbyEnemies.isEmpty() || threatLevel > 1.0;
    }

    /**
     * Check if there's a friendly destroyer nearby that can escort us
     */
    private boolean hasNearbyEscort(Transport u) {
        List<Unit> friendlyUnits = plan.getPlayer().getUnits();
        Location transportLoc = u.getLocation();

        // Look for destroyers or cruisers within 2 hexes
        for (Unit unit : friendlyUnits) {
            if (
                unit.getType() == Type.DESTROYER ||
                unit.getType() == Type.CRUISER
            ) {
                int distance = transportLoc.distance(unit.getLocation());
                if (distance <= 2) {
                    return true; // We have an escort
                }
            }
        }

        return false; // No escort nearby
    }
}
