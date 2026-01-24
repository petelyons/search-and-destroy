package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.ui.CityMenuBuilder;
import com.developingstorm.games.sad.ui.OrderMenuBuilder;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPopupMenu;

/**
 *  The GameCommander acts as a bridge between the UI and the game model. It models the actions a can perform on units in the game.
 *
 */
public class GameCommander extends BaseCommander {

    private List<Unit> commandedUnits;

    private GameCommander(
        SaDFrame frame,
        Game game,
        List<Unit> commandedUnits
    ) {
        super(frame, game);
        this.commandedUnits = commandedUnits;
    }

    public GameCommander(SaDFrame frame, Game game) {
        this(frame, game, null);
    }

    /**
     * The main GameCommander issues orders to the games selected unit. If you want to issue orders to a unit
     * but not change the selected unit, you can derive a new commander
     * @param units
     * @return
     */
    public GameCommander commanderForSpecifiedUnits(List<Unit> units) {
        GameCommander commander = new GameCommander(this.frame, this.game);
        commander.commandedUnits = units;
        return commander;
    }

    /**
     * The main GameCommander issues orders to the games selected unit. If you want to issue orders to a unit
     * but not change the selected unit, you can derive a new commander
     * @param units
     * @return
     */
    public GameCommander commanderForSpecificUnit(Unit u) {
        List<Unit> list = new ArrayList<Unit>();
        list.add(u);
        return commanderForSpecifiedUnits(list);
    }

    public boolean isPaused() {
        return this.game.isPaused();
    }

    public void pause() {
        if (!isPaused()) {
            this.game.postGameAction(
                new Runnable() {
                    @Override
                    public void run() {
                        GameCommander.this.game.pause();
                    }
                }
            );
        }
    }

    public void resume() {
        if (isPaused()) {
            this.game.resume(null);
        }
    }

    public void trackLocation(Location loc) {
        if (loc != null) {
            this.game.trackLocation(loc);
        }
    }

    public void move(Location loc) {
        issueOrders(OrderType.MOVE, loc);
        showLine(null, null);
    }

    public void move(Location from, Location loc) {
        Unit unit = this.game.unitAtLocation(from);
        if (unit == null) {
            throw new SaDException(
                "Unit expected at from location of move order"
            );
        }
        GameCommander commander = commanderForSpecificUnit(unit);
        commander.move(loc);
    }

    public void moveBegin() {}

    private void issueOrders(OrderType order) {
        issueOrders(order, null);
    }

    private void issueOrders(OrderType order, Location moveTo) {
        this.game.postAndRunGameAction(() -> {
            if (this.commandedUnits != null) {
                for (Unit u : this.commandedUnits) {
                    Log.debug(
                        "UI",
                        "Issuing Order:" + order + " to special context:" + u
                    );
                    // Enable avoidance for long-distance moves
                    boolean enableAvoidance = shouldEnableAvoidance(
                        u,
                        order,
                        moveTo
                    );
                    u.assignOrder(u.newOrder(order, moveTo, enableAvoidance));
                    // Push to pendingPlay so carried units can execute their orders
                    u.getOwner().pushPendingPlay(u);
                }
            } else if (this.game.selectedUnit() != null) {
                Log.debug(
                    "UI",
                    "Issuing Order:" +
                        order +
                        " to selected unit:" +
                        this.game.selectedUnit()
                );
                Unit selected = this.game.selectedUnit();
                // Enable avoidance for long-distance moves
                boolean enableAvoidance = shouldEnableAvoidance(
                    selected,
                    order,
                    moveTo
                );
                selected.assignOrder(
                    selected.newOrder(order, moveTo, enableAvoidance)
                );
                // Push to pendingPlay so carried units can execute their orders
                selected.getOwner().pushPendingPlay(selected);
            } else {
                throw new SaDException("No unit avaialble for orders");
            }

            Unit active = this.game.selectedUnit();

            // Refresh patrol paths display since orders changed
            this.canvas.showPatrolPaths(this.game.currentPlayer());
            this.canvas.repaint();
        });
    }

