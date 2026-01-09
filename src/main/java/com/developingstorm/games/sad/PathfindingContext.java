package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;

import java.util.ArrayList;

/**
 * Context for pathfinding operations.
 * Replaces static state in MapState to enable concurrent pathfinding and easier testing.
 */
public class PathfindingContext {
    private final Game game;
    private final Board board;
    private final Travel travel;
    private final Player player;
    private final Location goal;
    private final boolean checkBlocked;
    private final boolean canExplore;
    private final ArrayList<Unit> units;

    /**
     * Creates a pathfinding context for game pathfinding.
     */
    public PathfindingContext(Game game, Board board, Travel travel, Player player,
                              Location goal, boolean checkBlocked, boolean canExplore) {
        this.game = game;
        this.board = board;
        this.travel = travel;
        this.player = player;
        this.goal = goal;
        this.checkBlocked = checkBlocked;
        this.canExplore = canExplore;
        this.units = null;
    }

    /**
     * Creates a pathfinding context with specific units.
     */
    public PathfindingContext(Board board, Travel travel, Player player,
                              Location goal, ArrayList<Unit> units) {
        this.game = null;
        this.board = board;
        this.travel = travel;
        this.player = player;
        this.goal = goal;
        this.checkBlocked = false;
        this.canExplore = false;
        this.units = units;
    }

    public Game getGame() {
        return game;
    }

    public Board getBoard() {
        return board;
    }

    public Travel getTravel() {
        return travel;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getGoal() {
        return goal;
    }

    public boolean isCheckBlocked() {
        return checkBlocked;
    }

    public boolean isCanExplore() {
        return canExplore;
    }

    public ArrayList<Unit> getUnits() {
        return units;
    }
}
