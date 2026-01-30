package com.developingstorm.games.sad.brain.strategy;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.Battleplan;
import com.developingstorm.games.sad.brain.StrategyMemory;

/**
 * Strategy pattern for continent-level decision making.
 * Each continent can have a different strategy based on its situation:
 * - Secure continents focus on economy/export
 * - Expansion continents focus on growth
 * - Contested continents focus on defense
 */
public abstract class ContinentStrategy {

    protected Continent continent;
    protected Player player;
    protected Battleplan battleplan;

    public ContinentStrategy(Continent continent, Player player, Battleplan battleplan) {
        this.continent = continent;
        this.player = player;
        this.battleplan = battleplan;
    }

    /**
     * Get the name of this strategy for logging
     */
    public abstract String getStrategyName();

    /**
     * Determine what this continent should produce at the given city.
     * This is the primary decision point for continent-level strategy.
     */
    public abstract Type getProductionPriority(City city);

    /**
     * Determine the default role for units on this continent.
     * Units will follow this role unless overridden by specific assignments.
     */
    public abstract StrategyMemory.UnitRole getDefaultUnitRole(Unit unit);

    /**
     * Should this continent send units to other continents?
     * Secure continents can export forces to frontlines.
     */
    public abstract boolean shouldExportUnits();

    /**
     * Should this continent receive reinforcements from elsewhere?
     * Contested continents may need help from secure areas.
     */
    public abstract boolean needsReinforcements();

    /**
     * What priority level does this continent have for operations?
     * Higher priority continents get resources allocated first.
     * 0 = no priority, 10 = highest priority
     */
    public abstract int getOperationPriority();

    // Getters
    public Continent getContinent() {
        return continent;
    }

    public Player getPlayer() {
        return player;
    }
}
