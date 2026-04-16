package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Contact;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.types.Destroyer;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.CollectionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DestroyerCaptain extends UnitCaptain<Destroyer> {

    static final Type[] PrimaryTargetTypes = new Type[] {
        Type.TRANSPORT,
        Type.SUBMARINE,
    };
    static final Set<Type> PrimaryTargets = CollectionUtil.create(
        PrimaryTargetTypes
    );

    static final Type[] SecondaryTargetTypes = new Type[] {
        Type.DESTROYER,
        Type.BOMBER,
        Type.FIGHTER,
    };
    static final Set<Type> SecondaryTargets = CollectionUtil.create(
        SecondaryTargetTypes
    );

    public DestroyerCaptain(General gen, Battleplan plan) {
        super(gen, plan);
    }

    @Override
    public Order plan(Destroyer u) {
        StrategyMemory memory = plan.getStrategyMemory();

        // Check if this destroyer is assigned as an escort
        StrategyMemory.UnitRole role = memory.getUnitRole(u);
        if (role == StrategyMemory.UnitRole.ESCORT) {
            Order escortOrder = escortTransports(u);
            if (escortOrder != null) {
                return escortOrder;
            }
        }

        // Try to find transports that need escorting
        Order escortOrder = findAndEscortTransport(u);
        if (escortOrder != null) {
            // Assign as escort if we're escorting
            memory.setUnitRole(u, StrategyMemory.UnitRole.ESCORT);
            return escortOrder;
        }

        // No live naval target — investigate sea ghost contacts if we have any
        Location ghost = findGhostTarget(u);
        if (ghost != null) {
            Log.info(u, "Investigating sea ghost at " + ghost);
            return u.newMoveOrder(ghost);
        }

        // Otherwise, use normal attack strategy
        return attackShipStrategy(u, PrimaryTargets, SecondaryTargets);
    }

    /**
     * Returns the location of the best sea-ghost contact this captain should
     * investigate, or null if no useful ghost exists. Prefers identified
     * ghosts of higher-value targets; falls back to unidentified SEA ghosts.
     */
    private Location findGhostTarget(Unit unit) {
        Player owner = unit.getOwner();
        int currentTurn = owner.getGame().getTurn();

        Location bestLoc = null;
        double bestScore = -1.0;

        for (Contact c : owner.getContactMap().getAllContacts()) {
            if (c.isCurrent(currentTurn)) continue;
            if (c.travelType != Travel.SEA) continue;

            double score;
            if (c.identified && c.type != null) {
                score = c.type.getAttack() + 0.5;
            } else {
                score = 0.5;
            }

            int dist = unit.getLocation().distance(c.location);
            if (dist == 0) continue;
            score = score / (double) dist;

            if (score > bestScore) {
                bestScore = score;
                bestLoc = c.location;
            }
        }
        return bestLoc;
    }

    /**
     * Find nearby transports and escort them
     */
    private Order findAndEscortTransport(Destroyer u) {
        List<Unit> friendlyUnits = plan.getPlayer().getUnits();
        List<Unit> transports = new ArrayList<>();

        // Find all friendly transports
        for (Unit unit : friendlyUnits) {
            if (unit.getType() == Type.TRANSPORT) {
                transports.add(unit);
            }
        }

        if (transports.isEmpty()) {
            return null;
        }

        // Find the closest transport
        Location destroyerLoc = u.getLocation();
        Unit closestTransport = null;
        int closestDistance = Integer.MAX_VALUE;

        for (Unit transport : transports) {
            int distance = destroyerLoc.distance(transport.getLocation());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTransport = transport;
            }
        }

        // If there's a transport within reasonable range, escort it
        if (closestTransport != null && closestDistance <= 8) {
            return escortUnit(u, closestTransport);
        }

        return null;
    }

    /**
     * Escort a specific unit (stay near it and protect it)
     */
    private Order escortUnit(Destroyer u, Unit transportToEscort) {
        Location destroyerLoc = u.getLocation();
        Location transportLoc = transportToEscort.getLocation();
        int distance = destroyerLoc.distance(transportLoc);

        // Check for nearby threats to the transport
        ThreatMap threatMap = plan.getThreatMap();
        List<Unit> nearbyEnemies = threatMap.getNearbyEnemies(transportLoc, 3);

        // If there are enemies near the transport, engage them
        if (!nearbyEnemies.isEmpty()) {
            Unit closestThreat = null;
            int closestThreatDistance = Integer.MAX_VALUE;

            for (Unit enemy : nearbyEnemies) {
                int dist = transportLoc.distance(enemy.getLocation());
                if (dist < closestThreatDistance) {
                    closestThreatDistance = dist;
                    closestThreat = enemy;
                }
            }

            if (closestThreat != null) {
                Log.info(
                    u,
                    "Protecting transport from enemy at " +
                        closestThreat.getLocation()
                );
                return u.newMoveOrder(closestThreat.getLocation());
            }
        }

        // Stay close to the transport (within 2 hexes)
        if (distance > 2) {
            Log.info(u, "Moving to escort transport at " + transportLoc);
            return u.newMoveOrder(transportLoc);
        } else if (distance == 0) {
            // We're on top of the transport, move to an adjacent hex
            List<Location> adjacentLocations = transportLoc.getRing(1);
            for (Location adj : adjacentLocations) {
                if (u.canTravel(adj)) {
                    Log.info(u, "Positioning near transport");
                    return u.newMoveOrder(adj);
                }
            }
        }

        // We're at a good escort distance, patrol nearby
        Log.info(u, "Escorting transport");
        return patrol(u);
    }

    /**
     * Continue escorting transports (for destroyers already assigned as escorts)
     */
    private Order escortTransports(Destroyer u) {
        // Find the nearest transport to continue escorting
        List<Unit> friendlyUnits = plan.getPlayer().getUnits();
        Unit closestTransport = null;
        int closestDistance = Integer.MAX_VALUE;
        Location destroyerLoc = u.getLocation();

        for (Unit unit : friendlyUnits) {
            if (unit.getType() == Type.TRANSPORT) {
                int distance = destroyerLoc.distance(unit.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTransport = unit;
                }
            }
        }

        if (closestTransport != null) {
            return escortUnit(u, closestTransport);
        }

        // No transports to escort
        return null;
    }
}
