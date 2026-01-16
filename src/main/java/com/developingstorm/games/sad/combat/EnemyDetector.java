package com.developingstorm.games.sad.combat;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for enemy detection and combat assessment.
 * Shared between Patrol and Move orders to provide consistent threat evaluation.
 */
public class EnemyDetector {

    /**
     * Detects enemy units within detection range of the given unit.
     *
     * @param unit The unit scanning for enemies
     * @param game Game instance for accessing all units
     * @param detectionRange Distance to scan (hexes)
     * @return Closest enemy unit within range, or null if none found
     */
    public static Unit detectNearbyEnemy(
        Unit unit,
        Game game,
        int detectionRange
    ) {
        Location unitLoc = unit.getLocation();
        List<Unit> visibleEnemies = new ArrayList<>();

        // Get all enemy units within vision range
        List<Unit> allUnits = game.units();
        for (Unit other : allUnits) {
            if (other.getOwner() != unit.getOwner() && !other.isDead()) {
                Location otherLoc = other.getLocation();
                int distance = unitLoc.distance(otherLoc);

                // Check if enemy is within detection range
                if (distance <= detectionRange) {
                    visibleEnemies.add(other);
                }
            }
        }

        if (visibleEnemies.isEmpty()) {
            return null;
        }

        // Return the closest enemy
        Unit closestEnemy = null;
        int minDistance = Integer.MAX_VALUE;
        for (Unit enemy : visibleEnemies) {
            int dist = unitLoc.distance(enemy.getLocation());
            if (dist < minDistance) {
                minDistance = dist;
                closestEnemy = enemy;
            }
        }

        return closestEnemy;
    }

    /**
     * Determines if the unit should engage in combat with the enemy.
     * Compares combat strengths and returns true if the matchup is favorable.
     *
     * @param unit The unit considering combat
     * @param enemy The enemy unit
     * @param game Game instance for board/city checks
     * @param engagementThreshold Minimum strength ratio to attack (e.g., 0.7 = need 70% of enemy strength)
     * @return true if unit should attack, false if should avoid
     */
    public static boolean shouldEngageEnemy(
        Unit unit,
        Unit enemy,
        Game game,
        double engagementThreshold
    ) {
        // Calculate relative combat strength
        double ourStrength = calculateCombatStrength(unit, enemy, game);
        double enemyStrength = calculateCombatStrength(enemy, unit, game);

        // Engage if we have at least the threshold percentage of enemy strength
        double strengthRatio = ourStrength / enemyStrength;

        Log.debug(
            unit,
            "Combat assessment vs " +
                enemy +
                ": our strength=" +
                ourStrength +
                " enemy strength=" +
                enemyStrength +
                " ratio=" +
                strengthRatio +
                " threshold=" +
                engagementThreshold
        );

        return strengthRatio >= engagementThreshold;
    }

    /**
     * Calculates combat strength for a unit against a specific opponent.
     * Factors include: base attack, matchup effectiveness, health, terrain bonuses, and unit type.
     *
     * @param attacker The unit whose strength to calculate
     * @param defender The opponent unit (for matchup effectiveness)
     * @param game Game instance for board/city checks
     * @return Computed strength value (higher = stronger)
     */
    public static double calculateCombatStrength(
        Unit attacker,
        Unit defender,
        Game game
    ) {
        // Base strength from unit type's attack power
        double baseStrength = attacker.getType().getAttack();

        // Apply matchup effectiveness multiplier
        double effectiveness = UnitMatchups.getEffectivenessMultiplier(
            attacker.getType(),
            defender.getType()
        );
        baseStrength *= effectiveness;

        // Factor in current health (hits remaining)
        double healthFactor =
            (double) attacker.life().hits /
            (double) attacker.getType().getHits();

        // Apply health factor - wounded units are weaker
        double strength = baseStrength * healthFactor;

        // Bonus for units in cities (defensive advantage)
        if (game.getBoard().isCity(attacker.getLocation())) {
            strength *= 1.3; // 30% defensive bonus in cities
        }

        // Bonus for air units (generally more powerful)
        if (attacker.getTravel() == Travel.AIR) {
            strength *= 1.2;
        }

        return strength;
    }
}
