package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Menu bar for the JavaFX game UI.
 * Matches the Swing version's MenuBarBuilder.
 */
public class GameMenuBar extends MenuBar {

    private final GameController controller;
    private final GameQueryService query;
    private final MapCanvas mapCanvas;
    private final GameLifecycleHandler lifecycleHandler;

    // Menu items that need to be updated
    private CheckMenuItem viewSeaPaths;
    private CheckMenuItem viewAirPaths;
    private CheckMenuItem viewGroundPaths;

    public GameMenuBar(
        GameController controller,
        GameQueryService query,
        MapCanvas mapCanvas,
        GameLifecycleHandler lifecycleHandler
    ) {
        this.controller = controller;
        this.query = query;
        this.mapCanvas = mapCanvas;
        this.lifecycleHandler = lifecycleHandler;

        buildMenus();
    }

    private void buildMenus() {
        // File Menu
        Menu fileMenu = createFileMenu();

        // View Menu
        Menu viewMenu = createViewMenu();

        // Controls Menu
        Menu controlsMenu = createControlsMenu();

        // Debug Menu
        Menu debugMenu = createDebugMenu();

        // Help Menu
        Menu helpMenu = createHelpMenu();

        getMenus().addAll(
            fileMenu,
            viewMenu,
            controlsMenu,
            debugMenu,
            helpMenu
        );
    }

    private Menu createFileMenu() {
        Menu fileMenu = new Menu("_File");

        // New
        MenuItem newItem = new MenuItem("_New");
        newItem.setAccelerator(
            new KeyCodeCombination(KeyCode.N, KeyCombination.ALT_DOWN)
        );
        newItem.setOnAction(e -> onNew());

        // Load Game
        MenuItem loadItem = new MenuItem("_Load Game...");
        loadItem.setAccelerator(
            new KeyCodeCombination(KeyCode.L, KeyCombination.ALT_DOWN)
        );
        loadItem.setOnAction(e -> onLoadGame());

        // Save
        MenuItem saveItem = new MenuItem("_Save");
        saveItem.setAccelerator(
            new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN)
        );
        saveItem.setOnAction(e -> onSave());

        // Save As
        MenuItem saveAsItem = new MenuItem("Save _As...");
        saveAsItem.setOnAction(e -> onSaveAs());

        // Exit
        MenuItem exitItem = new MenuItem("E_xit");
        exitItem.setAccelerator(
            new KeyCodeCombination(KeyCode.X, KeyCombination.ALT_DOWN)
        );
        exitItem.setOnAction(e -> onExit());

        fileMenu
            .getItems()
            .addAll(
                newItem,
                loadItem,
                new SeparatorMenuItem(),
                saveItem,
                saveAsItem,
                new SeparatorMenuItem(),
                exitItem
            );

        return fileMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu("_View");

        // Center
        MenuItem centerItem = new MenuItem("_Center");
        centerItem.setOnAction(e -> onCenter());

        // Sea Paths
        viewSeaPaths = new CheckMenuItem("Sea Paths");
        viewSeaPaths.setSelected(true); // Default to visible
        viewSeaPaths.setOnAction(e -> onViewSeaPaths());

        // Air Paths
        viewAirPaths = new CheckMenuItem("Air Paths");
        viewAirPaths.setSelected(true); // Default to visible
        viewAirPaths.setOnAction(e -> onViewAirPaths());

        // Ground Paths
        viewGroundPaths = new CheckMenuItem("Ground Paths");
        viewGroundPaths.setSelected(true); // Default to visible
        viewGroundPaths.setOnAction(e -> onViewGroundPaths());

        viewMenu
            .getItems()
            .addAll(
                centerItem,
                new SeparatorMenuItem(),
                viewSeaPaths,
                viewAirPaths,
                viewGroundPaths
            );

