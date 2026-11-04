package com.developingstorm.games.sad.fx;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides which of the human player's units the map draws on each hex.
 */
final class MapUnitPicker {

    private MapUnitPicker() {}

    /** The human player's units that can appear on the map, grouped by hex. */
    static Map<Location, List<Unit>> ownUnitsByLocation(Collection<Unit> all, Player human, Unit selected) {
        Map<Location, List<Unit>> byLoc = new HashMap<>();
        for (Unit unit : all) {
            // Cargo stays hidden inside its transport, except the unit the player is ordering
            if (unit.isCarried() && unit != selected) continue;
            if (human != null && unit.getOwner() != human) continue;
            byLoc.computeIfAbsent(unit.getLocation(), k -> new ArrayList<>()).add(unit);
        }
        return byLoc;
    }

    /** The one unit to draw for a hex: the selection if it's here, else one still able to act. */
    static Unit unitToDraw(List<Unit> unitsAtLoc, Unit selected) {
        if (selected != null && unitsAtLoc.contains(selected)) {
            return selected;
        }
        for (Unit u : unitsAtLoc) {
            if (u.life().hasMoves() && !u.inSentryMode()) {
                return u;
            }
        }
        return unitsAtLoc.get(0);
    }

    /** Units on the hex for the stack badge, counting everything aboard transports and carriers. */
    static int stackCount(List<Unit> unitsAtLoc) {
        java.util.Set<Unit> all = new java.util.HashSet<>(unitsAtLoc);
        for (Unit u : unitsAtLoc) {
            if (u.carries != null) {
                all.addAll(u.carries);
            }
        }
        return all.size();
    }
}
