package com.developingstorm.games.astar;

import java.util.List;

/**

 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public interface AStarWatcher {
  void watch(List<AStarState> states);
  void displayError(AStarNode start, AStarNode end);
}
