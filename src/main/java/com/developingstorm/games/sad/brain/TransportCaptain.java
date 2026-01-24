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

    @Override
    public Order plan(Transport u) {
        Order order = null;

        // If we have cargo and are at an unloading point, unload
        if (u.hasCargo() && atUnloadPoint(u)) {
            return unload(u);
        }

        // If we have cargo, head to unloading point
        if (u.hasCargo()) {
            // Check if we're in dangerous waters and need an escort
            if (isInDanger(u) && !hasNearbyEscort(u)) {
                Log.info(u, "Waiting for escort in dangerous waters");
                return sentry(u); // Wait for escort
            }

            order = goToUnloadingPoint(u);
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