    /**
     * Determines if enemy avoidance should be enabled for this move order.
     * Enables avoidance for long-distance moves (>3 hexes).
     */
    private boolean shouldEnableAvoidance(
        Unit unit,
        OrderType order,
        Location moveTo
    ) {
        // Only enable for MOVE orders with a destination
        if (!order.equals(OrderType.MOVE) || moveTo == null) {
            return false;
        }

        // Enable avoidance for long-distance moves
        int distance = unit.getLocation().distance(moveTo);
        return distance > 3;
    }

    public void activate(BoardHex hex) {
        Unit current = this.game.selectedUnit();
        Unit newSel = null;

        if (this.commandedUnits != null) {
            Log.debug("Acting on specified units");
            for (Unit u : this.commandedUnits) {
                u.activate();
                newSel = u;
            }
        } else {
            Location loc = hex.getLocation();

            if (!current.getLocation().equals(loc)) {
                City c = this.game.cityAtLocation(hex.getLocation());

                if (c != null) {
                    // do nothing
                } else {
                    newSel = this.game.unitAtLocation(loc);
                }
            }

            if (newSel != null) {
                newSel.activate();
                //if (active.hasMoved()) {
                //  Log.println("UI", "*** Activated unit has already moved:" + active);
                //
            }
        }

        if (newSel != null) {
            Log.debug("UI", "Activating :" + newSel);
            this.game.selectUnit(newSel);
            this.game.resume(newSel);
        }
    }

    public void center() {
        if (this.game.selectedUnit() != null) {
            this.frame.center(this.game.selectedUnit().getLocation());
        }
    }

    public void moveEast() {
        // Smart east: prefer visually rightward movement
        // Check EAST first (same row), then NE/SE as fallbacks
        moveInVisualDirection(
            OrderType.MOVE_EAST,
            OrderType.MOVE_NORTH_EAST,
            OrderType.MOVE_SOUTH_EAST
        );
    }

    public void moveWest() {
        // Smart west: prefer visually leftward movement
        // Check WEST first (same row), then NW/SW as fallbacks
        moveInVisualDirection(
            OrderType.MOVE_WEST,
            OrderType.MOVE_NORTH_WEST,
            OrderType.MOVE_SOUTH_WEST
        );
    }

    public void moveNorthEast() {
        issueOrders(OrderType.MOVE_NORTH_EAST);
    }

    public void moveNorthWest() {
        issueOrders(OrderType.MOVE_NORTH_WEST);
    }

    /**
     * Try to move in a visual direction, picking the best available hex.
     * Tries primary direction first, then fallback options if blocked.
     */
    private void moveInVisualDirection(
        OrderType primary,
        OrderType fallback1,
        OrderType fallback2
    ) {
        Unit u = this.game.selectedUnit();
        if (u == null) {
            return;
        }

        Location currentLoc = u.getLocation();

        // Try primary direction first
        Location primaryDest = getDestinationForOrder(currentLoc, primary);
        if (
            primaryDest != null &&
            this.game.getBoard().isTravelable(u, primaryDest)
        ) {
            issueOrders(primary);
            return;
        }

        // Try first fallback
        Location fallback1Dest = getDestinationForOrder(currentLoc, fallback1);
        if (
            fallback1Dest != null &&
            this.game.getBoard().isTravelable(u, fallback1Dest)
        ) {
            issueOrders(fallback1);
            return;
        }

        // Try second fallback
        Location fallback2Dest = getDestinationForOrder(currentLoc, fallback2);
        if (
            fallback2Dest != null &&
            this.game.getBoard().isTravelable(u, fallback2Dest)
        ) {
            issueOrders(fallback2);
            return;
        }

        // No valid moves, just issue the primary order anyway (will fail as expected)
        issueOrders(primary);
    }

    /**
     * Gets the destination location for a given directional order type.
     */
    private Location getDestinationForOrder(
        Location from,
        OrderType orderType
    ) {
        Direction dir = null;
        switch (orderType) {
            case MOVE_NORTH_EAST:
                dir = Direction.NORTH_EAST;
                break;
            case MOVE_NORTH_WEST:
                dir = Direction.NORTH_WEST;
                break;
            case MOVE_EAST:
                dir = Direction.EAST;
                break;
            case MOVE_SOUTH_EAST:
                dir = Direction.SOUTH_EAST;
                break;
            case MOVE_SOUTH_WEST:
                dir = Direction.SOUTH_WEST;
                break;
            case MOVE_WEST:
                dir = Direction.WEST;
                break;
            default:
                return null;
        }

        return from.relative(dir);
    }

