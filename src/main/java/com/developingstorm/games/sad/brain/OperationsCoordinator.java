package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.StrategyMemory.InvasionPlan;
import com.developingstorm.games.sad.brain.StrategyMemory.OperationPhase;
import com.developingstorm.games.sad.brain.TargetPrioritizer.CityTarget;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coordinates multi-turn amphibious operations by:
 * - Creating invasion plans for high-value targets
 * - Assigning specific units to operations
 * - Managing operation phases (PREPARING -> LOADING -> TRANSIT -> LANDING -> EXECUTING)
 * - Providing phase-specific orders to units
 */
public class OperationsCoordinator {

    private final Player player;
    private final StrategyMemory memory;
    private Battleplan battleplan;

    // Track which units are assigned to which operations
    private Map<Long, InvasionPlan> unitAssignments;

    // Maximum simultaneous operations
    private static final int MAX_OPERATIONS = 3;

    // Turn counter
    private int currentTurn;

    public OperationsCoordinator(Player player, StrategyMemory memory) {
        this.player = player;
        this.memory = memory;
        this.unitAssignments = new HashMap<>();
        this.currentTurn = 0;
    }

    /**
     * Main coordination method called once per turn before unit planning.
     * Analyzes strategic situation, creates/updates invasion plans, assigns units.
     */
    public void planOperations(Battleplan plan) {
        this.battleplan = plan;
        this.currentTurn++;

        // Call memory's start new turn to age old plans
        memory.startNewTurn();

        // Clean up unit assignments (remove dead units)
        cleanupDeadUnits();

        // Advance existing operations through their phases
        advanceExistingOperations();

        // Abort stale operations
        abortStaleOperations();

        // Create new operations if we have capacity
        createNewOperations();
    }

    /**
     * Check if a unit is assigned to an operation.
     */
    public boolean isAssigned(Unit unit) {
        return unitAssignments.containsKey(unit.id);
    }

    /**
     * Get the operation a unit is assigned to.
     */
    public InvasionPlan getAssignment(Unit unit) {
        return unitAssignments.get(unit.id);
    }

    /**
     * Get operation-specific orders for a unit.
     * Returns null if no specific order (unit should use default behavior).
     */
    public Order getOperationOrder(Unit unit) {
        InvasionPlan operation = getAssignment(unit);
        if (operation == null) {
            return null;
        }

        // Generate order based on unit role and operation phase
        return generatePhaseSpecificOrder(unit, operation);
    }

    /**
     * Clean up dead units from all assignments.
     */
    private void cleanupDeadUnits() {
        List<Long> deadIds = new ArrayList<>();

        // Find unit by ID from player's unit list
        for (Map.Entry<Long, InvasionPlan> entry : unitAssignments.entrySet()) {
            Long unitId = entry.getKey();
            Unit foundUnit = null;

            for (Unit u : player.getUnits()) {
                if (u.id == unitId) {
                    foundUnit = u;
                    break;
                }
            }

            if (foundUnit == null || foundUnit.isDead()) {
                deadIds.add(unitId);
                if (foundUnit != null) {
                    entry.getValue().removeUnit(foundUnit);
                }
            }
        }

        for (Long id : deadIds) {
            unitAssignments.remove(id);
        }
    }

    /**
     * Advance existing operations through their phases.
     */
    private void advanceExistingOperations() {
        for (InvasionPlan op : memory.getActiveInvasions()) {
            if (
                op.phase == OperationPhase.COMPLETED ||
                op.phase == OperationPhase.ABORTED
            ) {
                continue;
            }

            // Check if we can advance to next phase
            switch (op.phase) {
                case PLANNING:
                    // Advance to PREPARING once we have minimum units assigned
                    if (op.readyToLaunch) {
                        op.advancePhase(OperationPhase.PREPARING, currentTurn);
                        Log.info(
                            "Operation for " +
                                op.targetCity +
                                " advancing to PREPARING phase"
                        );
                    }
                    break;
                case PREPARING:
                    // Advance to LOADING when all units near rally point
                    if (checkAllUnitsAtRallyPoint(op)) {
                        op.advancePhase(OperationPhase.LOADING, currentTurn);
                        Log.info(
                            "Operation for " +
                                op.targetCity +
                                " advancing to LOADING phase"
                        );
                    }
                    break;
                case LOADING:
                    // Advance to TRANSIT when transports are loaded
                    if (checkTransportsLoaded(op)) {
                        op.advancePhase(OperationPhase.TRANSIT, currentTurn);
                        Log.info(
                            "Operation for " +
                                op.targetCity +
                                " advancing to TRANSIT phase"
                        );
                    }
                    break;
                case TRANSIT:
                    // Advance to LANDING when transports reach unload point
                    if (checkTransportsAtUnloadPoint(op)) {
                        op.advancePhase(OperationPhase.LANDING, currentTurn);
                        Log.info(
                            "Operation for " +
                                op.targetCity +
                                " advancing to LANDING phase"
                        );
                    }
                    break;
                case LANDING:
                    // Advance to EXECUTING when cargo is on shore
                    if (checkCargoOnShore(op)) {
                        op.advancePhase(OperationPhase.EXECUTING, currentTurn);
                        Log.info(
                            "Operation for " +
                                op.targetCity +
                                " advancing to EXECUTING phase"
                        );
                    }
                    break;
                case EXECUTING:
                    // Advance to COMPLETED when target city is captured
                    if (checkTargetCaptured(op)) {
                        op.advancePhase(OperationPhase.COMPLETED, currentTurn);
                        Log.info(
                            "Operation for " +
                                op.targetCity +
                                " COMPLETED successfully!"
                        );
                        releaseUnits(op);
                    }
                    break;
            }
        }
    }

