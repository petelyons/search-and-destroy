package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Unit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintains persistent strategic goals and state across turns.
 * Prevents the AI from flip-flopping between objectives and
 * enables multi-turn strategic planning.
 */
public class StrategyMemory {

    // Invasion targets - cities we're actively trying to capture
    private Map<Location, InvasionPlan> activeInvasions;

    // Defense assignments - continents we're defending
    private Map<Continent, DefensePlan> defensePlans;

    // Unit roles - track what each unit is doing
    private Map<Long, UnitRole> unitRoles;

    // Strategic priorities - what we're focusing on this game
    private StrategyFocus currentFocus;

    // Turn counter for aging old plans
    private int turnCounter;

    public enum StrategyFocus {
        EXPANSION, // Early game - capture neutral cities
        CONSOLIDATION, // Mid game - defend what we have
        ASSAULT, // Late game - attack enemy
        SURVIVAL // Emergency - we're losing
    }

    public enum UnitRole {
        SCOUT, // Exploring unknown territory
        DEFENDER, // Protecting owned territory
        INVADER, // Part of invasion force
        ESCORT, // Protecting transport/carrier
        RESERVE // Waiting for assignment
    }

    public static class InvasionPlan {

        public Location targetCity;
        public Set<Unit> assignedUnits;
        public int turnCreated;
        public boolean readyToLaunch;

        public InvasionPlan(Location target, int turn) {
            this.targetCity = target;
            this.assignedUnits = new HashSet<>();
            this.turnCreated = turn;
            this.readyToLaunch = false;
        }

        public void addUnit(Unit u) {
            this.assignedUnits.add(u);
            checkReadiness();
        }

        public void removeUnit(Unit u) {
            this.assignedUnits.remove(u);
            checkReadiness();
        }

        private void checkReadiness() {
            // Need at least 1 transport and 2 ground units
            int transports = 0;
            int groundUnits = 0;

            for (Unit u : this.assignedUnits) {
                if (!u.isDead()) {
                    if (u.isTransport()) {
                        transports++;
                    } else if (u.isInfantry() || u.isArmour()) {
                        groundUnits++;
                    }
                }
            }

            this.readyToLaunch = (transports >= 1 && groundUnits >= 2);
        }
    }

    public static class DefensePlan {

        public Continent continent;
        public Set<Unit> defenders;
        public List<Location> defensivePositions;
        public int turnCreated;

        public DefensePlan(Continent cont, int turn) {
            this.continent = cont;
            this.defenders = new HashSet<>();
            this.defensivePositions = new ArrayList<>();
            this.turnCreated = turn;
        }

        public void addDefender(Unit u) {
            this.defenders.add(u);
        }

        public void removeDefender(Unit u) {
            this.defenders.remove(u);
        }
    }

    public StrategyMemory() {
        this.activeInvasions = new HashMap<>();
        this.defensePlans = new HashMap<>();
        this.unitRoles = new HashMap<>();
        this.currentFocus = StrategyFocus.EXPANSION;
        this.turnCounter = 0;
    }

    /**
     * Call at start of each turn to age old plans
     */
    public void startNewTurn() {
        this.turnCounter++;

        // Remove stale invasion plans (older than 10 turns)
        List<Location> staleInvasions = new ArrayList<>();
        for (Map.Entry<Location, InvasionPlan> entry : this.activeInvasions.entrySet()) {
            if (this.turnCounter - entry.getValue().turnCreated > 10) {
                staleInvasions.add(entry.getKey());
            }
        }
        for (Location loc : staleInvasions) {
            this.activeInvasions.remove(loc);
        }

        // Clean up dead units from plans
        cleanupDeadUnits();
    }

    /**
     * Remove dead units from all plans
     */
    private void cleanupDeadUnits() {
        // Clean invasion plans
        for (InvasionPlan plan : this.activeInvasions.values()) {
            Set<Unit> dead = new HashSet<>();
            for (Unit u : plan.assignedUnits) {
                if (u.isDead()) {
                    dead.add(u);
                }
            }
            for (Unit u : dead) {
                plan.removeUnit(u);
            }
        }

        // Clean defense plans
        for (DefensePlan plan : this.defensePlans.values()) {
            Set<Unit> dead = new HashSet<>();
            for (Unit u : plan.defenders) {
                if (u.isDead()) {
                    dead.add(u);
                }
            }
            for (Unit u : dead) {
                plan.removeDefender(u);
            }
        }

        // Clean unit roles
        List<Long> deadIds = new ArrayList<>();
        for (Map.Entry<Long, UnitRole> entry : this.unitRoles.entrySet()) {
            // We can't easily check if unit is dead without reference, so just clear old entries
        }
    }

    /**
     * Create or get an invasion plan for a target city
     */
    public InvasionPlan getOrCreateInvasionPlan(Location targetCity) {
        InvasionPlan plan = this.activeInvasions.get(targetCity);
        if (plan == null) {
            plan = new InvasionPlan(targetCity, this.turnCounter);
            this.activeInvasions.put(targetCity, plan);
        }
        return plan;
    }

    /**
     * Get all active invasion plans
     */
    public List<InvasionPlan> getActiveInvasions() {
        return new ArrayList<>(this.activeInvasions.values());
    }

    /**
     * Check if we're already planning to invade a location
     */
    public boolean hasInvasionPlan(Location loc) {
        return this.activeInvasions.containsKey(loc);
    }

    /**
     * Create or get a defense plan for a continent
     */
    public DefensePlan getOrCreateDefensePlan(Continent continent) {
        DefensePlan plan = this.defensePlans.get(continent);
        if (plan == null) {
            plan = new DefensePlan(continent, this.turnCounter);
            this.defensePlans.put(continent, plan);
        }
        return plan;
    }

    /**
     * Get all defense plans
     */
    public List<DefensePlan> getDefensePlans() {
        return new ArrayList<>(this.defensePlans.values());
    }

    /**
     * Assign a role to a unit
     */
    public void setUnitRole(Unit u, UnitRole role) {
        this.unitRoles.put(u.id, role);
    }

    /**
     * Get the assigned role for a unit
     */
    public UnitRole getUnitRole(Unit u) {
        return this.unitRoles.getOrDefault(u.id, UnitRole.RESERVE);
    }

    /**
     * Check if a unit has a specific role
     */
    public boolean hasRole(Unit u, UnitRole role) {
        return getUnitRole(u) == role;
    }

    /**
     * Set the current strategic focus
     */
    public void setStrategyFocus(StrategyFocus focus) {
        this.currentFocus = focus;
    }

    /**
     * Get the current strategic focus
     */
    public StrategyFocus getStrategyFocus() {
        return this.currentFocus;
    }

    /**
     * Check if we're in expansion mode
     */
    public boolean isExpanding() {
        return this.currentFocus == StrategyFocus.EXPANSION;
    }

    /**
     * Check if we're in defensive mode
     */
    public boolean isDefending() {
        return this.currentFocus == StrategyFocus.CONSOLIDATION ||
            this.currentFocus == StrategyFocus.SURVIVAL;
    }

    /**
     * Check if we're in assault mode
     */
    public boolean isAssaulting() {
        return this.currentFocus == StrategyFocus.ASSAULT;
    }
}
