package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
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
                    u.assignOrder(u.newOrder(order, moveTo));
                }
            } else if (this.game.selectedUnit() != null) {
                Log.debug(
                    "UI",
                    "Issuing Order:" +
                        order +
                        " to selected unit:" +
                        this.game.selectedUnit()
                );
                this.game.selectedUnit().assignOrder(
                    this.game.selectedUnit().newOrder(order, moveTo)
                );
            } else {
                throw new SaDException("No unit avaialble for orders");
            }

            Unit active = this.game.selectedUnit();
        });
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
        issueOrders(OrderType.MOVE_EAST);
    }

    public void moveWest() {
        issueOrders(OrderType.MOVE_WEST);
    }

    public void moveNorthEast() {
        issueOrders(OrderType.MOVE_NORTH_EAST);
    }

    public void moveNorthWest() {
        issueOrders(OrderType.MOVE_NORTH_WEST);
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

        if (u.getLocation().equals(loc)) {
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
        // TODO Auto-generated method stub
    }

    public void setAutoSentry(City c) {
        // TODO Auto-generated method stub
    }
}
