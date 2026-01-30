package com.developingstorm.games.sad.brain.strategy;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.brain.Battleplan;
import com.developingstorm.games.sad.util.Log;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classifies continents into strategic categories based on:
 * - Control level (how many cities we own)
 * - Threat level (nearby enemy presence)
 * - Expansion potential (uncaptured cities available)
 */
public class ContinentClassifier {

    private Battleplan battleplan;
    private Player player;

    public ContinentClassifier(Battleplan battleplan, Player player) {
        this.battleplan = battleplan;
        this.player = player;
    }

    /**
     * Classify a continent and return the appropriate strategy.
     */
    public ContinentStrategy classifyContinent(Continent continent) {
        if (continent == null || continent.getCities().isEmpty()) {
            return new ExpansionContinentStrategy(
                continent,
                player,
                battleplan
            );
        }

        int ownedCities = countOwnedCities(continent);
        int totalCities = continent.getCities().size();
        boolean hasEnemyNeighbor = hasAdjacentEnemyPresence(continent);

        ContinentStrategy strategy;

        if (ownedCities == totalCities && !hasEnemyNeighbor) {
            // Fully controlled, no threats = SECURE
            strategy = new SecureContinentStrategy(
                continent,
                player,
                battleplan
            );
            Log.debug(
                String.format(
                    "Continent %s classified as SECURE (%d/%d cities, no threats)",
                    continent,
                    ownedCities,
                    totalCities
                )
            );
        } else {
            // Any other case = EXPANSION (includes contested, frontier, etc.)
            strategy = new ExpansionContinentStrategy(
                continent,
                player,
                battleplan
            );
            Log.debug(
                String.format(
                    "Continent %s classified as EXPANSION (%d/%d cities, enemy neighbor: %s)",
                    continent,
                    ownedCities,
                    totalCities,
                    hasEnemyNeighbor
                )
            );
        }

        return strategy;
    }

    private int countOwnedCities(Continent continent) {
        return (int) continent
            .getCities()
            .stream()
            .filter(city -> city.getOwner() == player)
            .count();
    }

    private boolean hasAdjacentEnemyPresence(Continent continent) {
        return getAdjacentContinents(continent)
            .stream()
            .anyMatch(this::hasEnemyPresence);
    }

    /**
     * Find all continents that are adjacent to this one (reachable by sea).
     */
    private Set<Continent> getAdjacentContinents(Continent continent) {
        // Start with target continents from battleplan
        Set<Continent> adjacent = battleplan
            .getTargetContinents()
            .stream()
            .filter(target -> target != continent)
            .collect(Collectors.toSet());

        // Add other continents if we have coastal cities
        boolean hasCoastalCity = continent
            .getCities()
            .stream()
            .anyMatch(city -> city.isCoastal() && city.getOwner() == player);

        if (hasCoastalCity) {
            battleplan
                .getBoard()
                .getContinents()
                .stream()
                .filter(
                    other -> other != continent && !adjacent.contains(other)
                )
                .forEach(adjacent::add);
        }

        return adjacent;
    }

    /**
     * Check if a continent has enemy presence (cities or units).
     */
    private boolean hasEnemyPresence(Continent continent) {
        // Check for enemy cities
        boolean hasEnemyCities = continent
            .getCities()
            .stream()
            .map(City::getOwner)
            .anyMatch(owner -> owner != null && owner != player);

        if (hasEnemyCities) {
            return true;
        }

        // Check for enemy units (visible ones)
        return java.util.Arrays.stream(battleplan.getGame().getPlayers())
            .filter(enemy -> enemy != player)
            .flatMap(enemy -> enemy.getUnits().stream())
            .filter(unit -> !unit.isDead())
            .anyMatch(unit -> unit.getContinent() == continent);
    }
}
