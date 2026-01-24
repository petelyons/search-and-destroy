package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.types.Fighter;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.CollectionUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FighterCaptain extends UnitCaptain<Fighter> {

    static final Type[] PrimaryTargetTypes = new Type[] {
        Type.TRANSPORT,
        Type.ARMOR,
        Type.INFANTRY,
        Type.BOMBER,
        Type.CARGO,
        Type.FIGHTER,
    };
    static final Set<Type> PrimaryTargets = CollectionUtil.create(
        PrimaryTargetTypes
    );

    static final Type[] SecondaryTargetTypes = new Type[] {
        Type.DESTROYER,
        Type.CRUISER,
        Type.BATTLESHIP,
        Type.CARRIER,
    };
    static final Set<Type> SecondaryTargets = CollectionUtil.create(
        SecondaryTargetTypes
    );

    public FighterCaptain(General gen, Battleplan plan) {
        super(gen, plan);
    }

    @Override
    public Order plan(Fighter u) {
        // PRIORITY 1: Air superiority - engage enemy aircraft first
        Order airSuperiority = engageEnemyAircraft(u);
        if (airSuperiority != null) {
            return airSuperiority;
        }

        // PRIORITY 2: Attack ground and naval targets
        Order order = planAttack(u, PrimaryTargets);
        if (order == null) {
            order = planAttack(u, SecondaryTargets);
        }
        if (order != null) {
            return order;
        }

        // PRIORITY 3: Return to carrier if not carried
        if (!u.isCarried()) {
            order = gotoCarrier(u);
            if (order != null) {
                return order;
            }
        }

        // PRIORITY 4: Explore for new targets
        return explore(u);
    }

    /**
     * Prioritize engaging enemy aircraft to establish air superiority
     */
    private Order engageEnemyAircraft(Fighter u) {
        ThreatMap threatMap = plan.getThreatMap();
        List<Unit> visibleEnemies = threatMap.getVisibleEnemies();

        Unit closestAirEnemy = null;
        int closestDistance = Integer.MAX_VALUE;
        Location fighterLoc = u.getLocation();

        // Find the closest enemy aircraft (fighter or bomber)
        for (Unit enemy : visibleEnemies) {
            if (enemy.getTravel() == Travel.AIR) {
                int distance = fighterLoc.distance(enemy.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestAirEnemy = enemy;
                }
            }
        }

        // Engage if we found an enemy aircraft
        if (closestAirEnemy != null) {
            Log.info(
                u,
                "Engaging enemy aircraft at " +
                    closestAirEnemy.getLocation() +
                    " for air superiority"
            );
            return u.newMoveOrder(closestAirEnemy.getLocation());
        }

        return null;
    }

    private Order gotoCarrier(Unit u) {
        Set<Unit> carriers = new HashSet<Unit>();
        u
            .getOwner()
            .forEachUnit((Unit u2) -> {
                if (u2.isCarrier()) {
                    carriers.add(u2);
                }
            });

        Location loc = u.getLocation();
        for (Unit u2 : carriers) {
            int dist = loc.distance(u2.getLocation());
            if (dist <= u.life().remainingFuel()) {
                return u.newMoveOrder(u2.getLocation());
            }
        }
        return null;
    }
}
