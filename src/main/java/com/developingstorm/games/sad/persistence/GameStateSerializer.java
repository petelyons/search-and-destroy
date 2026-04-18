package com.developingstorm.games.sad.persistence;

import com.developingstorm.games.hexboard.Direction;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Fleet;
import com.developingstorm.games.sad.FleetRegistry;
import com.developingstorm.games.sad.FleetRole;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
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
        // Save array index (not player ID) so we can restore correctly
        int currentPlayerIdx = 0;
        Player[] allPlayers = game.getPlayers();
        for (int i = 0; i < allPlayers.length; i++) {
            if (allPlayers[i] == game.currentPlayer()) {
                currentPlayerIdx = i;
                break;
            }
        }
        root.put("currentPlayerIndex", currentPlayerIdx);
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

        // Continents (save names so they persist across saves)
        java.util.Set<com.developingstorm.games.sad.Continent> allContinents =
            game.getBoard().getContinents();
        Object[] continentsArray = new Object[allContinents.size()];
        int contIndex = 0;
        for (com.developingstorm.games.sad.Continent continent : allContinents) {
            continentsArray[contIndex++] = continent.toJson();
        }
        root.put("continents", continentsArray);

        // Fleets
        Collection<Fleet> allFleets = game.getFleetRegistry().allFleets();
        Object[] fleetsArray = new Object[allFleets.size()];
        int fIdx = 0;
        for (Fleet f : allFleets) {
            JsonObj jf = new JsonObj();
            jf.put("id", f.getId());
            jf.put("ownerId", (long) f.getOwner().getId());
            jf.put("flagshipId", f.getFlagship().id);
            Map<Unit, FleetRole> mv = f.membersView();
            Object[] membersArr = new Object[mv.size()];
            int mIdx = 0;
            for (Map.Entry<Unit, FleetRole> e : mv.entrySet()) {
                JsonObj jm = new JsonObj();
                jm.put("unitId", e.getKey().id);
                membersArr[mIdx++] = jm;
            }
            jf.put("members", membersArr);
            fleetsArray[fIdx++] = jf;
        }
        root.put("fleets", fleetsArray);

        logger.info(
            "Game state serialized: {} players, {} units, {} cities, {} continents",
            players.length,
            allUnits.size(),
            allCities.size(),
            allContinents.size()
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
     * Saves game state to a ZIP archive file.
     *
     * @param game the game to save
     * @param saveName the name for this save (without extension)
     * @throws IOException if file operations fail
     */
    public void saveGame(Game game, String saveName) throws IOException {
        Path saveDir = Paths.get(SAVE_DIR);
        Files.createDirectories(saveDir);

        SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd_HH-mm-ss"
        );
        String timestamp = dateFormat.format(new Date());
        String filename = saveName + "_" + timestamp + ".sav";
        Path savePath = saveDir.resolve(filename);

        saveGameToFile(game, savePath.toFile(), saveName);
    }

    public void saveGameToFile(Game game, File outFile) throws IOException {
        String base = outFile.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        saveGameToFile(game, outFile, base);
    }

    private void saveGameToFile(Game game, File outFile, String saveName)
        throws IOException {
        logger.info("Saving game to: {}", outFile);

        JsonObj gameState = serializeGame(game);
        JsonObj boardInfo = gameState.getObj("board");
        boardInfo.put("mapFile", "map.txt");
        boardInfo.put("saveName", saveName);

        String json = JsonFormatter.format(gameState);

        try (
            FileOutputStream fos = new FileOutputStream(outFile);
            ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            ZipEntry gameEntry = new ZipEntry("game.json");
            zos.putNextEntry(gameEntry);
            zos.write(json.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            ZipEntry mapEntry = new ZipEntry("map.txt");
            zos.putNextEntry(mapEntry);

            Path tempMapFile = Files.createTempFile("map", ".txt");
            try {
                game.getBoard().map.saveMap(tempMapFile.toString());
                byte[] mapData = Files.readAllBytes(tempMapFile);
                zos.write(mapData);
            } finally {
                Files.deleteIfExists(tempMapFile);
            }

            zos.closeEntry();
        }

        logger.info("Game saved successfully to: {}", outFile);
    }

    /**
     * Loads game state from a ZIP archive or legacy JSON file.
     *
     * @param saveFile the save file to load
     * @param ctx the hex board context for reconstruction
     * @return the reconstructed game
     * @throws IOException if file operations fail
     */
    public Game loadGame(File saveFile, HexBoardContext ctx)
        throws IOException, com.developingstorm.exceptions.InvalidMapException {
        logger.info("Loading game from: {}", saveFile);

        // Check if this is a new ZIP format or legacy JSON format
        boolean isZipFormat = saveFile.getName().endsWith(".sav");

        String json = null;
        String mapData = null;

        if (isZipFormat) {
            // Load from ZIP archive
            try (
                FileInputStream fis = new FileInputStream(saveFile);
                ZipInputStream zis = new ZipInputStream(fis)
            ) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("game.json")) {
                        json = new String(
                            readAllBytes(zis),
                            StandardCharsets.UTF_8
                        );
                    } else if (entry.getName().equals("map.txt")) {
                        mapData = new String(
                            readAllBytes(zis),
                            StandardCharsets.UTF_8
                        );
                    }
                    zis.closeEntry();
                }

                if (json == null) {
                    throw new GameException("ZIP archive missing game.json");
                }
                if (mapData == null) {
                    throw new GameException("ZIP archive missing map.txt");
                }
            }
        } else {
            // Legacy format: Read JSON file
            json = new String(Files.readAllBytes(saveFile.toPath()));
            mapData = null; // Will load from separate map file
        }

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

        // Load map
        com.developingstorm.games.hexboard.HexBoardMap gridMap;
        if (isZipFormat) {
            // Map data already loaded from ZIP - load from string using StringReader
            gridMap = com.developingstorm.games.hexboard.HexBoardMap.loadMap(
                new StringReader(mapData)
            );
        } else {
            // Legacy format: Load map from separate file
            String mapFileName = saveFile
                .getName()
                .replace(".json", "_map.txt");
            File mapFile = new File(saveFile.getParent(), mapFileName);
            if (!mapFile.exists()) {
                throw new GameException(
                    "Map file not found: " + mapFile.getAbsolutePath()
                );
            }
            gridMap = com.developingstorm.games.hexboard.HexBoardMap.loadMap(
                mapFile.getAbsolutePath()
            );
        }

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
            logger.info("Restoring explored areas from save file (v1.2+)");
            for (int i = 0; i < playersArray.length; i++) {
                JsonObj playerJson = (JsonObj) playersArray[i];
                Object[] exploredArray = playerJson.getArray("explored");
                if (exploredArray != null) {
                    Player player = players[i];
                    logger.info(
                        "Restoring {} explored locations for player {}",
                        exploredArray.length,
                        player.getName()
                    );
                    for (Object exploredObj : exploredArray) {
                        JsonObj exploredLoc = (JsonObj) exploredObj;
                        int x = exploredLoc.getInteger("x");
                        int y = exploredLoc.getInteger("y");
                        Location loc = Location.get(x, y);
                        if (loc != null) {
                            player.markExploredDirect(loc);
                        } else {
                            logger.warn(
                                "Invalid location in save file: ({}, {})",
                                x,
                                y
                            );
                        }
                    }
                } else {
                    logger.warn(
                        "No explored data found for player {}",
                        players[i].getName()
                    );
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
            unit.setUnloadingMode(unitData.unloadingMode);

            // Restore production location
            unit.productionContinentName = unitData.productionContinentName;
            unit.productionCityName = unitData.productionCityName;

            unitMap.put(unitData.id, unit);
        }

        // Resolve carrying relationships (orders deferred until after fleets rebuild)
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
                        unit.addCarried(carried, false);
                    }
                }
            }
        }

        restoreFleets(game, root, unitMap);

        // Restore unit orders (needs fleets in place for FLEET_MEMBER)
        for (int i = 0; i < unitsArray.length; i++) {
            JsonObj unitJson = (JsonObj) unitsArray[i];
            UnitSerializer.UnitData unitData = UnitSerializer.deserializeUnit(
                unitJson
            );
            Unit unit = unitMap.get(unitData.id);
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

        // Restore continent names
        Object[] continentsArray = root.getArray("continents");
        if (continentsArray != null) {
            java.util.Set<
                com.developingstorm.games.sad.Continent
            > existingContinents = game.getBoard().getContinents();
            for (int i = 0; i < continentsArray.length; i++) {
                JsonObj continentJson = (JsonObj) continentsArray[i];
                int continentId = continentJson.getInteger("id");

                // Find the continent with this ID and restore its name
                for (com.developingstorm.games.sad.Continent continent : existingContinents) {
                    if (continent.getID() == continentId) {
                        continent.fromJson(continentJson);
                        break;
                    }
                }
            }
        }

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

        // Always resume with the human player (index 0).
        // The game is only saved during the human's turn (AWAITING_ORDERS),
        // and the human should always get control first after loading.
        game.currentPlayer = players[0];

        // Second pass: restore edicts now that all cities are loaded
        for (City city : existingCities) {
            city.restoreEdictsSecondPass();
        }

        // Recalculate visibility for all players based on loaded units and cities
        // This ensures fog of war is correct when the game loads
        logger.info("Recalculating visibility for all players after load");
        for (Player player : players) {
            // Clear any stale visibility data before recalculating
            player.clearVisibility();

            // Adjust visibility for all cities owned by this player
            for (City city : player.getCities()) {
                player.adjustVisibility(city);
            }

            // Adjust visibility for all units owned by this player
            for (Unit unit : player.getUnits()) {
                player.adjustVisibility(unit);
            }
        }

        // Signal that the game is resuming from a load so the human player's
        // first turn will always pause for input.
        game.setResumingFromLoad();

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
            // Include both new .sav files and legacy .json files
            File[] files = dir.listFiles(
                (d, name) -> name.endsWith(".sav") || name.endsWith(".json")
            );
            if (files != null) {
                for (File file : files) {
                    saveFiles.add(file);
                }
            }
        }

        return saveFiles;
    }

    /**
     * Helper method to read all bytes from a ZipInputStream.
     */
    private byte[] readAllBytes(ZipInputStream zis) throws IOException {
        java.io.ByteArrayOutputStream buffer =
            new java.io.ByteArrayOutputStream();
        byte[] temp = new byte[4096];
        int bytesRead;
        while ((bytesRead = zis.read(temp)) != -1) {
            buffer.write(temp, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    /**
     * Gets the recommended save directory path.
     */
    public static String getSaveDirectory() {
        return Paths.get(SAVE_DIR).toAbsolutePath().toString();
    }

    /**
     * Extracts the base save name from a save file.
     * For new format (.sav): reads from game state
     * For legacy format: parses from filename
     *
     * @param saveFile the save file
     * @return the base save name (without timestamp)
     */
    public String extractSaveName(File saveFile) {
        try {
            String fileName = saveFile.getName();
            boolean isZipFormat = fileName.endsWith(".sav");

            if (isZipFormat) {
                // Read from ZIP archive
                try (
                    FileInputStream fis = new FileInputStream(saveFile);
                    ZipInputStream zis = new ZipInputStream(fis)
                ) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.getName().equals("game.json")) {
                            String json = new String(
                                readAllBytes(zis),
                                StandardCharsets.UTF_8
                            );
                            JsonObj root = (JsonObj) JsonParser.parse(json);
                            JsonObj boardInfo = root.getObj("board");
                            String saveName = boardInfo.getString("saveName");
                            if (saveName != null) {
                                return saveName;
                            }
                            break;
                        }
                        zis.closeEntry();
                    }
                }
            }

            // Fallback: extract from filename
            return extractGameNameFromFilename(fileName);
        } catch (Exception e) {
            logger.warn(
                "Failed to extract save name from {}: {}",
                saveFile.getName(),
                e.getMessage()
            );
            // Last resort: use filename without extension
            String name = saveFile.getName();
            int dotIndex = name.lastIndexOf('.');
            return dotIndex > 0 ? name.substring(0, dotIndex) : name;
        }
    }

    /**
     * Extracts game name from filename by removing timestamp suffix.
     */
    private String extractGameNameFromFilename(String filename) {
        // Remove extension
        filename = filename.replace(".sav", "").replace(".json", "");

        // Remove timestamp pattern: _yyyy-MM-dd_HH-mm-ss
        int lastUnderscore = filename.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String potentialTimestamp = filename.substring(lastUnderscore + 1);
            // Check if it looks like HH-mm-ss
            if (potentialTimestamp.matches("\\d{2}-\\d{2}-\\d{2}")) {
                filename = filename.substring(0, lastUnderscore);
                // Remove date part too
                lastUnderscore = filename.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String potentialDate = filename.substring(
                        lastUnderscore + 1
                    );
                    if (potentialDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        filename = filename.substring(0, lastUnderscore);
                    }
                }
            }
        }

        return filename;
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
                case ESCORT:
                    if (orderData != null) {
                        Long escortedUnitId = orderData.getLong(
                            "escortedUnitId"
                        );
                        if (escortedUnitId != null) {
                            // Find the escorted unit by ID
                            Unit escortedUnit = null;
                            for (Player p : game.getPlayers()) {
                                for (Unit u : p.getUnits()) {
                                    if (u.id == escortedUnitId) {
                                        escortedUnit = u;
                                        break;
                                    }
                                }
                                if (escortedUnit != null) break;
                            }

                            if (escortedUnit != null) {
                                return new com.developingstorm.games.sad.orders.Escort(
                                    game,
                                    unit,
                                    escortedUnit
                                );
                            } else {
                                logger.warn(
                                    "Escort order: escorted unit {} not found for unit {}",
                                    escortedUnitId,
                                    unit.id
                                );
                            }
                        }
                    }
                    logger.warn(
                        "Escort order missing escorted unit data for unit {}",
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
                case FLEET_MEMBER:
                    if (orderData != null) {
                        Long fleetId = orderData.getLong("fleetId");
                        if (fleetId != null) {
                            Fleet f = game.getFleetRegistry().byId(fleetId);
                            if (f != null && f.contains(unit)) {
                                return new com.developingstorm.games.sad.orders.FleetMember(
                                    game,
                                    unit,
                                    f
                                );
                            }
                        }
                    }
                    logger.warn(
                        "FleetMember order missing or invalid fleetId for unit {}",
                        unit.id
                    );
                    return null;
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

    private void restoreFleets(
        Game game,
        JsonObj root,
        java.util.Map<Long, Unit> unitMap
    ) {
        Object[] fleetsArr = root.getArray("fleets");
        if (fleetsArr == null || fleetsArr.length == 0) return;

        FleetRegistry reg = game.getFleetRegistry();
        long maxId = 0L;
        for (Object o : fleetsArr) {
            JsonObj jf = (JsonObj) o;
            Long fleetId = jf.getLong("id");
            Long flagshipId = jf.getLong("flagshipId");
            if (fleetId == null || flagshipId == null) continue;
            if (reg.byId(fleetId) != null) {
                logger.warn(
                    "Duplicate fleet id {} in save file, skipping",
                    fleetId
                );
                continue;
            }
            Unit flagship = unitMap.get(flagshipId);
            if (flagship == null) {
                logger.warn(
                    "Fleet {} references missing flagship unit {}",
                    fleetId,
                    flagshipId
                );
                continue;
            }

            Fleet f = reg.form(flagship, java.util.Collections.emptyList());
            long assignedId = f.getId();
            overrideFleetId(f, fleetId);
            rekeyFleetInRegistry(reg, assignedId, fleetId);
            if (fleetId > maxId) maxId = fleetId;

            Object[] members = jf.getArray("members");
            if (members != null) {
                for (Object mObj : members) {
                    JsonObj jm = (JsonObj) mObj;
                    Long unitId = jm.getLong("unitId");
                    if (unitId == null || unitId == flagshipId) continue;
                    Unit m = unitMap.get(unitId);
                    if (m != null) {
                        reg.add(f, m);
                    } else {
                        logger.warn(
                            "Fleet {} references missing member unit {}",
                            fleetId,
                            unitId
                        );
                    }
                }
            }
        }
        bumpFleetRegistryNextId(reg, maxId + 1L);
    }

    private static void overrideFleetId(Fleet f, long id) {
        try {
            java.lang.reflect.Field fld = Fleet.class.getDeclaredField("id");
            fld.setAccessible(true);
            fld.setLong(f, id);
        } catch (Exception ex) {
            throw new GameException(
                "failed to override fleet id: " + ex.getMessage()
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static void rekeyFleetInRegistry(
        FleetRegistry reg,
        long oldId,
        long newId
    ) {
        if (oldId == newId) return;
        try {
            java.lang.reflect.Field fld =
                FleetRegistry.class.getDeclaredField("byId");
            fld.setAccessible(true);
            java.util.Map<Long, Fleet> byId =
                (java.util.Map<Long, Fleet>) fld.get(reg);
            Fleet f = byId.remove(oldId);
            if (f != null) byId.put(newId, f);
        } catch (Exception ex) {
            throw new GameException(
                "failed to rekey fleet registry: " + ex.getMessage()
            );
        }
    }

    private static void bumpFleetRegistryNextId(FleetRegistry reg, long next) {
        try {
            java.lang.reflect.Field fld =
                FleetRegistry.class.getDeclaredField("nextId");
            fld.setAccessible(true);
            long current = fld.getLong(reg);
            if (next > current) {
                fld.setLong(reg, next);
            }
        } catch (Exception ex) {
            throw new GameException(
                "failed to bump fleet registry nextId: " + ex.getMessage()
            );
        }
    }
}
