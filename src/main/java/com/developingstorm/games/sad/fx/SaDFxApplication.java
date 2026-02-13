package com.developingstorm.games.sad.fx;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.ShipNames;
import com.developingstorm.games.sad.UnitNames;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameControllerImpl;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.controller.GameQueryServiceImpl;
import com.developingstorm.games.sad.persistence.GameStateSerializer;
import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX Application for Search and Destroy.
 *
 * This is the new UI implementation using JavaFX, running alongside
 * the existing Swing UI. Both UIs share the same Game instance and
 * use the event bus architecture for communication.
 */
public class SaDFxApplication extends Application {

    private Game game;
    private GameController controller;
    private GameQueryService queryService;
    private HexBoardContext ctx;
    private HexBoardMap map;
    private Thread gameThread;
    private Stage primaryStage;
    private GameView gameView;
    private String currentSaveName; // Track current save name for quick save

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Enable macOS native menu bar
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty(
            "com.apple.mrj.application.apple.menu.about.name",
            "Search and Destroy"
        );

        // Initialize the game
        initializeGame();

        // Create controllers
        controller = new GameControllerImpl(game);
        queryService = new GameQueryServiceImpl(game);

        // Create lifecycle handler
        GameLifecycleHandler lifecycleHandler = createLifecycleHandler();

        // Create the main view
        gameView = new GameView(
            game,
            controller,
            queryService,
            lifecycleHandler
        );

        // Set up the scene
        Scene scene = new Scene(gameView, 1200, 800);

