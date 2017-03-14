package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;

/**
 *
 *
 */
public class MoveController extends UserAction {

  private Location _start;
  private Location _end;
  private int _boardMiddle;

  public MoveController(GameCommander commander, UserActionOwner owner) {
    super(commander, owner);
    _start = _end = commander.getCurrentLocation();
    _boardMiddle = (commander.boardWidth() / 2);
  }
  
  public void keyPressed(KeyEvent ke) {
    
    Location oldEnd = _end;
    switch (ke.getKeyCode()) {
    case KeyEvent.VK_UP:
      if (_end.x >= _boardMiddle) {
        _end = _end.relative(Direction.NORTH_WEST);
      } else {
        _end = _end.relative(Direction.NORTH_EAST);
      }
      break;
    case KeyEvent.VK_DOWN:
      if (_end.x >= _boardMiddle) {
        _end = _end.relative(Direction.SOUTH_WEST);
      } else { 
        _end = _end.relative(Direction.SOUTH_EAST);
      }
      break;
    case KeyEvent.VK_LEFT:
      _end = _end.relative(Direction.WEST);
      break;
    case KeyEvent.VK_RIGHT:
      _end = _end.relative(Direction.EAST);
      break;
    case KeyEvent.VK_PAGE_UP:
      _end = _end.relative(Direction.NORTH_EAST);
      break;
    case KeyEvent.VK_PAGE_DOWN:
      _end = _end.relative(Direction.SOUTH_EAST);
      break;
    case KeyEvent.VK_HOME:
      _end = _end.relative(Direction.NORTH_WEST);
      break;
    case KeyEvent.VK_END:
      _end = _end.relative(Direction.SOUTH_WEST);
      break;
    case KeyEvent.VK_ENTER:
      _commander.move(_end);
      _commander.showLine(null, null);
      _owner.clearAction();
      return;

    case KeyEvent.VK_ESCAPE:
      _commander.showLine(null, null);
      _owner.clearAction();
      return;
    }

    if (!_commander.onBoard(_end)) {
      _end = oldEnd;
    }
    _commander.showLine(null, null);
    _commander.showLocation(_end);
    _commander.showLine(_start, _end);
  }

}
