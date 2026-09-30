package com.developingstorm.games.sad.brain.commit;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Unit;

/**
 * What {@link CommitmentRules} needs to know about the world.
 */
public interface CommitmentContext {
    int currentTurn();

    boolean ownedByMe(Location city);

    /** Path length from the unit to the target, 0 if already there, -1 if unreachable. */
    int pathDistance(Unit u, Location target);

    boolean isLoadingPoint(Location l);

    boolean isUnloadingPoint(Location l);

    boolean enemyWithin(Location l, int dist);

    boolean assignedToOperation(Unit u);
}
