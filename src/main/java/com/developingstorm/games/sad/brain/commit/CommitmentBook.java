package com.developingstorm.games.sad.brain.commit;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import java.util.HashMap;
import java.util.Map;

/**
 * The current commitment of each unit, keyed by unit id.
 */
public class CommitmentBook {

    /** Turns a unit avoids a target after its commitment to it became invalid. */
    public static final int ABANDON_TURNS = 10;

    private final Map<Long, Commitment> byUnit = new HashMap<>();

    /** Per unit: targets it gave up on, and the turn until which it avoids them. */
    private final Map<Long, Map<Location, Integer>> abandoned = new HashMap<>();
    private final Map<Long, Unit> abandonedBy = new HashMap<>();

    /** Record a new commitment, replacing any existing one for the unit. */
    public Commitment commit(Unit u, CommitmentKind kind, Location target, int turn) {
        Commitment c = new Commitment(u, kind, target, turn);
        Commitment old = this.byUnit.put(u.id, c);
        if (old == null || old.kind != kind || !java.util.Objects.equals(old.target, target)) {
            Log.info(u, "commitment " + c + " started");
        }
        return c;
    }

    public Commitment get(Unit u) {
        return this.byUnit.get(u.id);
    }

    public void release(Unit u, String reason) {
        Commitment c = this.byUnit.remove(u.id);
        if (c != null) {
            Log.info(u, "commitment " + c.kind + " ended: " + reason);
        }
    }

    /** Release the unit's commitment and avoid its target until {@code turn + ABANDON_TURNS}. */
    public void abandon(Unit u, String reason, int turn) {
        Commitment c = this.byUnit.get(u.id);
        if (c != null && c.target != null) {
            this.abandoned.computeIfAbsent(u.id, id -> new HashMap<>()).put(c.target, turn + ABANDON_TURNS);
            this.abandonedBy.put(u.id, u);
        }
        release(u, reason);
    }

    public boolean isAbandoned(Unit u, Location target, int turn) {
        Map<Location, Integer> m = this.abandoned.get(u.id);
        if (m == null) {
            return false;
        }
        Integer until = m.get(target);
        return until != null && turn < until;
    }

    public void pruneDead() {
        this.byUnit.values().removeIf(c -> c.unit.isDead());
        this.abandonedBy.values().removeIf(Unit::isDead);
        this.abandoned.keySet().retainAll(this.abandonedBy.keySet());
    }

    public int size() {
        return this.byUnit.size();
    }
}
