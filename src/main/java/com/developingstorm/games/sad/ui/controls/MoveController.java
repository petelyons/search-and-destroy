package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyEvent;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.Location;

/**
 *
 *
 */
public class MoveController extends UserAction {

  private Location start;
  private Location end;
  private int boardMiddle;

  public MoveController(GameCommander commander, UserActionOwner owner) {
    super(commander, owner);
    start = end = commander.getCurrentLocation();
    boardMiddle = (commander.boardWidth() / 2);
  }
  
  public void keyPressed(KeyEvent ke) {
    
    Location oldEnd = end;
    switch (ke.getKeyCode()) {
    case KeyEvent.VK_UP:
      if (this.end.x >= this.boardMiddle) {
        end = this.end.relative(Direction.NORTH_WEST);
      } else {
        end = this.end.relative(Direction.NORTH_EAST);
      }
      break;
    case KeyEvent.VK_DOWN:
      if (this.end.x >= this.boardMiddle) {
        end = this.end.relative(Direction.SOUTH_WEST);
      } else { 
        end = this.end.relative(Direction.SOUTH_EAST);
      }
      break;
    case KeyEvent.VK_LEFT:
      end = this.end.relative(Direction.WEST);
      break;
    case KeyEvent.VK_RIGHT:
      end = this.end.relative(Direction.EAST);
      break;
    case KeyEvent.VK_PAGE_UP:
      end = this.end.relative(Direction.NORTH_EAST);
      break;
    case KeyEvent.VK_PAGE_DOWN:
      end = this.end.relative(Direction.SOUTH_EAST);
      break;
    case KeyEvent.VK_HOME:
      end = this.end.relative(Direction.NORTH_WEST);
      break;
    case KeyEvent.VK_END:
      end = this.end.relative(Direction.SOUTH_WEST);
      break;
    case KeyEvent.VK_ENTER:
      this.commander.move(this.end);
      this.commander.showLine(null, null);
      this.owner.clearAction();
      return;

    case KeyEvent.VK_ESCAPE:
      this.commander.showLine(null, null);
      this.owner.clearAction();
      return;
    }

    if (!this.commander.onBoard(this.end)) {
      end = oldEnd;
    }
    this.commander.showLine(null, null);
    this.commander.showLocation(this.end);
    this.commander.showLine(this.start, this.end);
  }

}
