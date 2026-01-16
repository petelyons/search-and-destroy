package com.developingstorm.games.sad.persistence;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.GameException;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.orders.DirectionalMove;
import com.developingstorm.games.sad.orders.Disband;
import com.developingstorm.games.sad.orders.Explore;
import com.developingstorm.games.sad.orders.HeadHome;
import com.developingstorm.games.sad.orders.Move;
import com.developingstorm.games.sad.orders.Patrol;
import com.developingstorm.games.sad.orders.Sentry;
import com.developingstorm.games.sad.orders.SkipTurn;
import com.developingstorm.games.sad.orders.Unload;
import com.developingstorm.games.sad.util.json.JsonFormatter;
import com.developingstorm.games.sad.util.json.JsonObj;
import com.developingstorm.games.sad.util.json.JsonParser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializes and deserializes complete game state to/from JSON files.
 *
 * Game saves include:
 * - Game metadata (version, turn number, current player)
 * - All players (name, isHuman/robot, cities)
 * - All units (type, location, hits, orders, carrying)
 * - All cities (name, location, owner, production)
 * - Board reference (map file)
 */
public class GameStateSerializer {

    private static final Logger logger = LoggerFactory.getLogger(
        GameStateSerializer.class
    );

    private static final String SAVE_VERSION = "1.2"; // Added explored areas
    private static final String SAVE_DIR = "saves";

    /**
     * Serializes the complete game state to JSON.
     *
     * @param game the game to serialize
     * @return JSON object containing all game state
     */
    public JsonObj serializeGame(Game game) {
        logger.info("Serializing game state...");

        JsonObj root = new JsonObj();

        // Metadata
        root.put("version", SAVE_VERSION);
        root.put("turn", game.getTurn());
        root.put("currentPlayerIndex", game.currentPlayer().id);
        root.put("savedAt", System.currentTimeMillis());

        // Board reference (we'll save the map separately)
        JsonObj boardInfo = new JsonObj();
        boardInfo.put("width", game.getBoard().getWidth());
        boardInfo.put("height", game.getBoard().getHeight());
        boardInfo.put("mapFile", "map.txt"); // Map saved alongside game state
        root.put("board", boardInfo);

        // Players
        Player[] players = game.getPlayers();
        Object[] playersArray = new Object[players.length];
        for (int i = 0; i < players.length; i++) {
            Player player = players[i];
            playersArray[i] = serializePlayer(player, i, game);
        }
        root.put("players", playersArray);

        // Units
        List<Unit> allUnits = game.units();
        Object[] unitsArray = new Object[allUnits.size()];
        for (int i = 0; i < allUnits.size(); i++) {
            unitsArray[i] = UnitSerializer.serializeUnit(allUnits.get(i));
        }
        root.put("units", unitsArray);

        // Cities
        List<City> allCities = game.getBoard().getCities();
        Object[] citiesArray = new Object[allCities.size()];
        for (int i = 0; i < allCities.size(); i++) {
            citiesArray[i] = allCities.get(i).toJson(); // City already has toJson()
        }
        root.put("cities", citiesArray);

        logger.info(
            "Game state serialized: {} players, {} units, {} cities",
            players.length,
            allUnits.size(),
            allCities.size()
        );

        return root;
    }

