package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Unit;

/**
 * One press-and-release on the map in game mode: a click, a drag, or nothing when this mode
 * never saw the press (another mode handled it, then switched back to game mode).
 */
final class MouseGesture {

    enum Kind { NONE, CLICK, DRAG }

    record Result(Kind kind, Location from) {}

    private Location down;

    void pressed(Location location) {
        this.down = location;
    }

    /** Where the current gesture started, or null if none is in progress. */
    Location origin() {
        return this.down;
    }

    void reset() {
        this.down = null;
    }

    Result released(Location location) {
        Location from = this.down;
        this.down = null;
        if (from == null) {
            return new Result(Kind.NONE, null);
        }
        return new Result(from.equals(location) ? Kind.CLICK : Kind.DRAG, from);
    }

    /**
     * The unit a drag moves: the selected unit if it stands where the drag began. Asked at
     * release, not press, because the game thread may select the next unit after the press.
     */
    static Unit draggedUnit(Result drag, Unit selected) {
        if (drag.kind() != Kind.DRAG || selected == null) {
            return null;
        }
        return selected.getLocation().equals(drag.from()) ? selected : null;
    }
}
