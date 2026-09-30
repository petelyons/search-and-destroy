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

    private final Map<Long, Commitment> byUnit = new HashMap<>();

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

    public void pruneDead() {
        this.byUnit.values().removeIf(c -> c.unit.isDead());
    }

    public int size() {
        return this.byUnit.size();
    }
}
