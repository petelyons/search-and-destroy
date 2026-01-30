package com.developingstorm.games.sad.testing;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.sad.brain.AIConfiguration;
import com.developingstorm.games.sad.brain.AIConfigurationLoader;
import java.io.File;
import java.util.List;

/**
 * Command-line interface for AI testing and evaluation.
 *
 * Usage:
 *   java AITestCLI list
 *   java AITestCLI single <config1> <config2>
 *   java AITestCLI ab-test <baseline> <variant> <games>
 *   java AITestCLI create-baseline <name>
 *
 * Examples:
 *   java AITestCLI list
 *   java AITestCLI single baseline/v1.0.json profiles/easy.json
 *   java AITestCLI ab-test baseline/v1.0.json experiments/aggressive_v1.json 50
 */
public class AITestCLI {

    private static final String DEFAULT_MAP = "MedMap";
    private static final int DEFAULT_TURN_LIMIT = 200;
    private static final int DEFAULT_PARALLEL_GAMES = 4;

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        try {
            String command = args[0];

            switch (command) {
                case "list":
                    handleList();
                    break;
                case "single":
                    if (args.length < 3) {
                        System.err.println(
                            "Usage: single <config1> <config2> [--turn-limit <n>]"
                        );
                        System.exit(1);
                    }
                    // Parse optional arguments
                    int turnLimit = DEFAULT_TURN_LIMIT;
                    for (int i = 3; i < args.length; i++) {
                        if (
                            args[i].equals("--turn-limit") &&
                            i + 1 < args.length
                        ) {
                            turnLimit = Integer.parseInt(args[i + 1]);
                            i++; // skip the next arg
                        }
                    }
                    handleSingle(args[1], args[2], turnLimit);
                    break;
                case "ab-test":
                    if (args.length < 4) {
                        System.err.println(
                            "Usage: ab-test <baseline> <variant> <games>"
                        );
                        System.exit(1);
                    }
                    handleABTest(args[1], args[2], Integer.parseInt(args[3]));
                    break;
                case "create-baseline":
                    if (args.length < 2) {
                        System.err.println("Usage: create-baseline <name>");
                        System.exit(1);
                    }
                    handleCreateBaseline(args[1]);
                    break;
                case "help":
                case "--help":
                case "-h":
                    printUsage();
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * List all available configurations.
     */
    private static void handleList() {
        System.out.println("Available AI Configurations:");
        System.out.println("===========================");

        List<String> configs = AIConfigurationLoader.listConfigurations();

        if (configs.isEmpty()) {
            System.out.println("(none found)");
        } else {
            for (String config : configs) {
                System.out.println("  " + config);
            }
        }

        System.out.println("\nTotal: " + configs.size() + " configurations");
    }

    /**
     * Run a single game between two configurations.
     */
    private static void handleSingle(
        String config1Path,
        String config2Path,
        int turnLimit
    ) throws Exception {
        System.out.println("Running single game...");
        System.out.println("  Config 1: " + config1Path);
        System.out.println("  Config 2: " + config2Path);

        // Load configurations
        AIConfiguration config1 = AIConfigurationLoader.loadByName(config1Path);
        AIConfiguration config2 = AIConfigurationLoader.loadByName(config2Path);

        // Create map and context
        HexBoardMap map = loadMap(DEFAULT_MAP);
        HexBoardContext ctx = createContext(map);

        // Run game
        HeadlessGameRunner runner = new HeadlessGameRunner(
            map,
            ctx,
            turnLimit,
            config1,
            config2
        );

        System.out.println("\nGame starting...\n");
        HeadlessGameRunner.GameResult result = runner.run();

        // Display result
        System.out.println("\n" + result);

        if (result.getWinnerPlayerNumber() != null) {
            String winnerConfig =
                result.getWinnerPlayerNumber() == 1
                    ? config1.getName()
                    : config2.getName();
            System.out.println("Winner: " + winnerConfig);
        }
    }

    /**
     * Run an A/B test.
     */
    private static void handleABTest(
        String baselinePath,
        String variantPath,
        int numGames
    ) throws Exception {
        // Create map and context
        HexBoardMap map = loadMap(DEFAULT_MAP);
        HexBoardContext ctx = createContext(map);

        // Create tournament manager
        TournamentManager manager = new TournamentManager(
            map,
            ctx,
            DEFAULT_TURN_LIMIT,
            DEFAULT_PARALLEL_GAMES
        );

        // Run tournament
        TournamentManager.TournamentResult result = manager.runABTest(
            baselinePath,
            variantPath,
            numGames
        );

        // Display results
        System.out.println(result.generateReport());
    }

    /**
     * Create a new baseline configuration.
     */
    private static void handleCreateBaseline(String name) throws Exception {
        System.out.println("Creating baseline configuration: " + name);

        // Create configuration with default values
        AIConfiguration config = new AIConfiguration();
        config.setName(name);
        config.setVersion("1.0.0");
        config.setDescription("Baseline configuration created via CLI");

        // Save to baseline directory
        String path = "ai-configs/baseline/" + name + ".json";
        AIConfigurationLoader.saveConfiguration(config, path);

        System.out.println("Configuration saved to: " + path);
    }

    /**
     * Load a map by name.
     */
    private static HexBoardMap loadMap(String mapName) throws Exception {
        // Try to load from resources
        try {
            return HexBoardMap.loadMapAsResource(
                AITestCLI.class,
                mapName + ".sdm"
            );
        } catch (Exception e) {
            // If that fails, create a simple default test map
            System.out.println(
                "Warning: Could not load map '" +
                    mapName +
                    "', using default test map"
            );
            HexBoardMap map = new HexBoardMap(32, 28);

            // Initialize with a simple pattern
            int[][] data = map.getData();
            for (int x = 0; x < 32; x++) {
                for (int y = 0; y < 28; y++) {
                    // Create some land masses
                    if (x < 8 || x >= 24 || y < 6 || y >= 22) {
                        data[x][y] = 0; // water
                    } else {
                        data[x][y] = 1; // land
                    }
                }
            }

            return map;
        }
    }

    /**
     * Create a hex board context.
     */
    private static HexBoardContext createContext(HexBoardMap map) {
        return new HexBoardContext() {
            @Override
            public int getPrototypeHex() {
                return 0;
            }

            @Override
            public java.awt.Image[] getImages() {
                return new java.awt.Image[0];
            }

            @Override
            public int getHexSide() {
                return 30;
            }

            @Override
            public boolean showBorder() {
                return true;
            }

            @Override
            public java.awt.Color getBorderColor() {
                return java.awt.Color.GRAY;
            }

            @Override
            public java.awt.Color getSelectionColor() {
                return java.awt.Color.YELLOW;
            }

            @Override
            public java.awt.Color getXorColor() {
                return java.awt.Color.WHITE;
            }

            @Override
            public int getZs() {
                return 0;
            }

            @Override
            public int getWidth() {
                return map.getWidth();
            }

            @Override
            public int getHeight() {
                return map.getHeight();
            }

            @Override
            public int getTerrainImageSelector(int x, int y) {
                return 0;
            }

            @Override
            public int getUnexploredImageSelector() {
                return 0;
            }
        };
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("AI Testing CLI");
        System.out.println("==============");
        System.out.println();
        System.out.println("Usage: java AITestCLI <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  list");
        System.out.println("      List all available AI configurations");
        System.out.println();
        System.out.println("  single <config1> <config2>");
        System.out.println(
            "      Run a single game between two configurations"
        );
        System.out.println("      Example: single baseline/v1.0 profiles/easy");
        System.out.println();
        System.out.println("  ab-test <baseline> <variant> <games>");
        System.out.println("      Run A/B test comparing two configurations");
        System.out.println(
            "      Example: ab-test baseline/v1.0 experiments/aggressive_v1 50"
        );
        System.out.println();
        System.out.println("  create-baseline <name>");
        System.out.println("      Create a new baseline configuration");
        System.out.println("      Example: create-baseline v2.0");
        System.out.println();
        System.out.println("  help");
        System.out.println("      Show this help message");
        System.out.println();
    }
}
