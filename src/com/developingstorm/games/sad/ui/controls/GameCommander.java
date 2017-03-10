package com.developingstorm.games.sad.ui.controls;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.CityMenuBuilder;
import com.developingstorm.games.sad.ui.OrderMenuBuilder;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.util.Log;

/**
 *  The GameCommander acts as a bridge between the UI and the game model. It models the actions a can perform on units in the game. 
 * 
 */
public class GameCommander extends BaseCommander {
 
  private List<Unit> _commandedUnits;
  
  
  
  private GameCommander(SaDFrame frame, BoardCanvas canvas, Game game, List<Unit> commandedUnits) {
    super(frame, canvas, game);
    _frame = frame;
    _canvas = canvas;
    _game = game;
    _commandedUnits = commandedUnits;
  }
  
  public GameCommander(SaDFrame frame, BoardCanvas canvas, Game game) {
    this(frame, canvas, game, null);
  }
  
  /**
   * The main GameCommander issues orders to the games selected unit. If you want to issue orders to a unit
   * but not change the selected unit, you can derive a new commander
   * @param units
   * @return
   */
  public GameCommander commanderForSpecifiedUnits(List<Unit> units) {
    GameCommander commander = new GameCommander(_frame, _canvas, _game);
    commander._commandedUnits = units;
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

  public boolean isWaiting() {
    return _game.isWaiting();
  }

  public void move(Location loc) {
    issueOrders(OrderType.MOVE, loc);
    showLine(null, null);
  }

  public void move(Location from, Location loc) {
    Unit unit = _game.unitAtLocation(from);
    if (unit == null) {
      throw new SaDException("Unit expected at from location of move order");
    }
    GameCommander commander = commanderForSpecificUnit(unit);
    commander.move(loc);
  }

  public void moveBegin() {

  }

  private void issueOrders(OrderType order) {
    issueOrders(order, null);
  }

  private void issueOrders(OrderType order, Location moveTo) {
    

    if (_commandedUnits != null) {
      for(Unit u : _commandedUnits) {
        Log.debug("UI", "Issuing Order:" + order + " to special context:" + u);
        _game.issueOrders(u, order, moveTo, null);
      }
    } else if (_game.selectedUnit() != null){
      Log.debug("UI", "Issuing Order:" + order + " to selected unit:" + _game.selectedUnit());
      _game.issueOrders(_game.selectedUnit(), order, moveTo, null);
    }
    else {
      throw new SaDException("No unit avaialble for orders");
    }

    Unit active = _game.selectedUnit();
    if (active != null && active.hasOrders()) {
      _game.endWait(active);
    }
  }

  public void activate(BoardHex hex) {
    Unit active = _game.selectedUnit();
    Unit last = null;
    if (_commandedUnits != null) {
      for (Unit u : _commandedUnits) {
        u.clearOrders();
        last = u;
      }
    } else {
      Location loc = hex.getLocation();
      
      if (!active.getLocation().equals(loc)) {
        City c = _game.cityAtLocation(hex.getLocation());

        if (c != null) {
          // do nothing
        } else {
          active = _game.unitAtLocation(loc);
        }
      }

      if (active != null) {
        active.clearOrders();
        //if (active.hasMoved()) {
        //  Log.println("UI", "*** Activated unit has already moved:" + active);
        //}
        last = active;
      }
    }

    if (last != null) {
      Log.debug("UI", "Activating :" + last);
      _game.endWait(last);
    }
  }

  public void center() {
    if (_game.selectedUnit() != null) {
      _frame.center(_game.selectedUnit().getLocation());
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
    if (_game.selectedUnit() != null) {
      return _game.selectedUnit().getLocation();
    } else {
      return null;
    }
  }
 
  @Override
  public void choose(BoardHex hex) {
    Unit u = _game.selectedUnit();
    Point p = hex.center();
    JPopupMenu pm = null;
    Location loc = hex.getLocation();
    
    if (u.getLocation().equals(loc)) {
      ArrayList<Unit> ulist = new ArrayList<Unit>();
      ulist.add(u);

      GameCommander spc = commanderForSpecifiedUnits(ulist);
      OrderMenuBuilder om = new OrderMenuBuilder(_frame, _game, ulist, spc);
      pm = om.build();
    } else {
      City c = _game.cityAtLocation(hex.getLocation());

      if (c != null) {
        CityMenuBuilder cmb = new CityMenuBuilder(_frame, _game, c, this);
        pm = cmb.build();
      } else {
        List<Unit> ul = _game.unitsAtLocation(loc);
        GameCommander spc = commanderForSpecifiedUnits(ul);
        OrderMenuBuilder omb = new OrderMenuBuilder(_frame, _game, ul, spc);
        pm = omb.build();
      }
    }

    if (pm != null)
      pm.show(_canvas, p.x, p.y);
  }

  @Override
  public boolean isDraggable(BoardHex hex) {
    Unit unit = _game.unitAtLocation(hex.getLocation());
    return (unit != null);
  }

  public void setSeaPath(City c) {
    PathsCommander pathsCommander = _frame.startPathsMode();
    pathsCommander.setPathOrigin(c, Travel.SEA);
    
  }

  public void setAirPath(City c) {
    PathsCommander pathsCommander = _frame.startPathsMode();
    pathsCommander.setPathOrigin(c, Travel.AIR);
  }

  public void setLandPath(City c) {
    PathsCommander pathsCommander = _frame.startPathsMode();
    pathsCommander.setPathOrigin(c, Travel.LAND);
  }

  public void setAirPatrol(City c) {
    // TODO Auto-generated method stub
    
  }

  public void setAutoSentry(City c) {
    // TODO Auto-generated method stub
    
  }

}
