package com.developingstorm.games.astar;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

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

        AStar astar = new AStar(
            startNode,
            goalNode,
            width,
            height,
            (AStarWatcher) null
        );
        List<AStarState> path = astar.solve();

        assertNotNull(path, "Path should be found");
        assertTrue(path.size() > 0, "Path should not be empty");

        // Verify start and end positions
        assertEquals(
            start,
            path.get(path.size() - 1),
            "Path should start at start position"
        );
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

        AStar astar = new AStar(
            startNode,
            goalNode,
            width,
            height,
            (AStarWatcher) null
        );
        List<AStarState> path = astar.solve();

        assertNotNull(path, "Path should be found despite obstacles");
        assertTrue(path.size() > 0, "Path should not be empty");

        // Verify start and end
        assertEquals(
            start,
            path.get(path.size() - 1),
            "Path should start at start position"
        );
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

        AStar astar = new AStar(
            startNode,
            goalNode,
            width,
            height,
            (AStarWatcher) null
        );
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

        AStar astar = new AStar(
            startNode,
            goalNode,
            width,
            height,
            (AStarWatcher) null
        );
        List<AStarState> path = astar.solve();

        assertNotNull(path, "Path should be found");
        assertEquals(
            1,
            path.size(),
            "Path should have single node when start equals goal"
        );
        assertEquals(
            start,
            path.get(0),
            "Single node should be the start/goal"
        );
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

        AStar astar = new AStar(
            startNode,
            goalNode,
            width,
            height,
            (AStarWatcher) null
        );
        List<AStarState> path = astar.solve();

        assertNotNull(path, "Path should be found");
        assertEquals(
            2,
            path.size(),
            "Path between adjacent nodes should have 2 nodes"
        );
    }

    @Test
    public void testPathsAreShortestOnRandomGrids() {
        int width = 20;
        int height = 20;
        Random rnd = new Random(42);
        for (int trial = 0; trial < 300; trial++) {
            boolean[][] obstacles = new boolean[width][height];
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    obstacles[x][y] = rnd.nextInt(100) < 30;
                }
            }
            int sx = rnd.nextInt(width), sy = rnd.nextInt(height);
            int gx = rnd.nextInt(width), gy = rnd.nextInt(height);
            obstacles[sx][sy] = false;
            obstacles[gx][gy] = false;

            int expected = bfsSteps(obstacles, width, height, sx, sy, gx, gy);

            GridState start = new GridState(sx, sy, obstacles, width, height);
            GridState goal = new GridState(gx, gy, obstacles, width, height);
            AStar astar = new AStar(
                new AStarNode(start, 0),
                new AStarNode(goal, 0),
                width,
                height,
                (AStarWatcher) null
            );
            List<AStarState> path = astar.solve();

            if (expected < 0) {
                assertNull(path, "trial " + trial + ": goal is unreachable");
            } else {
                assertNotNull(path, "trial " + trial + ": path should be found");
                assertEquals(expected + 1, path.size(),
                    "trial " + trial + ": path should be shortest");
            }
        }
    }

    private static int bfsSteps(boolean[][] obstacles, int w, int h,
                                int sx, int sy, int gx, int gy) {
        int[][] dist = new int[w][h];
        for (int[] row : dist) java.util.Arrays.fill(row, -1);
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        dist[sx][sy] = 0;
        queue.add(new int[] {sx, sy});
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] d : dirs) {
                int nx = c[0] + d[0], ny = c[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                if (obstacles[nx][ny] || dist[nx][ny] >= 0) continue;
                dist[nx][ny] = dist[c[0]][c[1]] + 1;
                queue.add(new int[] {nx, ny});
            }
        }
        return dist[gx][gy];
    }
}
