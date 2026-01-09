package com.developingstorm.games.astar;

import com.developingstorm.games.astar.AStarWatcher.AStarRequestState;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AStar {

    public AStarNode initialnode; // Start node
    public AStarNode goalnode; // Desired goal node
    public AStarNode workingNode; // Node retrieved from OPEN
    public AStarNode tempNode; // Temporary node

    // public ArrayList openNodes; // Node containers
    public ArrayList<AStarNode> nextNodes; //
    public HashMap<AStarPosition, AStarNode> openNodes;

    // private long this.startTime; // Timing variables
    // private long this.endTime; //

    private boolean[][] closed;
    int width;
    int height;

    private List<AStarWatcher> watchers;

    public AStar(
        AStarNode initial,
        AStarNode goal,
        int width,
        int height,
        AStarWatcher w
    ) {
        this.initialnode = initial;
        this.width = width;
        this.height = height;
        this.goalnode = goal;

        // Trace.println("Watcher:" + w);
        if (w != null) {
            this.watchers = new ArrayList<AStarWatcher>();
            this.watchers.add(w);
        }
    }

    // public AStar(Node initial, Node goal, int width, int height) {
    // initialnode = initial;
    // goalnode = goal;
    // closed = new boolean[width][height];
    // watchers = null;
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

    private void notifyWatchers(
        boolean knowError,
        AStarRequestState stateList
    ) {
        if (this.watchers != null) {
            for (AStarWatcher watcher : this.watchers) {
                watcher.watch(knowError, stateList);
            }
        }
    }

    private void notifyErrorWatchers(AStarNode start, AStarNode end) {
        if (this.watchers != null) {
            for (AStarWatcher watcher : this.watchers) {
                watcher.displayError(start, end);
            }
        }
    }

    public List<AStarState> solve() {
        List<AStarState> stateList = solve(false);
        if (stateList == null) {
            solve(true);
        }
        return stateList;
    }

    private List<AStarState> solve(boolean debug) {
        closed = new boolean[this.width][this.height];
        // this.startTime = System.currentTimeMillis();

        // Initializing the initial node
        this.initialnode.f = this.initialnode.estimate(this.goalnode);
        this.initialnode.h = this.initialnode.f;
        this.initialnode.g = 0;

        // Instantiating OPEN, CLOSED and M
        // openNodes = new ArrayList();
        openNodes = new HashMap<AStarPosition, AStarNode>();
        nextNodes = new ArrayList<AStarNode>();

        // Placing initial node on OPEN
        this.openNodes.put(this.initialnode.state.pos(), this.initialnode);

        int count = 0;

        // After finishing the initial phase, we enter the main loop
        // of the A* algorithm
        while (true) {
            count++;

            // Check if OPEN is empty, exit if this is the case
            if (this.openNodes.size() == 0) {
                Log.error("No open nodes in A* search:" + count);
                notifyErrorWatchers(this.initialnode, this.goalnode);

                // solve(true);
                notifyWatchers(false, null);
                return null;
            }

            // Locate next node to retrieve from OPEN, based on lowest heuristic

            int low = 0;
            int num;
            Collection<AStarNode> nodes = this.openNodes.values();
            for (AStarNode nx : nodes) {
                if (low == 0) {
                    low = nx.f;
                    workingNode = nx;
                }
                num = nx.f;
                if (num < low) {
                    low = num;
                    workingNode = nx;
                }
            }
            // Move selected node from OPEN to n
            this.openNodes.remove(this.workingNode.state.pos());

            if (this.watchers != null) {
                ArrayList<AStarState> stateList = buildStateList(
                    this.workingNode
                );
                AStarRequestState stateReq = new AStarRequestState();
                stateReq.states = stateList;
                stateReq.start = initialnode;
                stateReq.end = goalnode;
                notifyWatchers(debug, stateReq);
            }

            // Successful exit if n proves to be the goal node
            if (this.workingNode.equals(this.goalnode)) {
                // this.endTime = System.currentTimeMillis();
                // printStatistics(n);
                notifyWatchers(false, null);
                return buildStateList(this.workingNode);
            }

            // Retrieve all possible successors of n
            nextNodes = this.workingNode.successors();

            // Compute f-, g- and h-value for every remaining successor
            for (int i = 0; i < this.nextNodes.size(); i++) {
                AStarNode s = this.nextNodes.get(i);
                s.g = this.workingNode.g + s.cost;
                s.h = s.estimate(this.goalnode);
                s.f = s.g + s.h;
            }

            // Establishing arcs from n to each member of this.nextNodes
            for (int i = 0; i < this.nextNodes.size(); i++) {
                tempNode = this.nextNodes.get(i);
                this.tempNode.ancestor = workingNode;
            }

            AStarState state = this.workingNode.state;
            this.closed[state.pos().getX()][state.pos().getY()] = true;

            // Augmenting OPEN with suitable nodes from this.nextNodes
            for (int i = 0; i < this.nextNodes.size(); i++) {
                AStarNode n = this.nextNodes.get(i);
                state = n.state;
                if (
                    this.closed[state.pos().getX()][state.pos().getY()] == false
                ) {
                    if (
                        !this.openNodes.containsKey(state.pos())
                    ) this.openNodes.put(state.pos(), n);
                }
            }
        }
    }
} // END class AStar
