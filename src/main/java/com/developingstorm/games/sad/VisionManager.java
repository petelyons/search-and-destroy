package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages fog of war and vision for all players.
 *
 * Vision States:
 * - UNEXPLORED (0): Never seen, shown as black fog
 * - EXPLORED (1): Previously seen, terrain visible, units NOT visible
 * - VISIBLE (2): Currently in sight range, units visible
 *
 * Vision Rules:
 * - Each unit type has a vision range (1-5 hexes)
 * - Vision types: SURFACE (land/sea), COMPLETE (sees submarines), WATER (water only)
 * - Hexes within range of friendly units are VISIBLE
 * - Previously VISIBLE hexes become EXPLORED when units move away
 */
public class VisionManager {

    private static final Logger logger = LoggerFactory.getLogger(
        VisionManager.class
    );

    // Vision state constants
    private static final byte UNEXPLORED = 0;
    private static final byte EXPLORED = 1;
    private static final byte VISIBLE = 2;

    private final Game game;
    private final Board board;

    // Per-player vision map: player -> byte[x][y]
    private final Map<Player, byte[][]> visionMaps;

    public VisionManager(Game game, Board board) {
        this.game = game;
        this.board = board;
        this.visionMaps = new HashMap<>();

        // Initialize vision maps for all players
        int width = board.getWidth();
        int height = board.getHeight();

        for (Player player : game.getPlayers()) {
            byte[][] visionMap = new byte[width][height];
            // Initialize all hexes as UNEXPLORED
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    visionMap[x][y] = UNEXPLORED;
                }
            }
            visionMaps.put(player, visionMap);
        }

        logger.info(
            "VisionManager initialized for {} players",
            game.getPlayers().length
        );
    }

    /**
     * Recalculates vision for a specific player based on their unit positions.
     * Should be called at the start of each player's turn.
     */
    public void recalculateVision(Player player) {
        byte[][] visionMap = visionMaps.get(player);
        if (visionMap == null) {
            logger.warn("No vision map for player: {}", player.toString());
            return;
        }

        int width = board.getWidth();
        int height = board.getHeight();

        // Mark all currently VISIBLE hexes as EXPLORED (they were seen)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (visionMap[x][y] == VISIBLE) {
                    visionMap[x][y] = EXPLORED;
                }
            }
        }

        // Calculate new vision based on unit positions
        List<Unit> playerUnits = player.units;
        for (Unit unit : playerUnits) {
            Location unitLoc = unit.getLocation();
            int visionRange = unit.getType().getVisionDistance();
            Vision visionType = unit.getType().getVision();

            // Mark hexes within vision range as VISIBLE
            markVisionArea(visionMap, unitLoc, visionRange, visionType);
        }

        logger.debug(
            "Recalculated vision for {}: {} units",
            player.toString(),
            playerUnits.size()
        );
    }

    /**
     * Marks hexes within vision range of a unit as VISIBLE.
     */
    private void markVisionArea(
        byte[][] visionMap,
        Location center,
        int range,
        Vision visionType
    ) {
        int width = board.getWidth();
        int height = board.getHeight();

        // Use simple radius-based vision (can be optimized with line-of-sight later)
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Location loc = Location.get(x, y);
                int distance = center.distance(loc);

                if (distance <= range) {
                    // Check if this hex type is visible by this vision type
                    if (canSeeHex(loc, visionType)) {
                        visionMap[x][y] = VISIBLE;

                        // Mark as explored even if not currently visible by vision type
                        if (visionMap[x][y] < EXPLORED) {
                            visionMap[x][y] = EXPLORED;
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if a hex can be seen with the given vision type.
     */
    private boolean canSeeHex(Location loc, Vision visionType) {
        switch (visionType) {
            case COMPLETE:
                // Can see everything (destroyers, cruisers, battleships)
                return true;
            case SURFACE:
                // Can see land and water surface (most units)
                return true;
            case WATER:
                // Can only see water (submarines)
                return board.isWater(loc);
            case NONE:
            default:
                return false;
        }
    }

    /**
     * Checks if a hex is currently visible to a player.
     */
    public boolean isVisible(Player player, Location loc) {
        byte[][] visionMap = visionMaps.get(player);
        if (visionMap == null) return false;

        int x = loc.getX();
        int y = loc.getY();

        if (
            x < 0 || x >= visionMap.length || y < 0 || y >= visionMap[0].length
        ) {
            return false;
        }

        return visionMap[x][y] == VISIBLE;
    }

    /**
     * Checks if a hex has been explored (previously seen) by a player.
     */
    public boolean isExplored(Player player, Location loc) {
        byte[][] visionMap = visionMaps.get(player);
        if (visionMap == null) return false;

        int x = loc.getX();
        int y = loc.getY();

        if (
            x < 0 || x >= visionMap.length || y < 0 || y >= visionMap[0].length
        ) {
            return false;
        }

        return visionMap[x][y] >= EXPLORED;
    }

    /**
     * Gets the vision state for a hex (for debugging/UI).
     * Returns: 0=unexplored, 1=explored, 2=visible
     */
    public byte getVisionState(Player player, Location loc) {
        byte[][] visionMap = visionMaps.get(player);
        if (visionMap == null) return UNEXPLORED;

        int x = loc.getX();
        int y = loc.getY();

        if (
            x < 0 || x >= visionMap.length || y < 0 || y >= visionMap[0].length
        ) {
            return UNEXPLORED;
        }

        return visionMap[x][y];
    }

    /**
     * Reveals entire map to a player (for debugging/god mode).
     */
    public void revealAll(Player player) {
        byte[][] visionMap = visionMaps.get(player);
        if (visionMap == null) return;

        for (int x = 0; x < visionMap.length; x++) {
            for (int y = 0; y < visionMap[0].length; y++) {
                visionMap[x][y] = VISIBLE;
            }
        }

        logger.info("Revealed entire map for player: {}", player.toString());
    }
}
