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
        // Check for enemy units before moving
        Unit enemyTarget = detectNearbyEnemy();
        if (enemyTarget != null) {
            // Decide whether to attack or avoid based on combat odds
            if (shouldEngageEnemy(enemyTarget)) {
                Log.info(this.unit, "Patrol: engaging enemy " + enemyTarget);
                // Attack the enemy - create a temporary move to intercept
                Location enemyLoc = enemyTarget.getLocation();
                this.currentMove = new Move(this.game, this.unit, enemyLoc);
                OrderResponse response = this.currentMove.execute();

                // After attack attempt, resume patrol
                if (
                    response.getCode() == ResponseCode.ORDER_COMPLETE ||
                    response.getCode() == ResponseCode.ORDER_AND_TURN_COMPLETE
                ) {
                    this.currentMove = null;
                    if (
                        this.unit.life().movesLeft() > 0 && !this.unit.isDead()
                    ) {
                        return executeInternal();
                    } else {
                        return new OrderResponse(
                            ResponseCode.TURN_COMPLETE,
                            this,
                            null
                        );
                    }
                }
                return response;
            } else {
                Log.info(
                    this.unit,
                    "Patrol: avoiding enemy " +
                        enemyTarget +
                        " (unfavorable odds)"
                );
                // Continue patrol, avoiding the enemy
            }
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