    public void moveSouthEast() {
        issueOrders(OrderType.MOVE_SOUTH_EAST);
    }

    public void moveSouthWest() {
        issueOrders(OrderType.MOVE_SOUTH_WEST);
    }

    public void explore() {
        issueOrders(OrderType.EXPLORE);
    }

    public void skipTurn() {
        issueOrders(OrderType.SKIPTURN);
    }

    public void sentry() {
        issueOrders(OrderType.SENTRY);
    }

    public void unload() {
        issueOrders(OrderType.UNLOAD);
    }

    public void disband() {
        issueOrders(OrderType.DISBAND);
    }

    public void headHome() {
        issueOrders(OrderType.HEAD_HOME);
    }

    public void escort(Unit unitToEscort) {
        this.game.postAndRunGameAction(() -> {
            if (this.commandedUnits != null) {
                for (Unit u : this.commandedUnits) {
                    Log.debug(
                        "UI",
                        "Issuing Order:ESCORT to special context:" + u
                    );
                    u.orderEscort(unitToEscort);
                    u.getOwner().pushPendingPlay(u);
                }
            } else if (this.game.selectedUnit() != null) {
                Log.debug(
                    "UI",
                    "Issuing Order:ESCORT to selected unit:" +
                        this.game.selectedUnit()
                );
                Unit selected = this.game.selectedUnit();
                selected.orderEscort(unitToEscort);
            }
            this.game.resume(null);
        });
    }

    @Override
    public Location getCurrentLocation() {
        if (this.game.selectedUnit() != null) {
            return this.game.selectedUnit().getLocation();
        } else {
            return null;
        }
    }

    @Override
    public void choose(BoardHex hex) {
        Unit u = this.game.selectedUnit();
        Point p = hex.center();
        JPopupMenu pm = null;
        Location loc = hex.getLocation();

        if (u != null && u.getLocation().equals(loc)) {
            ArrayList<Unit> ulist = new ArrayList<Unit>();
            ulist.add(u);

            GameCommander spc = commanderForSpecifiedUnits(ulist);
            OrderMenuBuilder om = new OrderMenuBuilder(
                this.frame,
                this.game,
                ulist,
                spc
            );
            pm = om.build();
        } else {
            City c = this.game.cityAtLocation(hex.getLocation());

            if (c != null) {
                CityMenuBuilder cmb = new CityMenuBuilder(
                    this.frame,
                    this.game,
                    c,
                    this
                );
                pm = cmb.build();
            } else {
                List<Unit> ul = this.game.unitsAtLocation(loc);
                GameCommander spc = commanderForSpecifiedUnits(ul);
                OrderMenuBuilder omb = new OrderMenuBuilder(
                    this.frame,
                    this.game,
                    ul,
                    spc
                );
                pm = omb.build();
            }
        }

        if (pm != null) pm.show(this.canvas, p.x, p.y);
    }

    @Override
    public boolean isDraggable(BoardHex hex) {
        Unit unit = this.game.unitAtLocation(hex.getLocation());
        return (unit != null);
    }

    public void setSeaPath(City c) {
        PathsCommander pathsCommander = this.frame.startPathsMode();
        pathsCommander.setPathOrigin(c, Travel.SEA);
    }

    public void setAirPath(City c) {
        PathsCommander pathsCommander = this.frame.startPathsMode();
        pathsCommander.setPathOrigin(c, Travel.AIR);
    }

    public void setLandPath(City c) {
        PathsCommander pathsCommander = this.frame.startPathsMode();
        pathsCommander.setPathOrigin(c, Travel.LAND);
    }

    public void setAirPatrol(City c) {
        this.game.postAndRunGameAction(() -> {
            if (c.getGovernor().hasAirPatrol()) {
                c.getGovernor().clearAirPatrol();
            } else {
                c.getGovernor().setAirPatrol();
            }
            // Refresh the display to show/hide patrol lines
            this.frame.getCanvas().repaint();
        });
    }

    public void setAutoSentry(City c) {
        this.game.postAndRunGameAction(() -> {
            if (c.getGovernor().hasAutoSentry()) {
                c.getGovernor().clearAutoSenty();
            } else {
                c.getGovernor().setAutoSentry();
            }
        });
    }
}
