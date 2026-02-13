package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.GameState;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.events.CombatResolvedEvent;
import com.developingstorm.games.sad.events.GameEvent;
import com.developingstorm.games.sad.events.GameEventListener;
import com.developingstorm.games.sad.events.GameEventType;
import com.developingstorm.games.sad.events.UnitSelectedEvent;
import com.developingstorm.games.sad.events.UnitTrackedEvent;
import com.developingstorm.games.sad.events.WaitingForOrdersEvent;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Main JavaFX view for the game.
 * Uses BorderPane layout with:
 * - Center: Map canvas
 * - Right: Unit info panel
 * - Bottom: Status bar
 */
public class GameView extends BorderPane {

    private final Game game;
    private final GameController controller;
    private final GameQueryService query;
    private final GameLifecycleHandler lifecycleHandler;
    private final GameCommandHandler commandHandler;

    private MapCanvas mapCanvas;
    private UnitInfoPanel unitInfoPanel;
    private BattleHistoryPanel battleHistoryPanel;
    private javafx.scene.control.ScrollPane scrollPane;
    private GameMenuBar menuBar;

    public GameView(
        Game game,
        GameController controller,
        GameQueryService query,
        GameLifecycleHandler lifecycleHandler
    ) {
        this.game = game;
        this.controller = controller;
        this.query = query;
        this.lifecycleHandler = lifecycleHandler;
        this.commandHandler = new GameCommandHandler(controller, query);

        initializeUI();
        registerEventHandlers();
        registerKeyboardShortcuts();
        updateStatus();

        // Start sprite animations (matching Swing's canvas.startAmination() call)
        startAnimation();
    }

    private void initializeUI() {
        // Create map canvas first (needed by menu bar)
        mapCanvas = new MapCanvas(game, controller, query);

        // Create menu bar (top) - needs mapCanvas reference
        menuBar = new GameMenuBar(
            controller,
            query,
            mapCanvas,
            lifecycleHandler
        );
        setTop(menuBar);

        // Set menu bar reference in canvas so it can check path visibility settings
        mapCanvas.setMenuBar(menuBar);

        // Create map canvas with scroll pane (center)
        scrollPane = new javafx.scene.control.ScrollPane(mapCanvas);
        scrollPane.setStyle("-fx-background: #1e1e1e;");
        scrollPane.setPannable(false); // Disable panning - we use mouse for game interaction
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        setCenter(scrollPane);

        // Create right sidebar with unit info and battle history
        VBox rightSidebar = new VBox();
        rightSidebar.setPrefWidth(250);
        rightSidebar.setMinWidth(250);

        // Unit info panel
        unitInfoPanel = new UnitInfoPanel(controller, query);
        unitInfoPanel.setStyle(
            "-fx-background-color: #2b2b2b; -fx-padding: 10;"
        );
        unitInfoPanel.setMinHeight(300);

        // Battle history panel
        battleHistoryPanel = new BattleHistoryPanel();
        VBox.setVgrow(battleHistoryPanel, Priority.ALWAYS);

        rightSidebar.getChildren().addAll(unitInfoPanel, battleHistoryPanel);
        setRight(rightSidebar);

        // Set some styling
        setStyle("-fx-background-color: #1e1e1e;");
    }

    /**
     * Register listeners for game events.
     */
    private void registerEventHandlers() {
        game
            .getEventBus()
            .addListener(
                new GameEventListener() {
                    @Override
                    public void onGameEvent(GameEvent event) {
                        // Event bus automatically marshals to EDT (Swing)
                        // For JavaFX, we need to use Platform.runLater
                        Platform.runLater(() -> handleGameEvent(event));
                    }

                    @Override
                    public GameEventType[] getInterestedEventTypes() {
                        return new GameEventType[] {
                            GameEventType.UNIT_SELECTED,
                            GameEventType.UNIT_TRACKED,
                            GameEventType.COMBAT_RESOLVED,
                            GameEventType.WAITING_FOR_ORDERS,
                            GameEventType.MAP_UPDATED,
                            GameEventType.GAME_ABORTED,
                        };
                    }
                }
            );
    }

    /**
     * Handle game events on the JavaFX Application Thread.
     */
    private void handleGameEvent(GameEvent event) {
        switch (event.getEventType()) {
            case UNIT_SELECTED:
                UnitSelectedEvent use = (UnitSelectedEvent) event;
                unitInfoPanel.setUnit(use.getUnit());
                mapCanvas.refresh();
                break;
            case UNIT_TRACKED:
                UnitTrackedEvent ute = (UnitTrackedEvent) event;
                if (ute.getUnit() != null) {
                    mapCanvas.centerOnLocation(ute.getUnit().getLocation());
                }
                break;
            case UNIT_MOVED:
                // Unit moved - refresh map and update info panel if this is the displayed unit
                com.developingstorm.games.sad.events.UnitMovedEvent ume =
                    (com.developingstorm.games.sad.events.UnitMovedEvent) event;
                Unit displayedUnit = unitInfoPanel.getUnit();
                if (
                    displayedUnit != null && displayedUnit.id == ume.getUnitId()
                ) {
                    // Displayed unit moved - refresh info panel to show new location
                    unitInfoPanel.setUnit(displayedUnit);
                }
                mapCanvas.refresh();
                break;
            case COMBAT_RESOLVED:
                CombatResolvedEvent cre = (CombatResolvedEvent) event;
                if (cre.getResult() != null) {
                    battleHistoryPanel.addBattle(cre.getResult());
                }
                mapCanvas.refresh();
                break;
            case WAITING_FOR_ORDERS:
                WaitingForOrdersEvent woe = (WaitingForOrdersEvent) event;
                updateStatus();
                break;
            case MAP_UPDATED:
                // Refresh paths when map updates (units moved, paths changed, etc.)
                Player currentPlayer = query.getCurrentPlayer();
                if (currentPlayer != null && !currentPlayer.isRobot()) {
                    boolean[] pathVisibility = mapCanvas.getPathVisibility();
                    mapCanvas.refreshPaths(
                        currentPlayer,
                        pathVisibility[0], // air
                        pathVisibility[1], // ground
                        pathVisibility[2] // sea
                    );
                }
                mapCanvas.refresh();
                break;
            case GAME_ABORTED:
                handleGameAborted();
                break;
        }

        // Always update status
        updateStatus();
    }

