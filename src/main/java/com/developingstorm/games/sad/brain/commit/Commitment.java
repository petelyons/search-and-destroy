package com.developingstorm.games.sad.brain.commit;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Unit;

/**
 * A unit's current goal. Progress fields are updated by {@link CommitmentRules}.
 */
public class Commitment {

    public final Unit unit;
    public final CommitmentKind kind;
    /** City, loading point, unloading hex or defended city; null for OPERATION and EXPLORE. */
    public final Location target;
    public final int startTurn;
    int lastProgressTurn;
    int bestDistance = Integer.MAX_VALUE;
    int quietTurns;

    Commitment(Unit unit, CommitmentKind kind, Location target, int startTurn) {
        this.unit = unit;
        this.kind = kind;
        this.target = target;
        this.startTurn = startTurn;
        this.lastProgressTurn = startTurn;
    }

    @Override
    public String toString() {
        return kind + (target != null ? "(" + target + ")" : "");
    }
}