        // Use system menu bar on macOS
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            gameView.useSystemMenuBarProperty().set(true);
        }

        // Configure stage
        primaryStage.setTitle("Search and Destroy (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the game thread
        startGameThread();
    }

    /**
     * Initialize a new game with default settings.
     */
    private void initializeGame() {
        // Reset naming pools
        ShipNames.reset();
        UnitNames.reset();

        // Reset save name for new game
        this.currentSaveName = null;

        // Load map
        this.map = HexBoardMap.loadMapAsResource(this, "MedMap.sdm");
        this.ctx = new HexBoardContext() {
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

        // Create players
        Player[] players = new Player[2];
        players[0] = new Player("Player 1", 1);
        players[1] = new Robot("AI Player", 2);

        UnitNames.autoAssignThemes(players.length);

        // Create game
        game = new Game(players, map, ctx);

        System.out.println("JavaFX: Game initialized");
    }

    /**
     * Start the game execution thread.
     */
    private void startGameThread() {
        if (gameThread != null && gameThread.isAlive()) {
            // Stop existing game thread
            game.end();
            try {
                gameThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        gameThread = new Thread(
            () -> {
                try {
                    System.out.println("JavaFX: Starting game thread");
                    game.play();
                } catch (Exception e) {
                    System.err.println(
                        "FATAL: Game thread crashed with exception:"
                    );
                    e.printStackTrace();

                    // Publish game aborted event to notify UI
                    game
                        .getEventBus()
                        .publish(
                            new com.developingstorm.games.sad.events.GameAbortedEvent()
                        );
                }
            },
            "JavaFX-Game-Thread"
        );

        gameThread.setDaemon(true);
        gameThread.start();
    }

    /**
     * Create a player of the given type.
     * @param type 0=Human, 1=Robot
     */
    private Player createPlayer(int type, String name, int id) {
        if (type == 1) {
            return new Robot(name, id);
        }
        return new Player(name, id);
    }

    /**
     * Create the lifecycle handler that delegates to this application.
     */
    private GameLifecycleHandler createLifecycleHandler() {
        return new GameLifecycleHandler() {
            @Override
            public void loadGame(File saveFile) {
                SaDFxApplication.this.loadGame(saveFile);
            }

            @Override
            public void saveGame(File saveFile) {
                SaDFxApplication.this.saveGame(saveFile);
            }

            @Override
            public Game getCurrentGame() {
                return game;
            }

            @Override
            public String getCurrentSaveName() {
                return SaDFxApplication.this.getCurrentSaveName();
            }

            @Override
            public void quickSave(String saveName) {
                SaDFxApplication.this.quickSave(saveName);
            }

            @Override
            public void saveAs(String saveName) {
                SaDFxApplication.this.saveAs(saveName);
            }

            @Override
            public void newGame(
                String player1Name,
                int player1Type,
                String player2Name,
                int player2Type,
                String mapResource
            ) {
                SaDFxApplication.this.newGame(
                    player1Name,
                    player1Type,
                    player2Name,
                    player2Type,
                    mapResource
                );
            }
        };
    }

    @Override
    public void stop() {
        // Clean up when application closes
        if (game != null) {
            game.end();
        }
        System.out.println("JavaFX: Application stopped");
    }

    /**
     * Load a game from a save file.
     */
    private void loadGame(File saveFile) {
        try {
            // Stop current game
            if (game != null) {
                game.end();
            }
            if (gameThread != null && gameThread.isAlive()) {
                gameThread.join(1000);
            }

            // Load the game
            GameStateSerializer serializer = new GameStateSerializer();
            Game loadedGame = serializer.loadGame(saveFile, ctx);

            // Extract and store the save name for future saves
            this.currentSaveName = serializer.extractSaveName(saveFile);

            // Update game reference
            this.game = loadedGame;

            // Recreate controllers with new game
            controller = new GameControllerImpl(game);
            queryService = new GameQueryServiceImpl(game);

            // Create lifecycle handler
            GameLifecycleHandler lifecycleHandler = createLifecycleHandler();

            // Recreate the view on JavaFX thread
            Platform.runLater(() -> {
                GameView newGameView = new GameView(
                    game,
                    controller,
                    queryService,
                    lifecycleHandler
                );

                // Use system menu bar on macOS
                if (
                    System.getProperty("os.name").toLowerCase().contains("mac")
                ) {
                    newGameView.useSystemMenuBarProperty().set(true);
                }

                Scene scene = new Scene(newGameView, 1200, 800);
                primaryStage.setScene(scene);
                primaryStage.setTitle("Search and Destroy (JavaFX)");

                this.gameView = newGameView;

                // Start the game thread
                startGameThread();
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR
                    );
                alert.setTitle("Load Failed");
                alert.setHeaderText("Failed to load game");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    /**
     * Start a new game with the given settings.
     */
    private void newGame(
        String player1Name,
        int player1Type,
        String player2Name,
        int player2Type,
        String mapResource
    ) {
        try {
            // Stop current game
            if (game != null) {
                game.end();
            }
            if (gameThread != null && gameThread.isAlive()) {
                gameThread.join(1000);
            }

            // Reset naming pools
            ShipNames.reset();
            UnitNames.reset();

            // Reset save name for new game
            this.currentSaveName = null;

            // Load map
            this.map = HexBoardMap.loadMapAsResource(this, mapResource);

            // Create players
            Player[] players = new Player[2];
            players[0] = createPlayer(player1Type, player1Name, 1);
            players[1] = createPlayer(player2Type, player2Name, 2);

            UnitNames.autoAssignThemes(players.length);

            // Create game
            this.game = new Game(players, map, ctx);

            // Recreate controllers with new game
            controller = new GameControllerImpl(game);
            queryService = new GameQueryServiceImpl(game);

            // Create lifecycle handler
            GameLifecycleHandler lifecycleHandler = createLifecycleHandler();

            // Recreate the view on JavaFX thread
            Platform.runLater(() -> {
                GameView newGameView = new GameView(
                    game,
                    controller,
                    queryService,
                    lifecycleHandler
                );

                // Use system menu bar on macOS
                if (
                    System.getProperty("os.name").toLowerCase().contains("mac")
                ) {
                    newGameView.useSystemMenuBarProperty().set(true);
                }

                Scene scene = new Scene(newGameView, 1200, 800);
                primaryStage.setScene(scene);
                primaryStage.setTitle("Search and Destroy (JavaFX)");

                this.gameView = newGameView;

                // Start the game thread
                startGameThread();
            });

            System.out.println("JavaFX: New game started");
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR
                    );
                alert.setTitle("New Game Failed");
                alert.setHeaderText("Failed to create new game");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    /**
     * Save the current game to a file.
     */
    private void saveGame(File saveFile) {
        try {
            GameStateSerializer serializer = new GameStateSerializer();
            // Extract save name from file (remove .sav extension)
            String saveName = saveFile.getName().replace(".sav", "");
            serializer.saveGame(game, saveName);

            // Update current save name after successful save
            this.currentSaveName = saveName;
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.ERROR
                    );
                alert.setTitle("Save Failed");
                alert.setHeaderText("Failed to save game");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }

    /**
     * Get the current save name (null if never saved).
     */
    public String getCurrentSaveName() {
        return currentSaveName;
    }

    /**
     * Set the current save name.
     */
    public void setCurrentSaveName(String saveName) {
        this.currentSaveName = saveName;
    }

    /**
     * Public method to trigger save with current save name.
     */
    public void quickSave(String saveName) {
        File saveFile = new File(
            GameStateSerializer.getSaveDirectory(),
            saveName + ".sav"
        );
        saveGame(saveFile);
    }

    /**
     * Public method to trigger save with a new name.
     */
    public void saveAs(String saveName) {
        File saveFile = new File(
            GameStateSerializer.getSaveDirectory(),
            saveName + ".sav"
        );
        saveGame(saveFile);
    }

    /**
     * Public method to trigger load from file.
     */
    public void load(File saveFile) {
        loadGame(saveFile);
    }

    /**
     * Main entry point for JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
