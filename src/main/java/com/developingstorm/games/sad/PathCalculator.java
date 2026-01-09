package com.developingstorm.games.sad;

import com.developingstorm.games.astar.AStar;
import com.developingstorm.games.astar.AStarNode;
import com.developingstorm.games.astar.AStarState;
import com.developingstorm.games.astar.AStarWatcher;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;

import java.util.Iterator;
import java.util.List;

/**
 * Calculates paths for unit movement using A* algorithm.
 * Extracted from Game.java to improve maintainability.
 * This is a stateless utility class.
 */
class PathCalculator {

    /**
     * Calculates a path from one location to another.
     * Wrapper method that delegates to calcTravelPath with default parameters.
     */
    static Path calcPath(
        Game game,
        Board board,
        GameListener gameListener,
        Player player,
        Location from,
        Location to,
        Travel travel
    ) {
        Path p = calcTravelPath(game, board, gameListener, player, from, to, travel, false, true);
        return p;
    }

    /**
     * Calculates a path considering terrain, travel type, and exploration constraints.
     * Uses A* pathfinding algorithm.
     */
    static Path calcTravelPath(
        Game game,
        Board board,
        GameListener gameListener,
        Player player,
        Location from,
        Location to,
        Travel travel,
        boolean checkBlocked,
        boolean canExplore
    ) {
        MapState.start(
            game,
            board,
            travel,
            player,
            to,
            checkBlocked,
            canExplore
        );

        MapState start = MapState.getUntested(from);
        MapState goal = MapState.getTerrainTested(to);
        if (start == null || goal == null) {
            if (goal == null) {
                Log.error(
                    "The path's goal is not reachable. Travel is " +
                        travel +
                        " loc==" +
                        to +
                        " " +
                        board.getTerrain(to)
                );
            }
            return null;
        }

        AStarNode s = new AStarNode(start, 1);
        AStarNode g = new AStarNode(goal, 1);
        AStarWatcher watcher = gameListener != null ? gameListener.getWatcher() : null;
        AStar astar = new AStar(
            s,
            g,
            board.getWidth(),
            board.getHeight(),
            watcher
        );
        List<AStarState> list = astar.solve();

        Path path = null;
        if (list != null) {
            Iterator<AStarState> itr = list.iterator();
            while (itr.hasNext()) {
                MapState state = (MapState) itr.next();
                Location loc = state.getLocation();
                if (path == null) {
                    path = new Path(loc);
                } else {
                    path.addLocation(loc);
                }
            }
        }
        if (path != null) {
            path.reverse();
        }

        Log.info(
            path,
            "calculated travel path:" + travel + " from " + from + " to " + to
        );

        if (path == null) {
            path = new Path(to);
        }

        return path;
    }
}
