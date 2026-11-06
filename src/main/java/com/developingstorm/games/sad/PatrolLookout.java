package com.developingstorm.games.sad;

/**
 * What a patrolling unit can see that its owner hasn't been told about yet.
 */
public final class PatrolLookout {

    private PatrolLookout() {}

    /**
     * The nearest enemy within the patroller's vision distance that its vision type can see
     * (submarines need WATER or COMPLETE vision) and its owner hasn't reported; null if none.
     */
    public static Unit unreportedEnemyInSight(Game game, Unit patroller) {
        Player owner = patroller.getOwner();
        Type type = patroller.getType();
        int range = type.getVisionDistance();
        Unit nearest = null;
        int nearestDist = Integer.MAX_VALUE;
        for (Unit u : game.units()) {
            if (u.getOwner() == owner || u.isDead() || owner.isReported(u)) {
                continue;
            }
            int d = patroller.getLocation().distance(u.getLocation());
            if (d <= range && d < nearestDist && u.isVisible(type.getVision())) {
                nearest = u;
                nearestDist = d;
            }
        }
        return nearest;
    }
}