    /**
     * Serializes a single player.
     */
    private JsonObj serializePlayer(Player player, int index, Game game) {
        JsonObj json = new JsonObj();
        json.put("index", index);
        json.put("id", player.getId());
        json.put("name", player.toJsonLink()); // Save actual player name
        json.put("isHuman", !player.isRobot());

        // Serialize explored areas (v1.2+)
        int width = game.getBoard().getWidth();
        int height = game.getBoard().getHeight();
        java.util.List<JsonObj> exploredList = new java.util.ArrayList<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Location loc = Location.get(x, y);
                if (player.isExplored(loc)) {
                    JsonObj exploredLoc = new JsonObj();
                    exploredLoc.put("x", x);
                    exploredLoc.put("y", y);
                    exploredList.add(exploredLoc);
                }
            }
        }
        json.put("explored", exploredList.toArray());

        return json;
    }

    /**
     * Saves game state to a file in the user's home directory.
     *
     * @param game the game to save
     * @param saveName the name for this save (without extension)
     * @throws IOException if file operations fail
     */
    public void saveGame(Game game, String saveName) throws IOException {
        // Create save directory if it doesn't exist (relative to cwd)
        Path saveDir = Paths.get(SAVE_DIR);
        Files.createDirectories(saveDir);

        // Generate filenames with human-readable timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss"
        );
        String timestamp = dateFormat.format(new Date());
        String filename = saveName + "_" + timestamp + ".json";
        String mapFilename = saveName + "_" + timestamp + "_map.txt";
        Path savePath = saveDir.resolve(filename);
        Path mapPath = saveDir.resolve(mapFilename);

        logger.info("Saving game to: {}", savePath);

        // Serialize game state
        JsonObj gameState = serializeGame(game);

        // Store map filename in game state
        JsonObj boardInfo = gameState.getObj("board");
        boardInfo.put("mapFile", mapFilename);

        String json = JsonFormatter.format(gameState);

        // Write JSON to file
        try (FileWriter writer = new FileWriter(savePath.toFile())) {
            writer.write(json);
        }

        // Save map file alongside game state
        game.getBoard().map.saveMap(mapPath.toString());

        logger.info("Game saved successfully to: {}", savePath);
    }

    /**
     * Loads game state from a JSON file.
     *
     * @param saveFile the save file to load
     * @param ctx the hex board context for reconstruction
     * @return the reconstructed game
     * @throws IOException if file operations fail
     */
    public Game loadGame(File saveFile, HexBoardContext ctx)
        throws IOException, com.developingstorm.exceptions.InvalidMapException {
        logger.info("Loading game from: {}", saveFile);

        // Read JSON file
        String json = new String(Files.readAllBytes(saveFile.toPath()));
        JsonObj root = (JsonObj) JsonParser.parse(json);

        // Validate version and check for backward compatibility
        String version = root.getString("version");
        boolean isOldVersion = "1.0".equals(version);
        boolean isVersion11 = "1.1".equals(version);
        if (!SAVE_VERSION.equals(version) && !isOldVersion && !isVersion11) {
            logger.warn(
                "Save file version mismatch: expected {}, got {}",
                SAVE_VERSION,
                version
            );
        }
        if (isOldVersion) {
            logger.info(
                "Loading old save file format (v1.0), applying compatibility fixes"
            );
        }
        if (isVersion11) {
            logger.info("Loading save file format v1.1 (no explored areas)");
        }

        // Extract saved data
        int savedTurn = root.getInteger("turn");
        int currentPlayerIndex = root.getInteger("currentPlayerIndex");

        // Load map file
        String mapFileName = saveFile.getName().replace(".json", "_map.txt");
        File mapFile = new File(saveFile.getParent(), mapFileName);
        if (!mapFile.exists()) {
            throw new GameException(
                "Map file not found: " + mapFile.getAbsolutePath()
            );
        }

        com.developingstorm.games.hexboard.HexBoardMap gridMap =
            com.developingstorm.games.hexboard.HexBoardMap.loadMap(
                mapFile.getAbsolutePath()
            );

        // Deserialize players
        Object[] playersArray = root.getArray("players");
        Player[] players = new Player[playersArray.length];
        for (int i = 0; i < playersArray.length; i++) {
            JsonObj playerJson = (JsonObj) playersArray[i];
            players[i] = deserializePlayer(playerJson);
        }

        // Create Game object
        Game game = new Game(players, gridMap, ctx);

        // Restore turn number
        game.turn = savedTurn;

        // Restore explored areas (v1.2+)
        if (!isOldVersion && !isVersion11) {
            for (int i = 0; i < playersArray.length; i++) {
                JsonObj playerJson = (JsonObj) playersArray[i];
                Object[] exploredArray = playerJson.getArray("explored");
                if (exploredArray != null) {
                    Player player = players[i];
                    for (Object exploredObj : exploredArray) {
                        JsonObj exploredLoc = (JsonObj) exploredObj;
                        int x = exploredLoc.getInteger("x");
                        int y = exploredLoc.getInteger("y");
                        Location loc = Location.get(x, y);
                        player.markExploredDirect(loc);
                    }
                }
            }
        } else {
            // For old saves (v1.0, v1.1), recalculate visibility from units and cities
            logger.info("Recalculating visibility for old save format");
            for (Player player : players) {
                // Adjust visibility for all owned cities
                for (City city : player.getCities()) {
                    player.adjustVisibility(city);
                }
            }
        }

        // Deserialize units
        Object[] unitsArray = root.getArray("units");
        java.util.Map<Long, Unit> unitMap = new java.util.HashMap<>();

        for (int i = 0; i < unitsArray.length; i++) {
            JsonObj unitJson = (JsonObj) unitsArray[i];
            UnitSerializer.UnitData unitData = UnitSerializer.deserializeUnit(
                unitJson
            );

            // Find owner by player ID (not array index)
            Player owner = null;
            for (Player p : players) {
                if (p.getId() == unitData.ownerIndex) {
                    owner = p;
                    break;
                }
            }
            if (owner == null) {
                throw new GameException(
                    "Could not find player with ID " + unitData.ownerIndex
                );
            }

            // Get type
            com.developingstorm.games.sad.Type type =
                com.developingstorm.games.sad.Type.get(unitData.typeName);

            // Get location from x,y coordinates
            Location loc = Location.get(unitData.x, unitData.y);

            // Create unit using Game's factory method
            Unit unit = game.createUnit(type, owner, loc);

            // Register unit with its owner player
            owner.addUnit(unit);

            // Restore unit state
            unit.life.hits = unitData.hits;
            unit.dist = unitData.dist;

            unitMap.put(unitData.id, unit);
        }

        // Resolve carrying relationships and orders
        for (int i = 0; i < unitsArray.length; i++) {
            JsonObj unitJson = (JsonObj) unitsArray[i];
            UnitSerializer.UnitData unitData = UnitSerializer.deserializeUnit(
                unitJson
            );

            Unit unit = unitMap.get(unitData.id);

            if (unitData.carryingIds != null) {
                for (Long carriedId : unitData.carryingIds) {
                    Unit carried = unitMap.get(carriedId);
                    if (carried != null) {
                        // Don't assign orders when loading - they'll be restored separately
                        unit.addCarried(carried, false);
                    }
                }
            }

            // Restore unit order
            if (unitData.orderType != null) {
                Order order = reconstructOrder(
                    game,
                    unit,
                    unitData.orderType,
                    unitData.orderData
                );
                if (order != null) {
                    unit.assignOrder(order);
                }
            }
        }

        // Deserialize cities
        // Note: Game constructor already initialized board with random cities
        // We need to clear those and replace with saved cities
        java.util.List<City> existingCities = game.getBoard().getCities();
        existingCities.clear();

        Object[] citiesArray = root.getArray("cities");
        for (int i = 0; i < citiesArray.length; i++) {
            JsonObj cityJson = (JsonObj) citiesArray[i];
            City city = new City(game, cityJson);
            existingCities.add(city);
        }

        // Rebuild the city location lookup HashMap so isCity() works correctly
        game.getBoard().rebuildCityLookup();

        // Rebuild player cities lists - clear old cities and add loaded ones
        for (Player p : players) {
            p.getCities().clear();
        }
        for (City city : existingCities) {
            Player cityOwner = city.getOwner();
            if (cityOwner != null) {
                cityOwner.getCities().add(city);
            }
        }

        // Set current player
        game.currentPlayer = players[currentPlayerIndex];

        // Second pass: restore edicts now that all cities are loaded
        for (City city : existingCities) {
            city.restoreEdictsSecondPass();
        }

        logger.info("Game loaded successfully: turn {}", savedTurn);
        return game;
    }

    /**
     * Deserializes a single player.
     */
    private Player deserializePlayer(JsonObj json) {
        int id = json.getInteger("id");
        String name = json.getString("name");
        Boolean isHumanObj = json.getBoolean("isHuman");
        boolean isHuman = (isHumanObj != null) ? isHumanObj : true;

        logger.debug(
            "Deserializing player: name={}, id={}, isHuman={}",
            name,
            id,
            isHuman
        );

        Player player;
        if (isHuman) {
            player = new Player(name, id);
        } else {
            player = new com.developingstorm.games.sad.Robot(name, id);
        }

        logger.debug(
            "Created player instance: class={}",
            player.getClass().getName()
        );
        return player;
    }

    /**
     * Lists all available save files in the save directory.
     *
     * @return list of save file paths
     */
    public List<File> listSaveFiles() {
        Path saveDir = Paths.get(SAVE_DIR);

        List<File> saveFiles = new ArrayList<>();

        if (Files.exists(saveDir) && Files.isDirectory(saveDir)) {
            File dir = saveDir.toFile();
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    saveFiles.add(file);
                }
            }
        }

        return saveFiles;
    }

    /**
     * Gets the recommended save directory path.
     */
    public static String getSaveDirectory() {
        return Paths.get(SAVE_DIR).toAbsolutePath().toString();
    }

    /**
     * Reconstructs an order from its serialized data.
     *
     * @param game the game instance
     * @param unit the unit to assign the order to
     * @param orderTypeName the order type name
     * @param orderData the order-specific data (may be null)
     * @return the reconstructed order, or null if reconstruction fails
     */
    private Order reconstructOrder(
        Game game,
        Unit unit,
        String orderTypeName,
        JsonObj orderData
    ) {
        try {
            OrderType orderType = OrderType.valueOf(orderTypeName);

            switch (orderType) {
                case MOVE:
                    if (
                        orderData != null &&
                        orderData.getObj("destination") != null
                    ) {
                        JsonObj destJson = orderData.getObj("destination");
                        int x = destJson.getInteger("x");
                        int y = destJson.getInteger("y");
                        Location dest = Location.get(x, y);
                        return new Move(game, unit, dest);
                    }
                    logger.warn(
                        "Move order missing destination data for unit {}",
                        unit.id
                    );
                    return null;
                case HEAD_HOME:
                    return new HeadHome(game, unit);
                case EXPLORE:
                    return new Explore(game, unit);
                case SENTRY:
                    return new Sentry(game, unit);
                case SKIPTURN:
                    return new SkipTurn(game, unit);
                case UNLOAD:
                    return new Unload(game, unit);
                case PATROL:
                    if (
                        orderData != null &&
                        orderData.getArray("waypoints") != null
                    ) {
                        Object[] waypointsArray = orderData.getArray(
                            "waypoints"
                        );
                        List<Location> waypoints = new ArrayList<>();
                        for (Object wpObj : waypointsArray) {
                            JsonObj wpJson = (JsonObj) wpObj;
                            int x = wpJson.getInteger("x");
                            int y = wpJson.getInteger("y");
                            waypoints.add(Location.get(x, y));
                        }

                        String modeStr = orderData.getString("mode");
                        Patrol.PatrolMode mode = Patrol.PatrolMode.valueOf(
                            modeStr
                        );

                        Patrol patrol = new Patrol(game, unit, waypoints, mode);

                        // Restore patrol state using reflection
                        try {
                            java.lang.reflect.Field currentWaypointField =
                                Patrol.class.getDeclaredField(
                                    "currentWaypointIndex"
                                );
                            currentWaypointField.setAccessible(true);
                            currentWaypointField.setInt(
                                patrol,
                                orderData.getInteger("currentWaypointIndex")
                            );

                            java.lang.reflect.Field reverseField =
                                Patrol.class.getDeclaredField(
                                    "reverseDirection"
                                );
                            reverseField.setAccessible(true);
                            reverseField.setBoolean(
                                patrol,
                                orderData.getBoolean("reverseDirection")
                            );
                        } catch (Exception e) {
                            logger.warn(
                                "Failed to restore patrol state for unit {}: {}",
                                unit.id,
                                e.getMessage()
                            );
                        }

                        return patrol;
                    }
                    logger.warn(
                        "Patrol order missing waypoints data for unit {}",
                        unit.id
                    );
                    return null;
                case ATTACK:
                    if (orderData != null) {
                        Integer targetX = orderData.getInteger("targetX");
                        Integer targetY = orderData.getInteger("targetY");
                        if (targetX != null && targetY != null) {
                            Location targetLoc = Location.get(targetX, targetY);
                            return new com.developingstorm.games.sad.orders.Attack(
                                game,
                                unit,
                                targetLoc
                            );
                        }
                    }
                    logger.warn(
                        "Attack order missing target data for unit {}",
                        unit.id
                    );
                    return null;
                case DISBAND:
                    // Disband has protected constructor - skip restoration
                    // Unit will simply not have an order when loaded
                    logger.debug(
                        "Skipping Disband order restoration for unit {}",
                        unit.id
                    );
                    return null;
                case MOVE_NORTH_EAST:
                    return new DirectionalMove(
                        game,
                        unit,
                        OrderType.MOVE_NORTH_EAST,
                        Direction.NORTH_EAST
                    );
                case MOVE_NORTH_WEST:
                    return new DirectionalMove(
                        game,
                        unit,
                        OrderType.MOVE_NORTH_WEST,
                        Direction.NORTH_WEST
                    );
                case MOVE_EAST:
                    return new DirectionalMove(
                        game,
                        unit,
                        OrderType.MOVE_EAST,
                        Direction.EAST
                    );
                case MOVE_WEST:
                    return new DirectionalMove(
                        game,
                        unit,
                        OrderType.MOVE_WEST,
                        Direction.WEST
                    );
                case MOVE_SOUTH_EAST:
                    return new DirectionalMove(
                        game,
                        unit,
                        OrderType.MOVE_SOUTH_EAST,
                        Direction.SOUTH_EAST
                    );
                case MOVE_SOUTH_WEST:
                    return new DirectionalMove(
                        game,
                        unit,
                        OrderType.MOVE_SOUTH_WEST,
                        Direction.SOUTH_WEST
                    );
                case NONE:
                default:
                    logger.debug(
                        "Skipping order reconstruction for type: {}",
                        orderType
                    );
                    return null;
            }
        } catch (Exception e) {
            logger.error(
                "Failed to reconstruct order {} for unit {}: {}",
                orderTypeName,
                unit.id,
                e.getMessage()
            );
            return null;
        }
    }
}
