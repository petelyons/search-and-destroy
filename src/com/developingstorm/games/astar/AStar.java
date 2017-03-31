package com.developingstorm.games.astar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.util.Log;

public class AStar {

  public AStarNode _initialnode; // Start node
  public AStarNode _goalnode; // Desired goal node
  public AStarNode _workingNode; // Node retrieved from OPEN
  public AStarNode _tempNode; // Temporary node

  // public ArrayList _openNodes; // Node containers
  public ArrayList<AStarNode> _nextNodes; //

  public HashMap<AStarPosition, AStarNode> _openNodes;

  // private long _startTime; // Timing variables
  // private long _endTime; //

  boolean[][] _closed;

  List<AStarWatcher> _watchers;

  public AStar(AStarNode initial, AStarNode goal, int width, int height,
      AStarWatcher w) {
    _initialnode = initial;
    _goalnode = goal;
    _closed = new boolean[width][height];
    // Trace.println("Watcher:" + w);
    if (w != null) {
      _watchers = new ArrayList<AStarWatcher>();
      _watchers.add(w);
    }
  }

  // public AStar(Node initial, Node goal, int width, int height) {
  // _initialnode = initial;
  // _goalnode = goal;
  // _closed = new boolean[width][height];
  // _watchers = null;
  // }

  private static ArrayList<AStarState> buildStateList(AStarNode n) {
    AStarNode node = n;
    ArrayList<AStarState> list = new ArrayList<AStarState>();
    while (node != null) {
      list.add(node.state);
      node = node.ancestor;
    }
    if (list.isEmpty()) {
      throw new SaDException("Empty path????");
    }
    return list;
  }

  private void notifyWatchers(ArrayList<AStarState> stateList) {
    if (_watchers != null) {
      Iterator<AStarWatcher> itrx = _watchers.iterator();
      while (itrx.hasNext()) {
        AStarWatcher w = (AStarWatcher) itrx.next();
        w.watch(stateList);
      }
    }
  }

  public List<AStarState> solve() {
    return solve(false);
  }
  
  private List<AStarState> solve(boolean debug) {

    // _startTime = System.currentTimeMillis();

    // Initializing the initial node
    _initialnode.f = _initialnode.estimate(_goalnode);
    _initialnode.h = _initialnode.f;
    _initialnode.g = 0;

    // Instantiating OPEN, CLOSED and M
    // _openNodes = new ArrayList();
    _openNodes = new HashMap<AStarPosition, AStarNode>();
    _nextNodes = new ArrayList<AStarNode>();

    // Placing initial node on OPEN
    _openNodes.put(_initialnode.state.pos(), _initialnode);

    int count = 0;

    // After finishing the initial phase, we enter the main loop
    // of the A* algorithm
    while (true) {
      count++;

      // Check if OPEN is empty, exit if this is the case
      if (_openNodes.size() == 0) {
        Log.error("No open nodes in A* search:" + count);
       // solve(true);
        notifyWatchers(null);
        return null;
      }

      // Locate next node to retrieve from OPEN, based on lowest heuristic

      int low = 0;
      int num;
      Collection<AStarNode> nodes = _openNodes.values();
      for (AStarNode nx : nodes) {
        if (low == 0) {
          low = nx.f;
          _workingNode = nx;
        }
        num = nx.f;
        if (num < low) {
          low = num;
          _workingNode = nx;
        }
      }
      // Move selected node from OPEN to n
      _openNodes.remove(_workingNode.state.pos());

      if (_watchers != null) {
        ArrayList<AStarState> stateList = buildStateList(_workingNode);
        notifyWatchers(stateList);
      }

      // Successful exit if n proves to be the goal node
      if (_workingNode.equals(_goalnode)) {
        // _endTime = System.currentTimeMillis();
        // printStatistics(n);
        notifyWatchers(null);
        return buildStateList(_workingNode);
      }

      // Retrieve all possible successors of n
      _nextNodes = _workingNode.successors();

      // Compute f-, g- and h-value for every remaining successor
      for (int i = 0; i < _nextNodes.size(); i++) {
        AStarNode s = _nextNodes.get(i);
        s.g = _workingNode.g + s.cost;
        s.h = s.estimate(_goalnode);
        s.f = s.g + s.h;
      }

      // Establishing arcs from n to each member of _nextNodes
      for (int i = 0; i < _nextNodes.size(); i++) {
        _tempNode = _nextNodes.get(i);
        _tempNode.ancestor = _workingNode;
      }

      AStarState state = _workingNode.state;
      _closed[state.pos().getX()][state.pos().getY()] = true;

      // Augmenting OPEN with suitable nodes from _nextNodes
      for (int i = 0; i < _nextNodes.size(); i++) {
        AStarNode n = _nextNodes.get(i);
        state = n.state;
        if (_closed[state.pos().getX()][state.pos().getY()] == false) {
          if (!_openNodes.containsKey(state.pos()))
            _openNodes.put(state.pos(), n);
        }
      }

    }
  }

} // END class AStar