        return viewMenu;
    }

    private Menu createControlsMenu() {
        Menu controlsMenu = new Menu("_Controls");

        // Radio buttons for mode selection
        ToggleGroup modeGroup = new ToggleGroup();

        RadioMenuItem gameMode = new RadioMenuItem("Game Mode");
        gameMode.setToggleGroup(modeGroup);
        gameMode.setSelected(true);
        gameMode.setOnAction(e -> onGameMode());

        RadioMenuItem exploreMode = new RadioMenuItem("Explore Mode");
        exploreMode.setToggleGroup(modeGroup);
        exploreMode.setOnAction(e -> onExploreMode());

        controlsMenu.getItems().addAll(gameMode, exploreMode);

        return controlsMenu;
    }

    private Menu createDebugMenu() {
        Menu debugMenu = new Menu("_Debug");

        // Track A*
        CheckMenuItem debugAstar = new CheckMenuItem("Track A*");
        debugAstar.setSelected(false);
        debugAstar.setOnAction(e -> onDebugAstar(debugAstar.isSelected()));

        // God Lens
        CheckMenuItem debugGodLens = new CheckMenuItem("God Lens");
        debugGodLens.setSelected(false);
        debugGodLens.setOnAction(e ->
            onDebugGodLens(debugGodLens.isSelected())
        );

        // Continent Numbers
        CheckMenuItem debugContinentNumbers = new CheckMenuItem(
            "Continent Numbers"
        );
        debugContinentNumbers.setSelected(false);
        debugContinentNumbers.setOnAction(e ->
            onDebugContinentNumbers(debugContinentNumbers.isSelected())
        );

        // Locations
        CheckMenuItem debugLocations = new CheckMenuItem("Locations");
        debugLocations.setSelected(false);
        debugLocations.setOnAction(e ->
            onDebugLocations(debugLocations.isSelected())
        );

        // Path Errors
        CheckMenuItem debugPathErrors = new CheckMenuItem("Path Errors");
        debugPathErrors.setSelected(false);
        debugPathErrors.setOnAction(e ->
            onDebugPathErrors(debugPathErrors.isSelected())
        );

        // Dump State
        MenuItem debugDump = new MenuItem("Dump State");
        debugDump.setOnAction(e -> onDebugDump());

        debugMenu
            .getItems()
            .addAll(
                debugAstar,
                debugGodLens,
                debugContinentNumbers,
                debugLocations,
                debugPathErrors,
                new SeparatorMenuItem(),
                debugDump
            );

        return debugMenu;
    }

    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("_Help");

        // About
        MenuItem aboutItem = new MenuItem("About...");
        aboutItem.setOnAction(e -> onAbout());

        helpMenu.getItems().add(aboutItem);

        return helpMenu;
    }

    // File Menu Actions

    private void onNew() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Game");
        alert.setHeaderText("New Game");
        alert.setContentText(
            "New game dialog not yet implemented in JavaFX version."
        );
        alert.showAndWait();
    }

    private void onLoadGame() {
        try {
            // Show the load game dialog
            SaveGameDialog loadDialog = new SaveGameDialog(
                getScene().getWindow(),
                com.developingstorm.games.sad.persistence.GameStateSerializer.getSaveDirectory()
            );
            loadDialog.showAndWait();

            java.io.File saveFile = loadDialog.getSelectedFile();
            if (saveFile == null) {
                // User cancelled
                return;
            }

            if (!saveFile.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("File Not Found");
                alert.setHeaderText(null);
                alert.setContentText(
                    "Save file not found: " + saveFile.getPath()
                );
                alert.showAndWait();
                return;
            }

            // Load the game using the lifecycle handler
            lifecycleHandler.loadGame(saveFile);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Load Failed");
            alert.setHeaderText("Failed to load game");
            alert.setContentText("Error: " + e.getMessage());
            e.printStackTrace();
            alert.showAndWait();
        }
    }

    private void onSave() {
        if (query.getGame() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Save");
            alert.setHeaderText("No Game in Progress");
            alert.setContentText("No game in progress to save.");
            alert.showAndWait();
            return;
        }

        // If no save name exists, prompt for one (Save As behavior)
        String currentSaveName = lifecycleHandler.getCurrentSaveName();
        if (currentSaveName == null) {
            javafx.scene.control.TextInputDialog dialog =
                new javafx.scene.control.TextInputDialog();
            dialog.setTitle("Save Game");
            dialog.setHeaderText("Enter a name for this save:");
            dialog.setContentText("Save name:");

            java.util.Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String saveName = result.get().trim();
                lifecycleHandler.quickSave(saveName);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Game Saved");
                alert.setHeaderText("Success");
                alert.setContentText(
                    "Game saved successfully as '" + saveName + "'"
                );
                alert.showAndWait();
            }
        } else {
            // Quick save with existing name
            lifecycleHandler.quickSave(currentSaveName);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Saved");
            alert.setHeaderText("Success");
            alert.setContentText(
                "Game saved successfully as '" + currentSaveName + "'"
            );
            alert.showAndWait();
        }
    }

    private void onSaveAs() {
        if (query.getGame() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Cannot Save");
            alert.setHeaderText("No Game in Progress");
            alert.setContentText("No game in progress to save.");
            alert.showAndWait();
            return;
        }

        // Always prompt for a new save name
        javafx.scene.control.TextInputDialog dialog =
            new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Save Game As");
        dialog.setHeaderText("Enter a name for this save:");
        dialog.setContentText("Save name:");

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String saveName = result.get().trim();
            lifecycleHandler.saveAs(saveName);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Saved");
            alert.setHeaderText("Success");
            alert.setContentText(
                "Game saved successfully as '" + saveName + "'"
            );
            alert.showAndWait();
        }
    }

    private void onExit() {
        Platform.exit();
        System.exit(0);
    }

    // View Menu Actions

    private void onCenter() {
        var selected = query.getSelectedUnit();
        if (selected != null) {
            controller.trackUnit(selected);
        }
    }

    private void onViewSeaPaths() {
        updatePathVisualization();
    }

    private void onViewAirPaths() {
        updatePathVisualization();
    }

    private void onViewGroundPaths() {
        updatePathVisualization();
    }

    private void updatePathVisualization() {
        var currentPlayer = query.getCurrentPlayer();
        if (currentPlayer != null) {
            mapCanvas.refreshPaths(
                currentPlayer,
                viewAirPaths.isSelected(),
                viewGroundPaths.isSelected(),
                viewSeaPaths.isSelected()
            );
            mapCanvas.refresh();
        }
    }

    /**
     * Get the current path visibility settings.
     * Used by PathMode to update paths after setting them.
     */
    public boolean[] getPathVisibility() {
        return new boolean[] {
            viewAirPaths.isSelected(),
            viewGroundPaths.isSelected(),
            viewSeaPaths.isSelected(),
        };
    }

    // Controls Menu Actions

    private void onGameMode() {
        // Switch to game mode
        System.out.println("Game mode selected");
    }

    private void onExploreMode() {
        // Switch to explore mode
        System.out.println("Explore mode selected");
    }

    // Debug Menu Actions

    private void onDebugAstar(boolean selected) {
        System.out.println("Debug A*: " + selected);
    }

    private void onDebugGodLens(boolean selected) {
        System.out.println("God Lens: " + selected);
    }

    private void onDebugContinentNumbers(boolean selected) {
        System.out.println("Continent Numbers: " + selected);
    }

    private void onDebugLocations(boolean selected) {
        System.out.println("Locations: " + selected);
    }

    private void onDebugPathErrors(boolean selected) {
        System.out.println("Path Errors: " + selected);
    }

    private void onDebugDump() {
        if (query.getGame() != null) {
            query.getGame().dump();
        }
    }

    // Help Menu Actions

    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Search and Destroy");
        alert.setContentText(
            "Search and Destroy\n\n" +
                "A turn-based strategy game\n\n" +
                "JavaFX Version"
        );
        alert.showAndWait();
    }
}
