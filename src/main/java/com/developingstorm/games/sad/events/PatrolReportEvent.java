package com.developingstorm.games.sad.events;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Unit;

/**
 * A patrolling lookout stopped to report: it saw an unreported enemy, or its route is blocked.
 */
public class PatrolReportEvent extends AbstractGameEvent {

    public enum Kind {
        SIGHTING,
        BLOCKED,
    }

    private final Kind kind;
    private final Unit patroller;
    private final Unit enemy;
    private final Location location;
    private final int turn;

    public PatrolReportEvent(Kind kind, Unit patroller, Unit enemy, Location location, int turn) {
        super(GameEventType.PATROL_REPORT);
        this.kind = kind;
        this.patroller = patroller;
        this.enemy = enemy;
        this.location = location;
        this.turn = turn;
    }

    public Kind getKind() {
        return kind;
    }

    public Unit getPatroller() {
        return patroller;
    }

    /** The enemy seen, or the enemy blocking the route; null for a route blocked by terrain. */
    public Unit getEnemy() {
        return enemy;
    }

    /** The enemy's hex for a sighting; the unreachable waypoint for a blocked route. */
    public Location getLocation() {
        return location;
    }

    public int getTurn() {
        return turn;
    }
}
