package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.combat.UnitMatchups;
import com.developingstorm.games.sad.events.CombatResolvedEvent;
import com.developingstorm.games.sad.events.LocationHitEvent;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.RandomUtil;

/**
 * Resolves combat between units and cities.
 * Extracted from Game.java to improve maintainability.
 */
public class CombatResolver {

    private final Game game;
    private final UnitManager unitManager;

    CombatResolver(Game game, UnitManager unitManager) {
        this.game = game;
        this.unitManager = unitManager;
    }

    /**
     * Resolves combat between two units.
     * @return true if attacker wins, false if defender wins
     */
    public synchronized boolean resolveUnitAttack(Unit atk, Unit def) {
        Type at = atk.getType();
        // Type dt = def.getType();

        // Capture initial state for combat result
        int attackerInitialHits = atk.life().hits;
        int defenderInitialHits = def.life().hits;

        // trade blows until someone dies
        boolean attackerWon = false;
        while (true) {
            int attackStrength;
            // attacker hit
            if (RandomUtil.nextBoolean()) {
                // Apply effectiveness multiplier based on unit matchup
                int baseAttack = at.getAttack();
                double multiplier = UnitMatchups.getEffectivenessMultiplier(
                    at,
                    def.getType()
                );
                attackStrength = (int) Math.ceil(baseAttack * multiplier);

                game
                    .getEventBus()
                    .publish(new LocationHitEvent(def.getLocation()));

                if (attackStrength == 0 && def.getAttack() == 0) {
                    attackStrength = 1;
                }

                Log.debug(
                    atk,
                    "Attacks " +
                        def +
                        " with " +
                        attackStrength +
                        " damage (" +
                        UnitMatchups.getMatchupDescription(at, def.getType()) +
                        " " +
                        multiplier +
                        "x)"
                );

                if (def.life().attack(attackStrength)) {
                    attackerWon = true;
                    break;
                }
            }

            // defender hit
            if (RandomUtil.nextBoolean()) {
                // Apply effectiveness multiplier for defender's counterattack
                int baseAttack = def.getAttack();
                double multiplier = UnitMatchups.getEffectivenessMultiplier(
                    def.getType(),
                    at
                );
                attackStrength = (int) Math.ceil(baseAttack * multiplier);

                game
                    .getEventBus()
                    .publish(new LocationHitEvent(atk.getLocation()));

                if (attackStrength == 0 && def.getAttack() == 0) {
                    attackStrength = 1;
                }

                Log.debug(
                    def,
                    "Counterattacks " +
                        atk +
                        " with " +
                        attackStrength +
                        " damage (" +
                        UnitMatchups.getMatchupDescription(def.getType(), at) +
                        " " +
                        multiplier +
                        "x)"
                );

                if (atk.life().attack(attackStrength)) {
                    attackerWon = false;
                    break;
                }
            }
        }

        // Create and notify combat result
        CombatResult result = new CombatResult(
            atk,
            attackerInitialHits,
            def,
            defenderInitialHits,
            attackerWon
        );

        // Publish event with result for battle history
        game
            .getEventBus()
            .publish(new CombatResolvedEvent(def.getLocation(), result));

        return attackerWon;
    }

    /**
     * Resolves a one-way bombardment attack (no return fire).
     * Used for naval bombardment of land targets.
     * @param attacker The bombarding unit (ship)
     * @param defender The target unit (land unit)
     * @return true if defender is destroyed, false if defender survives
     */
    public synchronized boolean resolveBombardment(
        Unit attacker,
        Unit defender
    ) {
        Type at = attacker.getType();

        // Capture initial state for combat result
        int attackerInitialHits = attacker.life().hits;
        int defenderInitialHits = defender.life().hits;

        // Bombardment is one-way - only the attacker fires
        boolean targetDestroyed = false;

        // Apply effectiveness multiplier based on unit matchup
        int baseAttack = at.getAttack();
        double multiplier = UnitMatchups.getEffectivenessMultiplier(
            at,
            defender.getType()
        );
        int attackStrength = (int) Math.ceil(baseAttack * multiplier);

        game
            .getEventBus()
            .publish(new LocationHitEvent(defender.getLocation()));

        Log.info(
            attacker,
            "Bombards " +
                defender +
                " with " +
                attackStrength +
                " damage (" +
                UnitMatchups.getMatchupDescription(at, defender.getType()) +
                " " +
                multiplier +
                "x)"
        );

        // Each bombardment fires multiple times (50% hit chance per shot)
        // Continue until defender is destroyed or attacker runs out of shots
        int maxShots = attackStrength * 2; // Average of attackStrength hits
        for (int i = 0; i < maxShots; i++) {
            if (RandomUtil.nextBoolean()) {
                if (defender.life().attack(1)) {
                    targetDestroyed = true;
                    break;
                }
            }
        }

        // Create and notify combat result (attacker never takes damage in bombardment)
        CombatResult result = new CombatResult(
            attacker,
            attackerInitialHits, // Attacker health unchanged
            defender,
            defenderInitialHits,
            targetDestroyed
        );

        // Publish event with result for battle history
        game
            .getEventBus()
            .publish(new CombatResolvedEvent(defender.getLocation(), result));

        return targetDestroyed;
    }

    /**
     * Resolves combat between a unit and a city.
     * @return true if attacker wins, false if attacker dies
     */
    synchronized boolean resolveCityAttack(Unit atk, City def) {
        if (def.getOwner() == null) {
            game.getEventBus().publish(new LocationHitEvent(def.getLocation()));
            return RandomUtil.nextBoolean();
        } else {
            game.getEventBus().publish(new LocationHitEvent(def.getLocation()));
            for (Unit defu : def.getUnits()) {
                game
                    .getEventBus()
                    .publish(new LocationHitEvent(defu.getLocation()));
                if (defu.getTravel() == Travel.LAND) {
                    if (resolveUnitAttack(atk, defu) == false) {
                        return false;
                    }
                } else {
                    int k = RandomUtil.getInt(100);
                    if (k >= 75) {
                        unitManager.killUnit(defu, false);
                    }
                }
            }
            return RandomUtil.nextBoolean();
        }
    }
}
