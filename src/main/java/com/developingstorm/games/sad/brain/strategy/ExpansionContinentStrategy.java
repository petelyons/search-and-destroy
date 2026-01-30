package com.developingstorm.games.sad.brain.strategy;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.Battleplan;
import com.developingstorm.games.sad.brain.StrategyMemory;

/**
 * Strategy for continents focused on expansion and conquest.
 * This is the default/current behavior:
 * - Produce military units (infantry, armor, transports)
 * - Focus on capturing cities
 * - Units actively seek targets
 */
public class ExpansionContinentStrategy extends ContinentStrategy {

    public ExpansionContinentStrategy(
        Continent continent,
        Player player,
        Battleplan battleplan
    ) {
        super(continent, player, battleplan);
    }

    @Override
    public String getStrategyName() {
        return "EXPANSION";
    }

    @Override
    public Type getProductionPriority(City city) {
        // Use the existing supply-based production logic from Battleplan
        // This maintains current expansion-focused behavior
        return battleplan.supplyBasedProductionChoice(city);
    }

    @Override
    public StrategyMemory.UnitRole getDefaultUnitRole(Unit unit) {
        // Units on expansion continents are scouts/expanders by default
        if (unit.isInfantry() || unit.isArmour()) {
            return StrategyMemory.UnitRole.SCOUT;
        } else if (unit.isTransport()) {
            return StrategyMemory.UnitRole.INVADER;
        }
        return StrategyMemory.UnitRole.RESERVE;
    }

    @Override
    public boolean shouldExportUnits() {
        return false; // Expansion continents keep their forces for local conquest
    }

    @Override
    public boolean needsReinforcements() {
        int ownedCities = countOwnedCities();
        int totalCities = continent.getCities().size();
        return ownedCities < (totalCities / 2); // Need help if we control less than half
    }

    @Override
    public int getOperationPriority() {
        return 7; // High priority for expansion operations
    }

    private int countOwnedCities() {
        return (int) continent
            .getCities()
            .stream()
            .filter(city -> city.getOwner() == player)
            .count();
    }
}