    /**
     * Abort operations that are stale (stuck for too long).
     */
    private void abortStaleOperations() {
        for (InvasionPlan op : memory.getActiveInvasions()) {
            if (
                op.isStale(currentTurn) && op.phase != OperationPhase.COMPLETED
            ) {
                Log.warn(
                    "Aborting stale operation for " +
                        op.targetCity +
                        " stuck in " +
                        op.phase
                );
                op.advancePhase(OperationPhase.ABORTED, currentTurn);
                releaseUnits(op);
            }
        }
    }

    /**
     * Create new invasion operations if we have capacity.
     */
    private void createNewOperations() {
        // Count active operations
        int activeOps = 0;
        for (InvasionPlan op : memory.getActiveInvasions()) {
            if (
                op.phase != OperationPhase.COMPLETED &&
                op.phase != OperationPhase.ABORTED
            ) {
                activeOps++;
            }
        }

        if (activeOps >= MAX_OPERATIONS) {
            Log.debug("At max operations capacity (" + MAX_OPERATIONS + ")");
            return; // At capacity
        }

        // Find high-value invasion targets
        List<CityTarget> targets = battleplan.getPrioritizedCities();

        if (targets.isEmpty()) {
            Log.debug("No enemy/unoccupied cities to target");
            return;
        }

        Log.debug(
            "Evaluating " + targets.size() + " potential amphibious targets"
        );

        for (CityTarget target : targets) {
            if (activeOps >= MAX_OPERATIONS) {
                break;
            }

            City city = target.city;
            Location cityLoc = city.getLocation();

            // Skip if already targeting this city
            if (memory.hasInvasionPlan(cityLoc)) {
                continue;
            }

            // Check if city is on a different continent (requires amphibious assault)
            Continent cityCont = battleplan.getBoard().getContinent(cityLoc);
            if (cityCont == null) {
                continue;
            }

            // Check if we have LAND UNITS (infantry/armor) on this continent already
            // Simple continent check - if we have any infantry/armor on the same continent,
            // assume they can handle it (don't do expensive pathfinding here)
            boolean hasLandUnitsOnContinent = false;
            for (Unit u : player.getUnits()) {
                if (u.isDead()) continue;
                if (!u.isInfantry() && !u.isArmour()) continue; // Only count capture-capable units

                if (
                    u.getContinent() != null &&
                    u.getContinent().equals(cityCont)
                ) {
                    hasLandUnitsOnContinent = true;
                    break;
                }
            }

            if (hasLandUnitsOnContinent) {
                continue; // Land units on same continent, no amphibious operation needed
            }

            // This city needs an amphibious operation
            Log.info(
                "Identified amphibious target: " +
                    cityLoc +
                    " on continent with no land units"
            );

            // Create invasion plan
            InvasionPlan operation = memory.getOrCreateInvasionPlan(cityLoc);
            operation.rallyPoint = findBestLoadingPoint();
            operation.unloadPoint = findBestUnloadPoint(cityLoc, cityCont);

            if (operation.rallyPoint == null || operation.unloadPoint == null) {
                continue; // Can't plan this operation
            }

            Log.info("Creating new invasion operation for " + cityLoc);

            // Try to assign available units
            // Don't remove the operation if assignment fails - it will try again next turn
            // Operations are only removed if they become truly stale (see abortStaleOperations)
            assignUnitsToOperation(operation);

            activeOps++;
        }
    }

