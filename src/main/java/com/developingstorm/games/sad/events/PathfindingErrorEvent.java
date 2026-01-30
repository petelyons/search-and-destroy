package com.developingstorm.games.sad.events;

import com.developingstorm.games.astar.AStarNode;

/**
 * Event fired when A* pathfinding encounters an error.
 * Used for debugging pathfinding failures.
 * Replaces AStarWatcher.displayError() callback.
 */
public class PathfindingErrorEvent extends AbstractGameEvent {
    private final AStarNode start;
    private final AStarNode end;

    public PathfindingErrorEvent(AStarNode start, AStarNode end) {
        super(GameEventType.PATHFINDING_ERROR);
        this.start = start;
        this.end = end;
    }

    public AStarNode getStart() {
        return start;
    }

    public AStarNode getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "PathfindingErrorEvent[start=" + start + ", end=" + end + "]";
    }
}
