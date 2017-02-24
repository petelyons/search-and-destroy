

// This class defines a general node for use within the A*-algorithm.
// It relies on the existence of a specialized State class which 
// should provide details on the particular problem to be solved.
//
// Note that class variables are kept public, and should thus be
// accessed directly and not through class methods.
package com.developingstorm.games.astar;

import java.util.ArrayList;
import java.util.List;

public class AStarNode {
  public AStarState state; // Contains state information
  public int f; // Value of heuristic evaluation function (f = g + h)
  public int g; // Accumulated cost
  public int h; // Estimate of remaining cost
  public int cost; // Cost of this particular node
  public AStarNode ancestor; // Reference to the node's immediate parent

  // Constructor
  public AStarNode(AStarState s, int cost) {
    this.state = s;
    this.cost = cost;
  }

  // Equality check; uses the equality check of the State class
  public boolean equals(AStarNode n) {
    if (state.equals(n.state)) {
      return true;
    } else {
      return false;
    }
  }

  // Convert node to string
  public String toString() {
    return "State=" + state.toString() + ", f=" + f + ", g=" + g + ", h=" + h;
  }

  // Check for ancestors
  public boolean hasAncestor() {
    if (ancestor != null) {
      return true;
    } else {
      return false;
    }
  }

  // Successors
  public ArrayList<AStarNode> successors() {
    ArrayList<AStarNode> nodes = new ArrayList<AStarNode>();
    List<AStarState> states = state.successors();
    for (int i = 0; i < states.size(); i++) {
      // Note: for a more general implementation, the uniform costs
      // should be replace by an operator specific cost
      // nodes.add(0, new Node((State) states.get(i), 0));
      nodes.add(new AStarNode(states.get(i), (i == 0 ? 1 : 2))); // expirimental
      // other
      // is
      // proven
    }
    return nodes;
  }

  public int estimate(AStarNode goalnode) {
    return state.estimate(goalnode.state);
  }

} // End class Node
