package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.RandomUtil;

/**
 * Resolves combat between units and cities.
 * Extracted from Game.java to improve maintainability.
 */
class CombatResolver {

    private final Game game;
    private final UnitManager unitManager;

    CombatResolver(Game game, UnitManager unitManager) {
        this.game = game;
        this.unitManager = unitManager;
    }

    private GameListener getGameListener() {
        return game.getGameListener();
    }

    /**
     * Resolves combat between two units.
     * @return true if attacker wins, false if defender wins
     */
    synchronized boolean resolveUnitAttack(Unit atk, Unit def) {
        Type at = atk.getType();
        // Type dt = def.getType();

        // trade blows until someone dies
        while (true) {
            int attackStrength;
            // attacker hit
            if (RandomUtil.nextBoolean()) {
                attackStrength = at.getAttack();

                getGameListener().hitLocation(def.getLocation());
                if (attackStrength == 0 && def.getAttack() == 0) {
                    attackStrength = 1;
                }

                if (def.life().attack(attackStrength)) {
                    return true;
                }
            }

            // defender hit
            if (RandomUtil.nextBoolean()) {
                attackStrength = def.getAttack();
                getGameListener().hitLocation(atk.getLocation());

                if (attackStrength == 0 && def.getAttack() == 0) {
                    attackStrength = 1;
                }
                if (atk.life().attack(attackStrength)) {
                    return false;
                }
            }
        }
    }

    /**
     * Resolves combat between a unit and a city.
     * @return true if attacker wins, false if attacker dies
     */
    synchronized boolean resolveCityAttack(Unit atk, City def) {
        if (def.getOwner() == null) {
            getGameListener().hitLocation(def.getLocation());
            return RandomUtil.nextBoolean();
        } else {
            getGameListener().hitLocation(def.getLocation());
            for (Unit defu : def.getUnits()) {
                getGameListener().hitLocation(defu.getLocation());
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
