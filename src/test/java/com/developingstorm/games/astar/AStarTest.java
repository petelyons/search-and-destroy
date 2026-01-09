package com.developingstorm.games.astar;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Test suite for the A* pathfinding algorithm
 */
public class AStarTest {

    @Test
    public void testSimplePath() {
        // Create a simple 5x5 grid with no obstacles
        int width = 5;
        int height = 5;
        boolean[][] obstacles = new boolean[width][height];
        
        // Start at (0,0), goal at (4,4)
        GridState start = new GridState(0, 0, obstacles, width, height);
        GridState goal = new GridState(4, 4, obstacles, width, height);
        
        AStarNode startNode = new AStarNode(start, 0);
        AStarNode goalNode = new AStarNode(goal, 0);
        
        AStar astar = new AStar(startNode, goalNode, width, height, null);
        List<AStarState> path = astar.solve();
        
        assertNotNull(path, "Path should be found");
        assertTrue(path.size() > 0, "Path should not be empty");
        
        // Verify start and end positions
        assertEquals(start, path.get(path.size() - 1), "Path should start at start position");
        assertEquals(goal, path.get(0), "Path should end at goal position");
        
        // Manhattan distance from (0,0) to (4,4) is 8, so path should be 9 nodes (including start)
        assertEquals(9, path.size(), "Path should have optimal length");
    }

    @Test
    public void testPathWithObstacles() {
        // Create a 5x5 grid with a wall
        int width = 5;
        int height = 5;
        boolean[][] obstacles = new boolean[width][height];
        
        // Create a vertical wall at x=2 (except y=0)
        for (int y = 1; y < height; y++) {
            obstacles[2][y] = true;
        }
        
        // Start at (0,2), goal at (4,2)
        GridState start = new GridState(0, 2, obstacles, width, height);
        GridState goal = new GridState(4, 2, obstacles, width, height);
        
        AStarNode startNode = new AStarNode(start, 0);
        AStarNode goalNode = new AStarNode(goal, 0);
        
        AStar astar = new AStar(startNode, goalNode, width, height, null);
        List<AStarState> path = astar.solve();
        
        assertNotNull(path, "Path should be found despite obstacles");
        assertTrue(path.size() > 0, "Path should not be empty");
        
        // Verify start and end
        assertEquals(start, path.get(path.size() - 1), "Path should start at start position");
        assertEquals(goal, path.get(0), "Path should end at goal position");
        
        // Path should go around the wall (up to y=0, then across, then down)
        // So it should be longer than direct path
        assertTrue(path.size() > 5, "Path should go around obstacle");
    }

    @Test
    public void testNoPathAvailable() {
        // Create a 5x5 grid with complete wall blocking path
        int width = 5;
        int height = 5;
        boolean[][] obstacles = new boolean[width][height];
        
        // Create a complete vertical wall at x=2
        for (int y = 0; y < height; y++) {
            obstacles[2][y] = true;
        }
        
        // Start at (0,2), goal at (4,2)
        GridState start = new GridState(0, 2, obstacles, width, height);
        GridState goal = new GridState(4, 2, obstacles, width, height);
        
        AStarNode startNode = new AStarNode(start, 0);
        AStarNode goalNode = new AStarNode(goal, 0);
        
        AStar astar = new AStar(startNode, goalNode, width, height, null);
        List<AStarState> path = astar.solve();
        
        assertNull(path, "No path should be found when completely blocked");
    }

    @Test
    public void testSameStartAndGoal() {
        // Test when start and goal are the same
        int width = 5;
        int height = 5;
        boolean[][] obstacles = new boolean[width][height];
        
        GridState start = new GridState(2, 2, obstacles, width, height);
        GridState goal = new GridState(2, 2, obstacles, width, height);
        
        AStarNode startNode = new AStarNode(start, 0);
        AStarNode goalNode = new AStarNode(goal, 0);
        
        AStar astar = new AStar(startNode, goalNode, width, height, null);
        List<AStarState> path = astar.solve();
        
        assertNotNull(path, "Path should be found");
        assertEquals(1, path.size(), "Path should have single node when start equals goal");
        assertEquals(start, path.get(0), "Single node should be the start/goal");
    }

    @Test
    public void testAdjacentNodes() {
        // Test path between adjacent nodes
        int width = 5;
        int height = 5;
        boolean[][] obstacles = new boolean[width][height];
        
        GridState start = new GridState(1, 1, obstacles, width, height);
        GridState goal = new GridState(2, 1, obstacles, width, height);
        
        AStarNode startNode = new AStarNode(start, 0);
        AStarNode goalNode = new AStarNode(goal, 0);
        
        AStar astar = new AStar(startNode, goalNode, width, height, null);
        List<AStarState> path = astar.solve();
        
        assertNotNull(path, "Path should be found");
        assertEquals(2, path.size(), "Path between adjacent nodes should have 2 nodes");
    }
}