    /**
     * Handle game abort - show error dialog and exit.
     */
    private void handleGameAborted() {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Game Error");
        alert.setHeaderText("Game Terminated");
        alert.setContentText(
            "The game has terminated due to an error.\n" +
                "Please check the console for details.\n\n" +
                "The application will now exit."
        );
        alert.showAndWait();

        // Exit the application
        javafx.application.Platform.exit();
        System.exit(1);
    }

    /**
     * Register keyboard shortcuts.
     * Matches the Swing version's GameModeController keyboard handling.
     * Uses addEventFilter to capture keys before ScrollPane can consume them.
     */
    private void registerKeyboardShortcuts() {
        addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            Unit selected = query.getSelectedUnit();
            boolean consumed = false;

            switch (event.getCode()) {
                case C:
                    // Center on selected unit
                    if (selected != null) {
                        centerOnLocation(selected.getLocation());
                        consumed = true;
                    }
                    break;
                case S:
                    // Sentry
                    if (selected != null) {
                        commandHandler.sentry(selected);
                        consumed = true;
                    }
                    break;
                case SPACE:
                    // Skip turn
                    if (selected != null) {
                        commandHandler.skipTurn(selected);
                        consumed = true;
                    }
                    break;
                case X:
                    // Explore
                    if (selected != null) {
                        commandHandler.explore(selected);
                        consumed = true;
                    }
                    break;
                case U:
                    // Unload
                    if (selected != null) {
                        commandHandler.unload(selected);
                        consumed = true;
                    }
                    break;
                case H:
                    // Head home
                    if (selected != null) {
                        commandHandler.headHome(selected);
                        consumed = true;
                    }
                    break;
                case K:
                    // Disband
                    if (selected != null) {
                        commandHandler.disband(selected);
                        consumed = true;
                    }
                    break;
                case ENTER:
                    // End turn - skip all remaining units
                    commandHandler.endTurn();
                    consumed = true;
                    break;
                case ESCAPE:
                    // Delegate to mode manager first
                    // If not handled, deselect unit
                    boolean handled = mapCanvas
                        .getModeManager()
                        .delegateKeyPressed(event);
                    if (!handled) {
                        commandHandler.selectUnit(null);
                    }
                    consumed = true;
                    break;
                case F5:
                    // Toggle debug path display
                    // TODO: Implement debug path toggle when needed
                    consumed = true;
                    break;
                case F8:
                    // Pause/Resume (if needed for debugging)
                    // TODO: Implement pause/resume if needed
                    consumed = true;
                    break;
                default:
                    break;
            }

            // Consume the event to prevent it from propagating to ScrollPane
            if (consumed) {
                event.consume();
            }
        });

        // Request focus so keyboard events work
        setFocusTraversable(true);
        requestFocus();
    }

    /**
     * Center the viewport on a specific location.
     * Matches the Swing version's SaDFrame.center() method.
     */
    public void centerOnLocation(
        com.developingstorm.games.hexboard.Location loc
    ) {
        if (loc == null || scrollPane == null || mapCanvas == null) {
            return;
        }

        // Get the center coordinates of the hex
        double[] center = mapCanvas.getHexCenter(loc);
        double centerX = center[0];
        double centerY = center[1];

        // Get viewport dimensions
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();

        // Get content dimensions
        double contentWidth = mapCanvas.getWidth();
        double contentHeight = mapCanvas.getHeight();

        // Calculate the position to center the hex in the viewport
        double hValue =
            (centerX - viewportWidth / 2.0) / (contentWidth - viewportWidth);
        double vValue =
            (centerY - viewportHeight / 2.0) / (contentHeight - viewportHeight);

        // Clamp values to [0, 1]
        hValue = Math.max(0, Math.min(1, hValue));
        vValue = Math.max(0, Math.min(1, vValue));

        // Set scroll position
        scrollPane.setHvalue(hValue);
        scrollPane.setVvalue(vValue);
    }

    /**
     * Update status display from current game state.
     */
    private void updateStatus() {
        // Status is now shown in the unit info panel
        // This method is kept for event handling but does nothing
    }

    /**
     * Start sprite animations.
     * Matches Swing SaDFrame.initGame() which calls canvas.startAmination().
     */
    public void startAnimation() {
        if (mapCanvas != null) {
            mapCanvas.startAnimation();
        }
    }

    /**
     * Stop sprite animations.
     * Matches Swing SaDFrame.termGame() which calls sprites.stop().
     */
    public void stopAnimation() {
        if (mapCanvas != null) {
            mapCanvas.stopAnimation();
        }
    }

    /**
     * Get the menu bar's useSystemMenuBar property for macOS native menu bar.
     */
    public javafx.beans.property.BooleanProperty useSystemMenuBarProperty() {
        return menuBar.useSystemMenuBarProperty();
    }
}
