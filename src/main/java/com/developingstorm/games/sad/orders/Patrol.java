package com.developingstorm.games.sad.orders;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.combat.EnemyDetector;
import com.developingstorm.games.sad.events.PatrolReportEvent;
import com.developingstorm.games.sad.PatrolLookout;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrol order for multi-waypoint patrol paths.
 *
 * Air patrols must start and end at a city (closed loop).
 * Sea patrols can be linear (back and forth) or loop.
 */
public class Patrol extends Order {

    /**
     * Patrol mode determines how waypoints are traversed
     */
    public enum PatrolMode {
        /** Loop continuously through waypoints */
        LOOP,
        /** Travel back and forth between first and last waypoint */
        LINEAR,
    }

    private List<Location> waypoints;
    private PatrolMode mode;
    private int currentWaypointIndex;
    private boolean reverseDirection; // For linear mode
    private Move currentMove;
    private Unit pendingSighting;

    /**
     * Creates a new patrol order
     *
     * @param g The game
     * @param u The unit
     * @param waypoints List of waypoint locations (must have at least 2)
     * @param mode Patrol mode (LOOP or LINEAR)
     */
    public Patrol(Game g, Unit u, List<Location> waypoints, PatrolMode mode) {
        super(g, u, OrderType.PATROL);
        if (waypoints == null || waypoints.size() < 2) {
            throw new SaDException("Patrol requires at least 2 waypoints");
        }

        this.waypoints = new ArrayList<>(waypoints);
        this.mode = mode;
        this.currentWaypointIndex = 0;
        this.reverseDirection = false;
        this.currentMove = null;

        validatePatrol();
    }

    /**
     * Validates patrol based on unit type and travel mode
     */
    private void validatePatrol() {
        Travel travel = this.unit.getTravel();

        // Air patrols must start and end at a city
        if (travel == Travel.AIR) {
            Location firstWaypoint = this.waypoints.get(0);
            Location lastWaypoint = this.waypoints.get(
                this.waypoints.size() - 1
            );

            City firstCity = this.game.getBoard().getCity(firstWaypoint);
            City lastCity = this.game.getBoard().getCity(lastWaypoint);

            if (firstCity == null) {
                throw new SaDException("Air patrol must start at a city");
            }

            // For loop mode, first and last waypoint should be the same city
            // For linear mode, both endpoints must be cities
            if (this.mode == PatrolMode.LOOP) {
                if (!firstWaypoint.equals(lastWaypoint)) {
                    throw new SaDException(
                        "Air patrol loop must start and end at the same city"
                    );
                }
            } else {
                if (lastCity == null) {
                    throw new SaDException(
                        "Air patrol endpoints must both be cities"
                    );
                }
            }
        }
    }

    @Override
    protected OrderResponse executeInternal() {
        Log.info(
            this.unit,
            "Patrol executing - waypoint " +
                this.currentWaypointIndex +
                "/" +
                this.waypoints.size() +
                ", moves: " +
                this.unit.life().movesLeft()
        );

        // A human patrol is a lookout: stop and report an unreported enemy in sight
        if (isLookout()) {
            Unit seen = PatrolLookout.unreportedEnemyInSight(this.game, this.unit);
            if (seen != null) {
                return reportSighting(seen);
            }
        }

        // Nothing to do without moves (e.g. resumed after waking on its last step)
        if (!this.unit.life().hasMoves()) {
            return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }

        // A plane that can no longer finish the route goes home to refuel, then carries on
        if (this.unit.getTravel() == Travel.AIR && this.unit.life().mustLand() && !this.unit.hasLanded()) {
            this.currentMove = null;
            OrderResponse home = this.unit.newHeadHomeOrder().execute();
            if (home.getCode() == ResponseCode.DIED) {
                return new OrderResponse(ResponseCode.DIED, this, null);
            }
            return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
        }

        // If we have a current move in progress, continue it
        if (this.currentMove != null) {
            OrderResponse response = this.currentMove.execute();
            ResponseCode code = response.getCode();

            if (
                code == ResponseCode.ORDER_COMPLETE ||
                code == ResponseCode.ORDER_AND_TURN_COMPLETE
            ) {
                // Move completed, advance to next waypoint
                advanceWaypoint();
                this.currentMove = null;

                // If we have moves left, continue patrol
                if (this.unit.life().movesLeft() > 0 && !this.unit.isDead()) {
                    return executeInternal();
                } else {
                    return new OrderResponse(
                        ResponseCode.TURN_COMPLETE,
                        this,
                        null
                    );
                }
            } else if (code == ResponseCode.TURN_COMPLETE) {
                // Move used up all moves but didn't complete
                return new OrderResponse(
                    ResponseCode.TURN_COMPLETE,
                    this,
                    null
                );
            } else if (code == ResponseCode.DIED) {
                return new OrderResponse(ResponseCode.DIED, this, null);
            } else if (code == ResponseCode.YIELD_PASS) {
                // Blocked, try again next turn
                return new OrderResponse(
                    ResponseCode.TURN_COMPLETE,
                    this,
                    null
                );
            } else if (code == ResponseCode.INTERRUPTED && this.pendingSighting != null) {
                return reportSighting(this.pendingSighting);
            } else if (isLookout()) {
                // Enemy in the way, no path, or the step was refused: stop and tell the player
                return reportBlocked();
            } else {
                // Move cancelled or failed
                Log.warn(
                    this.unit,
                    "Patrol move failed with response: " + code
                );
                return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
            }
        }

        // Start a new move to the next waypoint
        Location targetWaypoint = getCurrentWaypoint();

        if (targetWaypoint == null) {
            Log.error(this.unit, "No valid waypoint for patrol");
            return new OrderResponse(ResponseCode.CANCEL_ORDER, this, null);
        }

        // If already at the waypoint, advance to next
        if (this.unit.getLocation().equals(targetWaypoint)) {
            advanceWaypoint();
            targetWaypoint = getCurrentWaypoint();
        }

        Log.debug(
            this.unit,
            "Patrol: moving to waypoint " +
                this.currentWaypointIndex +
                " at " +
                targetWaypoint
        );

        this.currentMove = new Move(this.game, this.unit, targetWaypoint);
        if (isLookout()) {
            this.currentMove.setStopAtEnemies(true);
            this.currentMove.setInterrupt(() -> {
                this.pendingSighting = PatrolLookout.unreportedEnemyInSight(this.game, this.unit);
                return this.pendingSighting != null;
            });
        }
        return executeInternal();
    }