    /**
     * Assign available units to an operation.
     * Returns true if minimum requirements met (at least 1 transport + 2 cargo).
     */
    private boolean assignUnitsToOperation(InvasionPlan operation) {
        int transportsNeeded = operation.requiredTransports;
        int cargoNeeded = operation.requiredCargo;
        int transportsAssigned = 0;
        int cargoAssigned = 0;

        // Find available transports
        for (Unit u : player.getUnits()) {
            if (u.isDead() || isAssigned(u)) continue;
            if (u.isTransport() && transportsNeeded > 0) {
                operation.addUnit(u);
                unitAssignments.put(u.id, operation);
                memory.setUnitRole(u, StrategyMemory.UnitRole.INVADER);
                transportsNeeded--;
                transportsAssigned++;

                // If transport already has cargo, assign those units to the operation too
                if (u.hasCargo() && u.carries != null) {
                    for (Unit carried : u.carries) {
                        if (
                            !isAssigned(carried) &&
                            (carried.isInfantry() || carried.isArmour())
                        ) {
                            operation.addUnit(carried);
                            unitAssignments.put(carried.id, operation);
                            memory.setUnitRole(
                                carried,
                                StrategyMemory.UnitRole.INVADER
                            );
                            cargoAssigned++;
                            Log.info(
                                "Assigned carried cargo " +
                                    carried +
                                    " to operation"
                            );
                        }
                    }
                }

                Log.info(
                    "Assigned transport " +
                        u +
                        " to operation for " +
                        operation.targetCity
                );
            }
        }

        // Find available cargo units (prefer non-combat units)
        for (Unit u : player.getUnits()) {
            if (u.isDead() || isAssigned(u)) continue;
            if ((u.isInfantry() || u.isArmour()) && cargoNeeded > 0) {
                // Prefer units not in high-threat combat
                if (isInHighThreatZone(u)) continue;

                operation.addUnit(u);
                unitAssignments.put(u.id, operation);
                memory.setUnitRole(u, StrategyMemory.UnitRole.INVADER);
                cargoNeeded--;
                cargoAssigned++;
                Log.info(
                    "Assigned cargo " +
                        u +
                        " to operation for " +
                        operation.targetCity
                );
            }
        }

        // If still need cargo, take units from lower-threat zones
        if (cargoNeeded > 0) {
            for (Unit u : player.getUnits()) {
                if (u.isDead() || isAssigned(u)) continue;
                if ((u.isInfantry() || u.isArmour()) && cargoNeeded > 0) {
                    operation.addUnit(u);
                    unitAssignments.put(u.id, operation);
                    memory.setUnitRole(u, StrategyMemory.UnitRole.INVADER);
                    cargoNeeded--;
                    cargoAssigned++;
                    Log.info(
                        "Assigned cargo " +
                            u +
                            " (from combat zone) to operation for " +
                            operation.targetCity
                    );
                }
            }
        }

        // Minimum viable operation: 1 transport + 2 cargo
        boolean viable = transportsAssigned >= 1 && cargoAssigned >= 2;
        if (!viable) {
            Log.warn(
                "Operation for " +
                    operation.targetCity +
                    " not viable: " +
                    transportsAssigned +
                    " transports, " +
                    cargoAssigned +
                    " cargo (need 1+ transports, 2+ cargo)"
            );
        }
        return viable;
    }

    /**
     * Release units from a completed/aborted operation.
     */
    private void releaseUnits(InvasionPlan operation) {
        for (Unit u : operation.assignedUnits) {
            unitAssignments.remove(u.id);
            memory.setUnitRole(u, StrategyMemory.UnitRole.RESERVE);
        }
    }

    /**
     * Generate phase-specific order for a unit.
     */
    private Order generatePhaseSpecificOrder(
        Unit unit,
        InvasionPlan operation
    ) {
        switch (operation.phase) {
            case PREPARING:
                // Move to rally point
                if (operation.rallyPoint != null) {
                    return unit.newMoveOrder(operation.rallyPoint);
                }
                break;
            case LOADING:
                if (unit.isTransport()) {
                    // Transports wait at rally point for cargo to board
                    return unit.newSentryOrder();
                } else if (unit.isInfantry() || unit.isArmour()) {
                    // Cargo units: if not on a transport yet, move to rally point
                    // Auto-loading will handle boarding when they reach same hex as transport
                    if (!unit.isCarried()) {
                        if (operation.rallyPoint != null) {
                            return unit.newMoveOrder(operation.rallyPoint);
                        }
                        return unit.newSentryOrder();
                    } else {
                        // Already on transport, stay in sentry
                        return unit.newSentryOrder();
                    }
                }
                break;
            case TRANSIT:
                if (unit.isTransport() && operation.unloadPoint != null) {
                    // Transports move to unload point
                    return unit.newMoveOrder(operation.unloadPoint);
                }
                // Cargo on transports and escorts stay in sentry
                return unit.newSentryOrder();
            case LANDING:
                if (unit.isTransport()) {
                    // Transports unload cargo
                    // The transport's own captain will handle unloading via atUnloadPoint() check
                    return unit.newSentryOrder();
                } else if (
                    (unit.isInfantry() || unit.isArmour()) && !unit.isCarried()
                ) {
                    // Cargo that's been unloaded moves toward target city
                    return unit.newMoveOrder(operation.targetCity);
                } else {
                    // Still on transport, wait
                    return unit.newSentryOrder();
                }
            case EXECUTING:
                // All ground units move toward target city
                if (unit.isInfantry() || unit.isArmour()) {
                    return unit.newMoveOrder(operation.targetCity);
                } else if (unit.isTransport()) {
                    // Transports can return to loading point or explore
                    return null; // Let transport captain decide
                }
                break;
        }

        return null; // No specific order
    }

