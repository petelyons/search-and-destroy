package com.developingstorm.games.sad.brain.commit;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.Battleplan;
import com.developingstorm.games.sad.brain.OperationsCoordinator;

/**
 * {@link CommitmentContext} backed by this turn's battleplan.
 */
public class BattleplanCommitmentContext implements CommitmentContext {

    private final Battleplan plan;
    private final OperationsCoordinator coordinator;

    public BattleplanCommitmentContext(Battleplan plan, OperationsCoordinator coordinator) {
        this.plan = plan;
        this.coordinator = coordinator;
    }

    @Override
    public int currentTurn() {
        return this.plan.getGame().getTurn();
    }

    @Override
    public boolean ownedByMe(Location city) {
        City c = this.plan.getBoard().getCity(city);
        return c != null && c.getOwner() == this.plan.getPlayer();
    }

    @Override
    public int pathDistance(Unit u, Location target) {
        if (u.getLocation().equals(target)) {
            return 0;
        }
        Path p = u.getPath(target);
        return p == null ? -1 : p.length();
    }

    @Override
    public boolean isLoadingPoint(Location l) {
        return this.plan.getLoadingPoints().contains(l);
    }

    @Override
    public boolean enemyWithin(Location l, int dist) {
        return !this.plan.getThreatMap().getNearbyEnemies(l, dist).isEmpty();
    }

    @Override
    public boolean assignedToOperation(Unit u) {
        return this.coordinator != null && this.coordinator.isAssigned(u);
    }
}
