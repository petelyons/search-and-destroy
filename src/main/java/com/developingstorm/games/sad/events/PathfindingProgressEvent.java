package com.developingstorm.games.sad.events;

import com.developingstorm.games.astar.AStarNode;
import com.developingstorm.games.astar.AStarState;
import java.util.List;

/**
 * Event fired during A* pathfinding to visualize the search process.
 * Used for debugging and visualization of pathfinding algorithm.
 * Replaces AStarWatcher.watch() callback.
 */
public class PathfindingProgressEvent extends AbstractGameEvent {
    private final boolean knownError;
    private final List<AStarState> states;
    private final AStarNode start;
    private final AStarNode end;

    public PathfindingProgressEvent(
        boolean knownError,
        List<AStarState> states,
        AStarNode start,
        AStarNode end
    ) {
        super(GameEventType.PATHFINDING_PROGRESS);
        this.knownError = knownError;
        this.states = states;
        this.start = start;
        this.end = end;
    }

    public boolean isKnownError() {
        return knownError;
    }

    public List<AStarState> getStates() {
        return states;
    }

    public AStarNode getStart() {
        return start;
    }

    public AStarNode getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "PathfindingProgressEvent[knownError=" + knownError +
               ", states=" + (states != null ? states.size() : 0) +
               ", start=" + start + ", end=" + end + "]";
    }
}
