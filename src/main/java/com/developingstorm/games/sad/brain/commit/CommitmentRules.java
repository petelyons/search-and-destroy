package com.developingstorm.games.sad.brain.commit;

/**
 * Decides whether a commitment is still worth pursuing.
 */
public class CommitmentRules {

    /** Turns without getting closer before a movement commitment is abandoned. */
    static final int NO_PROGRESS_LIMIT = 3;

    /** Enemy distance that keeps a DEFEND commitment alive. */
    static final int DEFEND_RADIUS = 3;

    /** Consecutive quiet turns before a DEFEND commitment completes. */
    static final int QUIET_TURNS_TO_COMPLETE = 2;

    public CommitmentStatus status(Commitment c, CommitmentContext ctx) {
        switch (c.kind) {
            case OPERATION:
                return ctx.assignedToOperation(c.unit) ? CommitmentStatus.ACTIVE : CommitmentStatus.COMPLETE;
            case DEFEND:
                return defend(c, ctx);
            case CAPTURE_CITY:
                return c.unit.isTransport() ? deliver(c, ctx) : capture(c, ctx);
            case STAGE:
                if (c.unit.isCarried()) {
                    return CommitmentStatus.COMPLETE;
                }
                return ctx.isLoadingPoint(c.target) ? CommitmentStatus.ACTIVE : CommitmentStatus.INVALID;
            case EXPLORE:
            default:
                return CommitmentStatus.ACTIVE;
        }
    }

    private static CommitmentStatus defend(Commitment c, CommitmentContext ctx) {
        if (!ctx.ownedByMe(c.target)) {
            return CommitmentStatus.INVALID;
        }
        if (ctx.enemyWithin(c.target, DEFEND_RADIUS)) {
            c.quietTurns = 0;
            return CommitmentStatus.ACTIVE;
        }
        c.quietTurns++;
        return c.quietTurns >= QUIET_TURNS_TO_COMPLETE ? CommitmentStatus.COMPLETE : CommitmentStatus.ACTIVE;
    }

    private static CommitmentStatus capture(Commitment c, CommitmentContext ctx) {
        if (ctx.ownedByMe(c.target)) {
            return CommitmentStatus.COMPLETE;
        }
        return progress(c, ctx);
    }

    /** A transport's CAPTURE_CITY targets the unloading hex for the invasion. */
    private static CommitmentStatus deliver(Commitment c, CommitmentContext ctx) {
        if (!c.unit.hasCargo()) {
            return CommitmentStatus.COMPLETE;
        }
        if (!ctx.isUnloadingPoint(c.target)) {
            return CommitmentStatus.INVALID;
        }
        return progress(c, ctx);
    }

    private static CommitmentStatus progress(Commitment c, CommitmentContext ctx) {
        int d = ctx.pathDistance(c.unit, c.target);
        if (d < 0) {
            return CommitmentStatus.INVALID;
        }
        int turn = ctx.currentTurn();
        if (d < c.bestDistance) {
            c.bestDistance = d;
            c.lastProgressTurn = turn;
        }
        return turn - c.lastProgressTurn >= NO_PROGRESS_LIMIT ? CommitmentStatus.INVALID : CommitmentStatus.ACTIVE;
    }
}
