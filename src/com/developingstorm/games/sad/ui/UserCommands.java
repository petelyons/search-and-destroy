package com.developingstorm.games.sad.ui;

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
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

/**

 * 
 */
public class UserCommands {
  private SaDFrame _frame;
  private boolean _paused;
  private BoardCanvas _canvas;
  private Game _game;
  private List<Unit> _spcCtx;

  public UserCommands(SaDFrame frame, BoardCanvas canvas, Game game) {
    _frame = frame;
    _paused = false;
    _canvas = canvas;
    _game = game;
    _spcCtx = null;
  }

  public UserCommands specialContext(List<Unit> units) {
    UserCommands uc = new UserCommands(_frame, _canvas, _game);
    uc._paused = _paused;
    uc._spcCtx = units;
    return uc;
  }

  public UserCommands specialContext(Unit u) {
    List<Unit> list = new ArrayList<Unit>();
    list.add(u);
    return specialContext(list);
  }


  public boolean onBoard(Location loc) {
    return _game.getBoard().onBoard(loc);
  }

  public void showLine(Location start, Location end) {
    _canvas.setLine(start, end);
  }

  public void showLocation(Location loc) {
    _frame.showLocation(loc);
  }

  public boolean isWaiting() {
    return _game.isWaiting();
  }

  public BoardHex trans(Point p) {
    return _game.getBoard().get(p);
  }

  public Location getCurrentLocation() {
    if (_game.selectedUnit() != null) {
      return _game.selectedUnit().getLocation();
    } else {
      return null;
    }
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
    UserCommands uc = specialContext(unit);
    uc.move(loc);
  }

  public void moveBegin() {

  }

  private void issueOrders(OrderType order) {
    issueOrders(order, null);
  }

  private void issueOrders(OrderType order, Location moveTo) {
    

    if (_spcCtx != null) {
      for(Unit u : _spcCtx) {
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
    if (active != null && active.hasOrders() && _paused == false) {
      _game.endWait(active);
    }
  }

  public void activate(BoardHex hex) {
    Unit active = _game.selectedUnit();
    Unit last = null;
    if (_spcCtx != null) {
      for (Unit u : _spcCtx) {
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

    if (_paused == false && last != null) {
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

  public void setFocus(BoardHex hex) {
    Log.debug(this, "Setting focus");
    _game.getBoard().setFocus(hex);
  }

  public void choose(BoardHex hex) {
    Unit u = _game.selectedUnit();
    Point p = hex.center();
    JPopupMenu pm = null;
    Location loc = hex.getLocation();
    
    if (u.getLocation().equals(loc)) {
      ArrayList<Unit> ulist = new ArrayList<Unit>();
      ulist.add(u);

      UserCommands spc = specialContext(ulist);
      OrderMenuBuilder om = new OrderMenuBuilder(_frame, _game, ulist, spc);
      pm = om.build();
    } else {
      City c = _game.cityAtLocation(hex.getLocation());

      if (c != null) {
        CityMenuBuilder cmb = new CityMenuBuilder(_frame, _game, c, this);
        pm = cmb.build();
      } else {
        List<Unit> ul = _game.unitsAtLocation(loc);
        UserCommands spc = specialContext(ul);
        OrderMenuBuilder omb = new OrderMenuBuilder(_frame, _game, ul, spc);
        pm = omb.build();
      }
    }

    if (pm != null)
      pm.show(_canvas, p.x, p.y);
  }

  public boolean isDraggable(BoardHex hex) {
    Unit unit = _game.unitAtLocation(hex.getLocation());
    return (unit != null);
  }

  public void setPlayMode() {
    _paused = false;
  }

  public void setPauseMode() {
    _paused = true;
  }

  public void refocus() {
    if (!_canvas.hasFocus()) {
      _canvas.requestFocus();
    }
  }
}
