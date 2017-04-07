package com.developingstorm.games.astar;

import java.util.List;

/**

 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public interface AStarWatcher {
  public static class AStarRequestState {
    public List<AStarState> states;
    public AStarNode start;
    public AStarNode end;
  }
  
  
  void watch(boolean knownError, AStarRequestState states);
  void displayError(AStarNode start, AStarNode end);
}
