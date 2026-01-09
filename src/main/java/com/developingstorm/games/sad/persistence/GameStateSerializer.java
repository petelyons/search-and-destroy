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
import java.util.ArrayList;
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

    private static final String SAVE_VERSION = "1.0";
    private static final String SAVE_DIR = ".searchanddestroy/saves";

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
            playersArray[i] = serializePlayer(player, i);
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
    private JsonObj serializePlayer(Player player, int index) {
        JsonObj json = new JsonObj();
        json.put("index", index);
        json.put("name", player.toString());
        json.put("isHuman", !player.isRobot());
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
        // Create save directory if it doesn't exist
        String homeDir = System.getProperty("user.home");
        Path saveDir = Paths.get(homeDir, SAVE_DIR);
        Files.createDirectories(saveDir);

        // Generate filename with timestamp
        String filename = saveName + "_" + System.currentTimeMillis() + ".json";
        Path savePath = saveDir.resolve(filename);

        logger.info("Saving game to: {}", savePath);

        // Serialize game state
        JsonObj gameState = serializeGame(game);
        String json = JsonFormatter.format(gameState);

        // Write to file
        try (FileWriter writer = new FileWriter(savePath.toFile())) {
            writer.write(json);
        }

        // Save map file alongside game state
        String mapFilename =
            saveName + "_" + System.currentTimeMillis() + "_map.txt";
        Path mapPath = saveDir.resolve(mapFilename);
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

        // Validate version
        String version = root.getString("version");
        if (!SAVE_VERSION.equals(version)) {
            logger.warn(
                "Save file version mismatch: expected {}, got {}",
                SAVE_VERSION,
                version
            );
        }

        // Extract saved data
        int savedTurn = root.getInteger("turn");
        int currentPlayerIndex = root.getInteger("currentPlayerIndex");

        // Load map file
        String mapFileName = saveFile
            .getName()
            .replace(".json", "this.map.txt");
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

        // Deserialize units
        Object[] unitsArray = root.getArray("units");
        java.util.Map<Long, Unit> unitMap = new java.util.HashMap<>();

        for (int i = 0; i < unitsArray.length; i++) {
            JsonObj unitJson = (JsonObj) unitsArray[i];
            UnitSerializer.UnitData unitData = UnitSerializer.deserializeUnit(
                unitJson
            );

            // Find owner
            Player owner = players[unitData.ownerIndex];

            // Get type
            com.developingstorm.games.sad.Type type =
                com.developingstorm.games.sad.Type.get(unitData.typeName);

            // Get location from x,y coordinates
            Location loc = Location.get(unitData.x, unitData.y);

            // Create unit using Game's factory method
            Unit unit = game.createUnit(type, owner, loc);

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
                        unit.addCarried(carried);
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

        // Set current player
        game.currentPlayer = players[currentPlayerIndex];

        logger.info("Game loaded successfully: turn {}", savedTurn);
        return game;
    }

    /**
     * Deserializes a single player.
     */
    private Player deserializePlayer(JsonObj json) {
        int index = json.getInteger("index");
        String name = json.getString("name");
        boolean isHuman = json.getBoolean("isHuman");

        Player player;
        if (isHuman) {
            player = new Player(name, index);
        } else {
            player = new com.developingstorm.games.sad.Robot(name, index);
        }

        return player;
    }

    /**
     * Lists all available save files in the save directory.
     *
     * @return list of save file paths
     */
    public List<File> listSaveFiles() {
        String homeDir = System.getProperty("user.home");
        Path saveDir = Paths.get(homeDir, SAVE_DIR);

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
        String homeDir = System.getProperty("user.home");
        return Paths.get(homeDir, SAVE_DIR).toString();
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