    /**
     * Gets the current waypoint to move towards
     */
    private Location getCurrentWaypoint() {
        if (
            this.currentWaypointIndex < 0 ||
            this.currentWaypointIndex >= this.waypoints.size()
        ) {
            return null;
        }
        return this.waypoints.get(this.currentWaypointIndex);
    }

    /**
     * Advances to the next waypoint based on patrol mode
     */
    private void advanceWaypoint() {
        if (this.mode == PatrolMode.LOOP) {
            this.currentWaypointIndex =
                (this.currentWaypointIndex + 1) % this.waypoints.size();
        } else {
            // LINEAR mode
            if (this.reverseDirection) {
                this.currentWaypointIndex--;
                if (this.currentWaypointIndex <= 0) {
                    this.currentWaypointIndex = 0;
                    this.reverseDirection = false;
                }
            } else {
                this.currentWaypointIndex++;
                if (this.currentWaypointIndex >= this.waypoints.size() - 1) {
                    this.currentWaypointIndex = this.waypoints.size() - 1;
                    this.reverseDirection = true;
                }
            }
        }
    }

    /**
     * Gets the list of waypoints
     */
    /** Restore progress along the route (loading a save). */
    public void restoreState(int waypointIndex, boolean reverse) {
        this.currentWaypointIndex = waypointIndex;
        this.reverseDirection = reverse;
    }

    /** Human patrols act as lookouts; robot patrols behave as before. */
    private boolean isLookout() {
        return !this.unit.getOwner().isRobot();
    }

    private OrderResponse reportSighting(Unit enemy) {
        // One report per hex: whatever shares it (cargo, a plane over a ship) is reported with it
        for (Unit u : this.game.unitsAtLocation(enemy.getLocation())) {
            if (u.getOwner() != this.unit.getOwner()) {
                this.unit.getOwner().markReported(u);
            }
        }
        this.unit.getOwner().markReported(enemy);
        suspend();
        this.game.getEventBus().publish(new PatrolReportEvent(
            PatrolReportEvent.Kind.SIGHTING, this.unit, enemy, enemy.getLocation(), this.game.getTurn()));
        return woken();
    }

    /** The patrol is suspended; the unit waits for the player, this turn if it has moves left. */
    private OrderResponse woken() {
        ResponseCode code = this.unit.life().hasMoves() ? ResponseCode.ORDER_COMPLETE : ResponseCode.ORDER_AND_TURN_COMPLETE;
        return new OrderResponse(code, this, null);
    }

    private OrderResponse reportBlocked() {
        Location waypoint = getCurrentWaypoint();
        Unit blocker = null;
        Location at = this.currentMove != null ? this.currentMove.getBlockedAt() : null;
        if (at != null) {
            for (Unit u : this.game.unitsAtLocation(at)) {
                if (u.getOwner() != this.unit.getOwner()) {
                    blocker = u;
                    break;
                }
            }
        }
        suspend();
        this.game.getEventBus().publish(new PatrolReportEvent(
            PatrolReportEvent.Kind.BLOCKED, this.unit, blocker, waypoint, this.game.getTurn()));
        return woken();
    }

    /** Park this patrol on the unit so the player can resume it; the next move starts afresh. */
    private void suspend() {
        this.currentMove = null;
        this.pendingSighting = null;
        this.unit.suspendPatrol(this);
    }

    public List<Location> getWaypoints() {
        return new ArrayList<>(this.waypoints);
    }

    /**
     * Gets the patrol mode
     */
    public PatrolMode getMode() {
        return this.mode;
    }

    /**
     * Gets the current waypoint index
     */
    public int getCurrentWaypointIndex() {
        return this.currentWaypointIndex;
    }

    /**
     * Gets whether the patrol is in reverse direction (for linear mode)
     */
    public boolean isReverseDirection() {
        return this.reverseDirection;
    }

    /**
     * Detects nearby enemy units within the unit's vision range.
     * Returns the closest enemy that is reachable.
     */
    private Unit detectNearbyEnemy() {
        int detectionRange = this.unit.getType().getDist() * 2;
        return EnemyDetector.detectNearbyEnemy(
            this.unit,
            this.game,
            detectionRange
        );
    }

    /**
     * Decides whether to engage an enemy based on combat strength comparison.
     * Returns true if we should attack, false if we should avoid.
     */
    private boolean shouldEngageEnemy(Unit enemy) {
        return EnemyDetector.shouldEngageEnemy(
            this.unit,
            enemy,
            this.game,
            0.7
        );
    }

    @Override
    public String toString() {
        return (
            "Patrol (" +
            this.mode +
            ", " +
            this.waypoints.size() +
            " waypoints, current: " +
            this.currentWaypointIndex +
            ")"
        );
    }
}
