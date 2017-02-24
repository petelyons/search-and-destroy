package com.developingstorm.games.sad;

import com.developingstorm.games.astar.AStarWatcher;
import com.developingstorm.games.hexboard.Location;

/**

 * 
 */
public interface GameListener {
  void abort();

  void selectUnit(Unit u);

  void trackUnit(Unit u);

  void killUnit(Unit u, boolean showDeath);

  void hitLocation(Location loc);

  void selectPlayer(Player p);

  void notifyWait();

  AStarWatcher getWatcher();

  void newTurn(int t);
}
