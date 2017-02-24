package com.developingstorm.games.astar;

import java.util.List;

public interface AStarState {

  /**
   * Compare one state with another
   * 
   * @param state
   * @return
   */
  boolean equals(AStarState state);

  /**
   * Weight the move to the goal state. Higher values are
   * 
   * @param goal
   * @return
   */
  int estimate(AStarState goal);

  List<AStarState> successors();

  AStarPosition pos();

}