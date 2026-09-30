package com.developingstorm.games.astar;

import com.developingstorm.games.astar.AStarWatcher.AStarRequestState;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.events.DebugEventBus;
import com.developingstorm.games.sad.events.PathfindingErrorEvent;
import com.developingstorm.games.sad.events.PathfindingProgressEvent;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class AStar {

    public AStarNode initialnode; // Start node
    public AStarNode goalnode; // Desired goal node
    public AStarNode workingNode; // Node retrieved from OPEN

    // public ArrayList openNodes; // Node containers
    public ArrayList<AStarNode> nextNodes; //
    public HashMap<AStarPosition, AStarNode> openNodes;

    // private long this.startTime; // Timing variables
    // private long this.endTime; //

    private boolean[][] closed;
    int width;
    int height;

    private List<AStarWatcher> watchers;
    private DebugEventBus debugEventBus;

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

    /**
     * Constructor that uses debug event bus for pathfinding visualization.
     * The debug event bus parameter can be null if debugging is not enabled,
     * avoiding event overhead during normal gameplay.
     *
     * Type safety: This specifically takes DebugEventBus (not GameEventBus) to prevent
     * accidentally publishing high-volume debug events to the production event bus.
     *
     * @param debugEventBus Debug event bus for pathfinding events (can be null)
     */
    public AStar(
        AStarNode initial,
        AStarNode goal,
        int width,
        int height,
        DebugEventBus debugEventBus
    ) {
        this.initialnode = initial;
        this.width = width;
        this.height = height;
        this.goalnode = goal;
        this.debugEventBus = debugEventBus;
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
        // Notify legacy watchers (for backward compatibility)
        if (this.watchers != null) {
            for (AStarWatcher watcher : this.watchers) {
                watcher.watch(knowError, stateList);
            }
        }

        // Publish event via debug event bus (only when debugging is enabled)
        if (this.debugEventBus != null && stateList != null) {
            debugEventBus.publish(
                new PathfindingProgressEvent(
                    knowError,
                    stateList.states,
                    stateList.start,
                    stateList.end
                )
            );
        }
    }

    private void notifyErrorWatchers(AStarNode start, AStarNode end) {
        // Notify legacy watchers (for backward compatibility)
        if (this.watchers != null) {
            for (AStarWatcher watcher : this.watchers) {
                watcher.displayError(start, end);
            }
        }

        // Publish event via debug event bus (only when debugging is enabled)
        if (this.debugEventBus != null) {
            debugEventBus.publish(new PathfindingErrorEvent(start, end));
        }
    }

    public List<AStarState> solve() {
        List<AStarState> stateList = solve(false);
        // Don't retry with debug visualization - it's distracting during normal gameplay
        // If you need to debug pathfinding, enable this manually
        // if (stateList == null) {
        //     solve(true);
        // }
        return stateList;
    }

    private List<AStarState> solve(boolean debug) {
        closed = new boolean[this.width][this.height];

        // Initializing the initial node
        this.initialnode.f = this.initialnode.estimate(this.goalnode);
        this.initialnode.h = this.initialnode.f;
        this.initialnode.g = 0;
        this.initialnode.ancestor = null;

        // OPEN holds the best known node per position; the queue orders them by f
        // (ties broken toward lower h). Superseded queue entries are skipped on poll.
        openNodes = new HashMap<AStarPosition, AStarNode>();
        PriorityQueue<AStarNode> openQueue = new PriorityQueue<AStarNode>(
            Comparator.<AStarNode>comparingInt(n -> n.f).thenComparingInt(n -> n.h)
        );
        nextNodes = new ArrayList<AStarNode>();

        // Placing initial node on OPEN
        this.openNodes.put(this.initialnode.state.pos(), this.initialnode);
        openQueue.add(this.initialnode);

        int count = 0;

        while (true) {
            count++;

            // Locate next node to retrieve from OPEN, based on lowest f
            this.workingNode = null;
            while (!openQueue.isEmpty()) {
                AStarNode candidate = openQueue.poll();
                AStarPosition pos = candidate.state.pos();
                if (this.openNodes.get(pos) == candidate) {
                    this.openNodes.remove(pos);
                    this.workingNode = candidate;
                    break;
                }
            }

            // Exit if OPEN is empty
            if (this.workingNode == null) {
                Log.debug("No open nodes in A* search:" + count);
                notifyErrorWatchers(this.initialnode, this.goalnode);
                notifyWatchers(false, null);
                return null;
            }

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
                notifyWatchers(false, null);
                return buildStateList(this.workingNode);
            }

            AStarState state = this.workingNode.state;
            this.closed[state.pos().getX()][state.pos().getY()] = true;

            // Retrieve all possible successors of n and add or improve them in OPEN
            nextNodes = this.workingNode.successors();
            for (AStarNode s : this.nextNodes) {
                AStarPosition pos = s.state.pos();
                if (this.closed[pos.getX()][pos.getY()]) {
                    continue;
                }
                s.g = this.workingNode.g + s.cost;
                AStarNode existing = this.openNodes.get(pos);
                if (existing != null && existing.g <= s.g) {
                    continue;
                }
                s.h = s.estimate(this.goalnode);
                s.f = s.g + s.h;
                s.ancestor = this.workingNode;
                this.openNodes.put(pos, s);
                openQueue.add(s);
            }
        }
    }
} // END class AStar