    // Helper methods for phase advancement checks

    private boolean checkAllUnitsAtRallyPoint(InvasionPlan op) {
        if (op.rallyPoint == null) return false;

        // Check if all transports are at rally point
        for (Unit transport : op.transports) {
            if (transport.isDead()) continue;
            if (transport.getLocation().distance(op.rallyPoint) > 2) {
                return false; // Transport not at rally
            }
        }

        // Check if all cargo is either at rally point OR already on a transport
        for (Unit cargo : op.cargo) {
            if (cargo.isDead()) continue;
            // Cargo is ready if it's already on a transport or near rally point
            boolean isReady =
                cargo.isCarried() ||
                cargo.getLocation().distance(op.rallyPoint) <= 2;
            if (!isReady) {
                return false; // Cargo not ready
            }
        }

        // All critical units are ready
        return true;
    }

    private boolean checkTransportsLoaded(InvasionPlan op) {
        for (Unit transport : op.transports) {
            if (transport.isDead()) continue;
            if (!transport.hasCargo()) {
                return false; // At least one transport not loaded
            }
        }
        return true;
    }

    private boolean checkTransportsAtUnloadPoint(InvasionPlan op) {
        if (op.unloadPoint == null) return false;

        for (Unit transport : op.transports) {
            if (transport.isDead()) continue;
            if (transport.getLocation().distance(op.unloadPoint) > 1) {
                return false; // Not at unload point
            }
        }
        return true;
    }

    private boolean checkCargoOnShore(InvasionPlan op) {
        for (Unit cargo : op.cargo) {
            if (cargo.isDead()) continue;
            if (cargo.isCarried()) {
                return false; // Still on transport
            }
        }
        return true;
    }

    private boolean checkTargetCaptured(InvasionPlan op) {
        City target = battleplan.getBoard().getCity(op.targetCity);
        if (target == null) return false;
        return target.getOwner() == player;
    }

    private Location findBestLoadingPoint() {
        // PRIORITY 1: Use production pair rally points (coastal cities)
        java.util.List<ProductionPair> pairs = battleplan.getProductionPairs();
        if (!pairs.isEmpty()) {
            // Return the first available production pair rally point
            ProductionPair pair = pairs.get(0);
            Log.info(
                "Using production pair rally point: " + pair.getRallyPoint()
            );
            return pair.getRallyPoint();
        }

        // FALLBACK: Use legacy loading points
        Set<Location> loadingPoints = battleplan.getLoadingPoints();
        if (loadingPoints.isEmpty()) {
            return null;
        }
        return loadingPoints.iterator().next();
    }

    private Location findBestUnloadPoint(
        Location targetCity,
        Continent targetContinent
    ) {
        // Calculate unload points dynamically based on target continent
        // Get all coastal water hexes of the target continent
        Set<Location> coastalWaters = targetContinent.getCoastalWaters();

        if (coastalWaters.isEmpty()) {
            Log.warn("No coastal waters found for target continent");
            return null;
        }

        // Find the coastal water hex closest to the target city
        Location best = null;
        int bestDist = Integer.MAX_VALUE;

        for (Location waterLoc : coastalWaters) {
            int dist = waterLoc.distance(targetCity);
            if (dist < bestDist) {
                bestDist = dist;
                best = waterLoc;
            }
        }

        if (best != null) {
            Log.info(
                "Found unload point " +
                    best +
                    " for target " +
                    targetCity +
                    " (dist=" +
                    bestDist +
                    ")"
            );
        }

        return best;
    }

    private boolean isInHighThreatZone(Unit u) {
        // Only avoid units under immediate threat (actively engaged)
        ThreatMap threatMap = battleplan.getThreatMap();
        double threat = threatMap.getThreatLevel(u.getLocation());
        return threat > 5.0; // Higher threshold - only avoid active combat
    }
}
