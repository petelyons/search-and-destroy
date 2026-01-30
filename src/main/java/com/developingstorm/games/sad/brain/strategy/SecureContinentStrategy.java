package com.developingstorm.games.sad.brain.strategy;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.Battleplan;
import com.developingstorm.games.sad.brain.StrategyMemory;

/**
 * Strategy for fully controlled, secure continents with no nearby threats.
 * Focus on:
 * - Minimal defensive garrison
 * - Export-focused production (transports to move troops elsewhere)
 * - Future: Economic/infrastructure development
 */
public class SecureContinentStrategy extends ContinentStrategy {

    // Maintain minimal garrison: 1 infantry per 2 cities
    private static final double GARRISON_RATIO = 0.5;

    public SecureContinentStrategy(
        Continent continent,
        Player player,
        Battleplan battleplan
    ) {
        super(continent, player, battleplan);
    }

    @Override
    public String getStrategyName() {
        return "SECURE";
    }

    @Override
    public Type getProductionPriority(City city) {
        int garrisonCount = countGarrisonUnits();
        int requiredGarrison = (int) Math.ceil(
            continent.getCities().size() * GARRISON_RATIO
        );

        // Build minimal garrison first
        if (garrisonCount < requiredGarrison) {
            return Type.INFANTRY;
        }

        // Then focus on transports for exporting units
        if (city.isCoastal() && countTransports() < 2) {
            return Type.TRANSPORT;
        }

        // Default: build occasional infantry to refresh garrison
        // Future: this could be buildings, economy, etc.
        return Type.INFANTRY;
    }

    @Override
    public StrategyMemory.UnitRole getDefaultUnitRole(Unit unit) {
        // Units on secure continents are either garrison or export candidates
        if (unit.isInfantry() || unit.isArmour()) {
            int garrisonCount = countGarrisonUnits();
            int requiredGarrison = (int) Math.ceil(
                continent.getCities().size() * GARRISON_RATIO
            );

            return garrisonCount > requiredGarrison
                ? StrategyMemory.UnitRole.INVADER // Excess, ready for export
                : StrategyMemory.UnitRole.DEFENDER; // Needed for garrison
        } else if (unit.isTransport()) {
            return StrategyMemory.UnitRole.INVADER; // Transports support exports
        }
        return StrategyMemory.UnitRole.RESERVE;
    }

    @Override
    public boolean shouldExportUnits() {
        int garrisonCount = countGarrisonUnits();
        int requiredGarrison = (int) Math.ceil(
            continent.getCities().size() * GARRISON_RATIO
        );
        return garrisonCount > requiredGarrison;
    }

    @Override
    public boolean needsReinforcements() {
        return false; // Secure continents don't need reinforcements
    }

    @Override
    public int getOperationPriority() {
        return 3; // Low priority - focus on supporting other continents
    }

    private int countGarrisonUnits() {
        return (int) player
            .getUnits()
            .stream()
            .filter(unit -> !unit.isDead())
            .filter(unit -> unit.getContinent() == continent)
            .filter(unit -> unit.isInfantry() || unit.isArmour())
            .count();
    }

    private int countTransports() {
        return (int) player
            .getUnits()
            .stream()
            .filter(unit -> !unit.isDead())
            .filter(unit -> unit.getContinent() == continent)
            .filter(Unit::isTransport)
            .count();
    }
}
