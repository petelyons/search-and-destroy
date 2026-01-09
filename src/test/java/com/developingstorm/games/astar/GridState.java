package com.developingstorm.games.astar;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple grid state for testing A* algorithm
 */
public class GridState implements AStarState {
    private final GridPosition position;
    private final boolean[][] obstacles;
    private final int width;
    private final int height;

    public GridState(int x, int y, boolean[][] obstacles, int width, int height) {
        this.position = new GridPosition(x, y);
        this.obstacles = obstacles;
        this.width = width;
        this.height = height;
    }

    @Override
    public AStarPosition pos() {
        return position;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof GridState)) return false;
        GridState other = (GridState) obj;
        return position.equals(other.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    @Override
    public int estimate(AStarState goal) {
        // Manhattan distance heuristic
        AStarPosition goalPos = goal.pos();
        return Math.abs(position.getX() - goalPos.getX()) + 
               Math.abs(position.getY() - goalPos.getY());
    }

    @Override
    public List<AStarState> successors() {
        List<AStarState> result = new ArrayList<AStarState>();
        int x = position.getX();
        int y = position.getY();

        // Try all four directions: up, down, left, right
        int[][] directions = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        
        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];
            
            // Check bounds
            if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                // Check if not an obstacle
                if (!obstacles[newX][newY]) {
                    result.add(new GridState(newX, newY, obstacles, width, height));
                }
            }
        }
        
        return result;
    }

    @Override
    public String toString() {
        return "GridState" + position.toString();
    }
}