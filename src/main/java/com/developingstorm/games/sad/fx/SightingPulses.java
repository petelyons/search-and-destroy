package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Unit;
import java.util.HashSet;
import java.util.Set;

/**
 * Lookouts that spotted something and haven't had focus since, so the map can pulse their vision
 * when the player first gets to them. FX thread only.
 */
public class SightingPulses {

    private final Set<Unit> pending = new HashSet<>();

    public void sighted(Unit lookout) {
        if (lookout != null) {
            pending.add(lookout);
        }
    }

    /** True the first time a lookout gets focus after a sighting, while it still waits on the player. */
    public boolean takeOnFocus(Unit unit) {
        if (unit == null || !pending.remove(unit)) {
            return false;
        }
        return !unit.isDead() && unit.getSuspendedPatrol() != null;
    }
}
