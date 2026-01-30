package com.developingstorm.games.sad.fx;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.EdictGovernor;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.Vision;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.fx.sprites.FxArrowSprite;
import com.developingstorm.games.sad.fx.sprites.FxLineSprite;
import com.developingstorm.games.sad.fx.sprites.FxSprite;
import com.developingstorm.games.sad.orders.Patrol;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * JavaFX canvas for rendering the hex map.
 * Uses pointy-top hexagon geometry matching the Swing HexFactory implementation.
 */
public class MapCanvas extends StackPane {

    private final Game game;
    private final GameController controller;
    private final GameQueryService query;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final TerrainImages terrainImages;

    // Reference to menu bar for accessing path visibility settings
    private GameMenuBar menuBar;

    // Sprite lists for path visualization (matching Swing version)
    private List<FxSprite> sprites;
    private List<FxSprite> seaPaths;
    private List<FxSprite> airPaths;
    private List<FxSprite> groundPaths;
    private List<FxSprite> patrolPaths;

    // Mode manager for handling different UI interaction modes
    private com.developingstorm.games.sad.fx.modes.MapCanvasModeManager modeManager;

    // Selection sprite for animated unit selection indicator
    private com.developingstorm.games.sad.fx.sprites.FxSelectionSprite selectionSprite;
    private Unit lastSelectedUnit; // Track which unit has the selection sprite
    private Location lastSelectedLocation; // Track unit's location to detect movement

    // Public methods needed by mode classes
    public Game getGame() {
        return game;
    }

    public GameController getController() {
        return controller;
    }

    public GameQueryService getQuery() {
        return query;
    }

    public com.developingstorm.games.sad.fx.modes.MapCanvasModeManager getModeManager() {
        return modeManager;
    }

    /**
     * Set the menu bar reference for accessing path visibility settings.
     * Must be called after construction since MapCanvas and GameMenuBar have circular dependency.
     */
    public void setMenuBar(GameMenuBar menuBar) {
        this.menuBar = menuBar;
    }

    /**
     * Get the current path visibility settings from the menu bar.
     * Returns [air, ground, sea] visibility flags.
     * If menuBar is not set, defaults to all visible.
     */
    public boolean[] getPathVisibility() {
        if (menuBar != null) {
            return menuBar.getPathVisibility();
        }
        // Default to all visible if menu bar not set yet
        return new boolean[] { true, true, true };
    }

    // Animation timer for sprite animations (matching Swing's SpriteEngine)
    private AnimationTimer animationTimer;
    private boolean animationRunning = false;

    // Match Swing version's hex geometry (HexFactory with hexSide=24)
    private static final int HEX_SIDE = 24;
    private static final double HEX_WIDTH = HEX_SIDE * 1.7320508; // sqrt(3)
    private static final double HEX_HALF_WIDTH = HEX_WIDTH / 2.0;
    private static final double HEX_PEAK = HEX_SIDE / 2.0;
    private static final double HEX_HEIGHT = HEX_PEAK + HEX_SIDE;

    public MapCanvas(
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        this.game = game;
        this.controller = controller;
        this.query = query;
        this.terrainImages = TerrainImages.getInstance();

        // Initialize sprite lists
        this.sprites = new ArrayList<>();
        this.seaPaths = null;
        this.airPaths = null;
        this.groundPaths = null;
        this.patrolPaths = null;

        // Create canvas
        Board board = query.getBoard();
        int mapWidth = (int) (board.getWidth() * HEX_WIDTH + HEX_WIDTH) + 100;
        int mapHeight = (int) (board.getHeight() * HEX_HEIGHT) + 100;

        canvas = new Canvas(mapWidth, mapHeight);
        gc = canvas.getGraphicsContext2D();

        getChildren().add(canvas);

        // Initialize mode manager and register all modes
        initializeModeManager();

        // Handle mouse events (matching Swing GameModeController)
        canvas.setOnMousePressed(this::handleMousePressed);
        canvas.setOnMouseReleased(this::handleMouseReleased);
        canvas.setOnMouseDragged(this::handleMouseDragged);
        canvas.setOnMouseMoved(this::handleMouseMoved);

        // Handle context menu (right-click)
        canvas.setOnContextMenuRequested(this::handleContextMenu);

        // Set up animation timer for sprite animations (matching Swing's SpriteEngine)
        // Timer is created but not started - must call startAnimation() explicitly
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateAnimations(now);
            }
        };

        // Initial draw
        refresh();
    }

    /**
     * Initialize the mode manager and register all modes.
     */
    private void initializeModeManager() {
        this.modeManager =
            new com.developingstorm.games.sad.fx.modes.MapCanvasModeManager();

        // Create and register all mode implementations
        this.modeManager.registerMode(
            com.developingstorm.games.sad.fx.UIMode.GAME,
            new com.developingstorm.games.sad.fx.modes.GameMode(
                this,
                game,
                controller,
                query
            )
        );

        this.modeManager.registerMode(
            com.developingstorm.games.sad.fx.UIMode.PATHS,
            new com.developingstorm.games.sad.fx.modes.PathMode(
                this,
                game,
                controller,
                query
            )
        );

        this.modeManager.registerMode(
            com.developingstorm.games.sad.fx.UIMode.EXPLORE,
            new com.developingstorm.games.sad.fx.modes.ExploreMode(
                this,
                game,
                controller,
                query
            )
        );

        this.modeManager.registerMode(
            com.developingstorm.games.sad.fx.UIMode.PATROL,
            new com.developingstorm.games.sad.fx.modes.PatrolMode(
                this,
                game,
                controller,
                query
            )
        );

        this.modeManager.registerMode(
            com.developingstorm.games.sad.fx.UIMode.ATTACK,
            new com.developingstorm.games.sad.fx.modes.AttackMode(
                this,
                game,
                controller,
                query
            )
        );

        this.modeManager.registerMode(
            com.developingstorm.games.sad.fx.UIMode.ESCORT,
            new com.developingstorm.games.sad.fx.modes.EscortMode(
                this,
                game,
                controller,
                query
            )
        );

        // Start in GAME mode
        this.modeManager.switchMode(
            com.developingstorm.games.sad.fx.UIMode.GAME
        );
    }

    /**
     * Start sprite animations.
     * Matches Swing HexCanvas.startAmination() behavior.
     */
    public void startAnimation() {
        if (!animationRunning && animationTimer != null) {
            animationTimer.start();
            animationRunning = true;
        }
    }

    /**
     * Stop sprite animations.
     * Matches Swing HexCanvas.stopAmination() behavior.
     */
    public void stopAnimation() {
        if (animationRunning && animationTimer != null) {
            animationTimer.stop();
            animationRunning = false;
        }
    }

    /**
     * Update sprite animations.
     * Called by the AnimationTimer on each frame.
     * Matches Swing SpriteEngine animation loop behavior.
     */
    private void updateAnimations(long currentTimeNanos) {
        if (!animationRunning) {
            return;
        }

        long currentTimeMillis = currentTimeNanos / 1_000_000; // Convert to milliseconds

        // Update animations for all sprites (matching Swing's sprite.check() pattern)
        for (FxSprite sprite : sprites) {
            if (sprite instanceof FxArrowSprite) {
                FxArrowSprite arrowSprite = (FxArrowSprite) sprite;
                arrowSprite.updateAnimation(currentTimeMillis);
            }
        }

        // Always refresh when animation is running - this keeps selection sprite
        // and other animations smooth, and also picks up unit status changes
        refresh();
    }

    /**
     * Refresh the map display.
     */
    public void refresh() {
        drawMap();
    }

    /**
     * Draw the entire map.
     */
    private void drawMap() {
        Board board = query.getBoard();

        // Clear canvas with dark background
        gc.setFill(Color.rgb(20, 20, 20));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Draw all hexes
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                Location loc = Location.get(x, y);
                BoardHex hex = board.get(loc);
                if (hex != null) {
                    drawHex(hex, loc);
                }
            }
        }

        // Draw cities (only in explored areas)
        Player currentPlayer = query.getCurrentPlayer();
        for (City city : board.getCities()) {
            Location cityLoc = city.getLocation();
            boolean isExplored =
                (currentPlayer == null) || currentPlayer.isExplored(cityLoc);
            if (isExplored) {
                drawCity(city);
            }
        }

        // Draw units (only in explored areas)
        // Group units by location to handle stacks properly
        java.util.Map<Location, java.util.List<Unit>> unitsByLocation =
            new java.util.HashMap<>();
        for (Unit unit : query.getAllUnits()) {
            // Show units that are either not carried, OR are carried but awake (being unloaded)
            if (
                !unit.isCarried() || (unit.isCarried() && !unit.inSentryMode())
            ) {
                unitsByLocation
                    .computeIfAbsent(unit.getLocation(), k ->
                        new java.util.ArrayList<>()
                    )
                    .add(unit);
            }
        }

        // Draw one unit per location (the active one)
        Unit selectedUnit = query.getSelectedUnit();
        for (java.util.Map.Entry<
            Location,
            java.util.List<Unit>
        > entry : unitsByLocation.entrySet()) {
            Location loc = entry.getKey();
            java.util.List<Unit> unitsAtLoc = entry.getValue();

            if (!unitsAtLoc.isEmpty()) {
                // Determine which unit to draw (active unit in stack)
                Unit unitToDraw = null;

                // Priority 1: Selected unit if it's in this stack
                if (selectedUnit != null && unitsAtLoc.contains(selectedUnit)) {
                    unitToDraw = selectedUnit;
                } else {
                    // Priority 2: First unit with available moves
                    for (Unit u : unitsAtLoc) {
                        if (u.life().hasMoves() && !u.inSentryMode()) {
                            unitToDraw = u;
                            break;
                        }
                    }
                    // Priority 3: Just use the first unit
                    if (unitToDraw == null) {
                        unitToDraw = unitsAtLoc.get(0);
                    }
                }

                // Only draw units that are either:
                // 1. Owned by the current player (always visible to themselves)
                // 2. Located in currently visible areas (not just explored)
                boolean shouldDraw = false;
                if (currentPlayer == null) {
                    // No fog of war (testing/spectator mode)
                    shouldDraw = true;
                } else if (unitToDraw.getOwner() == currentPlayer) {
                    // Always show our own units
                    shouldDraw = true;
                } else {
                    // Enemy units: only show in currently visible areas
                    Vision visibility = currentPlayer.getVisibility(loc);
                    shouldDraw = (visibility != Vision.NONE);
                }

                if (shouldDraw) {
                    drawUnit(unitToDraw);

                    // Draw stack indicator if multiple units
                    if (unitsAtLoc.size() > 1) {
                        double[] center = getHexCenter(loc);
                        String count = String.valueOf(unitsAtLoc.size());

                        // Scale badge size based on digit count
                        double badgeSize = count.length() == 1 ? 14 : 18;
                        double badgeX = center[0] + 12;
                        double badgeY = center[1] - 12;

                        // Draw white circle with black border
                        gc.setFill(Color.WHITE);
                        gc.setStroke(Color.BLACK);
                        gc.setLineWidth(1.5);
                        gc.fillOval(
                            badgeX - badgeSize / 2,
                            badgeY - badgeSize / 2,
                            badgeSize,
                            badgeSize
                        );
                        gc.strokeOval(
                            badgeX - badgeSize / 2,
                            badgeY - badgeSize / 2,
                            badgeSize,
                            badgeSize
                        );

                        // Draw centered text
                        gc.setFill(Color.BLACK);
                        javafx.scene.text.Font font =
                            javafx.scene.text.Font.font(
                                "Arial",
                                javafx.scene.text.FontWeight.BOLD,
                                count.length() == 1 ? 10 : 9
                            );
                        gc.setFont(font);
                        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
                        gc.setTextBaseline(javafx.geometry.VPos.CENTER);

                        // Draw text centered on badge
                        gc.fillText(count, badgeX, badgeY);

                        // Reset text alignment
                        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
                        gc.setTextBaseline(javafx.geometry.VPos.BASELINE);
                    }
                }
            }
        }

        // Update selection sprite based on selected unit
        updateSelectionSprite();

        // Draw sprites (paths, arrows, etc.)
        drawSprites();
    }

    /**
     * Draw all sprites on the canvas.
     */
    private void drawSprites() {
        for (FxSprite sprite : sprites) {
            sprite.draw(gc);
        }

        // Delegate drawing to current mode
        if (modeManager != null) {
            modeManager.drawCurrentMode(gc);
        }

        // Remove sprites that are done
        sprites.removeIf(FxSprite::done);
    }

    /**
     * Update the selection sprite based on the currently selected unit.
     * Recreates the sprite when the selected unit changes OR when the selected unit moves.
     */
    private void updateSelectionSprite() {
        Unit selected = query.getSelectedUnit();
        Location currentLocation = (selected != null)
            ? selected.getLocation()
            : null;

        // Check if we need to update: different unit OR same unit but different location
        boolean needsUpdate =
            (selected != lastSelectedUnit) ||
            (selected != null &&
                !selected.getLocation().equals(lastSelectedLocation));

        if (!needsUpdate) {
            return; // No change, keep existing sprite
        }

        // Remove old selection sprite if it exists
        if (selectionSprite != null) {
            sprites.remove(selectionSprite);
            selectionSprite = null;
        }

        // Add new selection sprite if there's a selected unit
        if (selected != null) {
            double[] center = getHexCenter(selected.getLocation());
            double bgSize = 28;
            selectionSprite =
                new com.developingstorm.games.sad.fx.sprites.FxSelectionSprite(
                    center[0],
                    center[1],
                    bgSize
                );
            sprites.add(selectionSprite);
        }

        // Update tracking
        lastSelectedUnit = selected;
        lastSelectedLocation = currentLocation;
    }

    /**
     * Add a sprite to the canvas.
     */
    public void addSprite(FxSprite sprite) {
        sprites.add(sprite);
    }

    /**
     * Remove a sprite from the canvas.
     */
    public void removeSprite(FxSprite sprite) {
        sprites.remove(sprite);
    }

    /**
     * Add a list of sprites.
     */
    private void addSprites(List<FxSprite> list) {
        if (list == null) {
            return;
        }
        sprites.addAll(list);
    }

    /**
     * Remove a list of sprites.
     */
    private void removeSprites(List<FxSprite> list) {
        if (list == null) {
            return;
        }
        sprites.removeAll(list);
    }

    /**
     * Draw a single hex using pointy-top hexagon geometry.
     * Matches the HexFactory algorithm from the Swing version.
     */
    private void drawHex(BoardHex hex, Location loc) {
        Board board = query.getBoard();

        // Check if location is explored (visibility/fog of war)
        Player currentPlayer = query.getCurrentPlayer();
        boolean isExplored =
            (currentPlayer == null) || currentPlayer.isExplored(loc);

        // If unexplored, show unexplored image
        if (!isExplored) {
            drawUnexploredHex(hex, loc);
            return;
        }

        int terrain = board.getTerrain(loc);

        // Calculate hex vertices using HexFactory algorithm
        double x2;
        if (loc.getY() % 2 != 0) x2 = HEX_HALF_WIDTH;
        else x2 = 0;

        double y2 = loc.getY() * HEX_HEIGHT + HEX_PEAK;

        // Build pointy-top hexagon vertices (matches HexFactory order)
        double[] xCoord = new double[6];
        double[] yCoord = new double[6];

        xCoord[3] = x2 + loc.getX() * HEX_WIDTH;
        yCoord[3] = y2;
        xCoord[2] = xCoord[3] + HEX_HALF_WIDTH;
        yCoord[2] = yCoord[3] - HEX_PEAK;
        xCoord[1] = xCoord[3] + HEX_WIDTH;
        yCoord[1] = yCoord[3];
        xCoord[0] = xCoord[1];
        yCoord[0] = yCoord[1] + HEX_SIDE;
        xCoord[5] = xCoord[2];
        yCoord[5] = yCoord[3] + HEX_SIDE + HEX_PEAK;
        xCoord[4] = xCoord[3];
        yCoord[4] = yCoord[0];

        // Calculate origin (minimum x and y, matching Hex.getOrigin())
        double originX = Double.MAX_VALUE;
        double originY = Double.MAX_VALUE;
        for (int i = 0; i < 6; i++) {
            if (xCoord[i] < originX) originX = xCoord[i];
            if (yCoord[i] < originY) originY = yCoord[i];
        }

        // Draw terrain image
        javafx.scene.image.Image terrainImage = terrainImages.getTerrainImage(
            terrain
        );
        if (terrainImage != null) {
            // Draw image at origin without scaling (use native size)
            gc.drawImage(terrainImage, originX, originY);
        } else {
            // Fallback to solid color if image not loaded
            Color color = getTerrainColor(hex);
            gc.setFill(color);
            gc.fillPolygon(xCoord, yCoord, 6);
        }

        // Draw border matching Swing version
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(1.0);
        gc.strokePolygon(xCoord, yCoord, 6);
    }

    /**
     * Draw an unexplored hex (fog of war).
     * Matches Swing HexCanvas behavior for unexplored hexes.
     */
    private void drawUnexploredHex(BoardHex hex, Location loc) {
        // Calculate hex vertices (same as drawHex)
        double x2;
        if (loc.getY() % 2 != 0) x2 = HEX_HALF_WIDTH;
        else x2 = 0;

        double y2 = loc.getY() * HEX_HEIGHT + HEX_PEAK;

        // Calculate origin
        double originX = x2 + loc.getX() * HEX_WIDTH;
        double originY = y2 - HEX_PEAK;

        // Draw unexplored image
        javafx.scene.image.Image unexploredImage =
            terrainImages.getUnexploredImage();
        if (unexploredImage != null) {
            gc.drawImage(unexploredImage, originX, originY);
        } else {
            // Fallback to dark color if image not loaded
            gc.setFill(Color.rgb(40, 40, 40));

            // Calculate vertices for polygon fill
            double[] xCoord = new double[6];
            double[] yCoord = new double[6];
            xCoord[3] = x2 + loc.getX() * HEX_WIDTH;
            yCoord[3] = y2;
            xCoord[2] = xCoord[3] + HEX_HALF_WIDTH;
            yCoord[2] = yCoord[3] - HEX_PEAK;
            xCoord[1] = xCoord[3] + HEX_WIDTH;
            yCoord[1] = yCoord[3];
            xCoord[0] = xCoord[1];
            yCoord[0] = yCoord[1] + HEX_SIDE;
            xCoord[5] = xCoord[2];
            yCoord[5] = yCoord[3] + HEX_SIDE + HEX_PEAK;
            xCoord[4] = xCoord[3];
            yCoord[4] = yCoord[0];

            gc.fillPolygon(xCoord, yCoord, 6);
        }
    }

    /**
     * Draw a city.
     */
    private void drawCity(City city) {
        double[] center = getHexCenter(city.getLocation());

        // Determine color based on owner (matches Swing version)
        Color cityColor = Color.BLACK;
        int size = 3;

        if (city.getOwner() != null) {
            int playerId = city.getOwner().getId();
            if (playerId == 1) {
                cityColor = Color.rgb(250, 100, 100); // Red
            } else if (playerId == 2) {
                cityColor = Color.rgb(150, 150, 250); // Blue
            } else {
                cityColor = Color.rgb(100, 250, 100); // Green
            }
            size = 5;
        }

        // Draw filled rectangle centered on hex
        gc.setFill(cityColor);
        gc.fillRect(center[0] - size, center[1] - size, size * 2, size * 2);

        // Draw black border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(center[0] - size, center[1] - size, size * 2, size * 2);

        // Draw city name with production (matching Swing version)
        String name = city.getName();
        if (name == null) {
            name = "City";
        }
        com.developingstorm.games.sad.Type production = city.getProduction();
        if (production != null) {
            name = name + " (" + production.getAbr() + ")";
        }

        gc.setFont(Font.font("Dialog", 12));
        gc.setFill(Color.BLACK);
        gc.setTextAlign(TextAlignment.CENTER);

        // Calculate text bounds for proper positioning
        javafx.scene.text.Text text = new javafx.scene.text.Text(name);
        text.setFont(gc.getFont());
        double textHeight = text.getLayoutBounds().getHeight();

        gc.fillText(name, center[0], center[1] + textHeight + (textHeight / 2));
    }

    /**
     * Draw a unit with actual icon images.
     */
    private void drawUnit(Unit unit) {
        double[] center = getHexCenter(unit.getLocation());

        // Get player color for background
        Color playerColor;
        int playerId = unit.getOwner().getId();
        if (playerId == 1) {
            playerColor = Color.rgb(250, 100, 100); // Red
        } else if (playerId == 2) {
            playerColor = Color.rgb(150, 150, 250); // Blue
        } else {
            playerColor = Color.rgb(100, 250, 100); // Green
        }

        // Check if selected
        Unit selected = query.getSelectedUnit();
        boolean isSelected = selected != null && selected.id == unit.id;

        // Draw colored background square for player identification
        double bgSize = 28;
        gc.setFill(playerColor);
        gc.fillRect(
            center[0] - bgSize / 2,
            center[1] - bgSize / 2,
            bgSize,
            bgSize
        );

        // Draw border with movement highlight
        boolean hasMoves = unit.life().hasMoves() && !unit.inSentryMode();
        if (hasMoves) {
            // Yellow highlight for units with available moves
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(2.5);
        } else {
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(1);
        }
        gc.strokeRect(
            center[0] - bgSize / 2,
            center[1] - bgSize / 2,
            bgSize,
            bgSize
        );

        // Draw unit icon on top (with status - sentry, loaded, etc.)
        javafx.scene.image.Image unitImage =
            terrainImages.getUnitImageWithStatus(unit);
        if (unitImage != null) {
            double iconSize = 24;
            gc.drawImage(
                unitImage,
                center[0] - iconSize / 2,
                center[1] - iconSize / 2,
                iconSize,
                iconSize
            );
        }

        // Draw anchor overlay for sea units in sentry mode
        if (
            unit.inSentryMode() &&
            unit.getTravel() == com.developingstorm.games.sad.Travel.SEA
        ) {
            javafx.scene.image.Image anchorImage =
                terrainImages.getAnchorImage();
            if (anchorImage != null) {
                double iconSize = 24;
                gc.drawImage(
                    anchorImage,
                    center[0] - iconSize / 2,
                    center[1] - iconSize / 2,
                    iconSize,
                    iconSize
                );
            }
        }

        // Draw fuel indicator for air units
        if (unit.getTravel() == com.developingstorm.games.sad.Travel.AIR) {
            int maxFuel = unit.life().getMaxFuel();
            if (maxFuel > 0) {
                int currentFuel = unit.life().getFuel();

                // Draw 4 small lines representing fuel quarters
                // All white = full fuel, all black = 1 or less fuel remaining
                // Position in lower-left corner, inset by 1 pixel
                double lineStartX = center[0] - 11; // Left edge of tile (12 - 1)
                double lineStartY = center[1] + 9; // Bottom edge of tile (12 - 3 line length)
                double lineLength = 3;

                for (int i = 0; i < 4; i++) {
                    // Calculate which quarter this line represents
                    float quarterThreshold = (maxFuel * (4 - i)) / 4.0f;

                    // Line is white if fuel is above this quarter, black if below
                    if (currentFuel >= quarterThreshold) {
                        gc.setStroke(Color.WHITE);
                    } else {
                        gc.setStroke(Color.BLACK);
                    }

                    // Draw vertical line
                    gc.setLineWidth(1);
                    double x = lineStartX + i;
                    gc.strokeLine(x, lineStartY, x, lineStartY + lineLength);
                }
            }
        }

        // Selection is now handled by animated selection sprite
        // (no static drawing here)
    }

    /**
     * Get terrain color for a hex.
     * Matches colors similar to the GIF terrain images used in Swing version.
     */
    private Color getTerrainColor(BoardHex hex) {
        Board board = query.getBoard();
        Location loc = hex.getLocation();
        int terrain = board.getTerrain(loc);

        if (terrain == 0) {
            // Water - darker blue to match water.gif appearance
            return Color.rgb(65, 105, 225); // Royal blue
        } else {
            // Land - earthy green/tan to match land.gif appearance
            return Color.rgb(194, 178, 128); // Tan/beige
        }
    }

    /**
     * Get the center point of a hex.
     */
    public double[] getHexCenter(Location loc) {
        double x2;
        if (loc.getY() % 2 != 0) x2 = HEX_HALF_WIDTH;
        else x2 = 0;

        double centerX = x2 + loc.getX() * HEX_WIDTH + HEX_HALF_WIDTH;
        double centerY = loc.getY() * HEX_HEIGHT + HEX_PEAK + HEX_SIDE / 2;

        return new double[] { centerX, centerY };
    }

    /**
     * Convert pixel coordinates to hex location.
     * Returns null if coordinates are outside the board.
     */
    private Location pixelToHex(double px, double py) {
        // Approximate conversion - find closest hex center
        int estY = (int) (py / HEX_HEIGHT);
        int estX;

        if (estY % 2 != 0) {
            estX = (int) ((px - HEX_HALF_WIDTH) / HEX_WIDTH);
        } else {
            estX = (int) (px / HEX_WIDTH);
        }

        Board board = query.getBoard();

        // If estimated position is way outside bounds, return null early
        if (
            estX < -1 ||
            estX > board.getWidth() ||
            estY < -1 ||
            estY > board.getHeight()
        ) {
            return null;
        }

        // Search nearby hexes for closest match
        Location closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int testX = estX + dx;
                int testY = estY + dy;

                if (
                    testX >= 0 &&
                    testX < board.getWidth() &&
                    testY >= 0 &&
                    testY < board.getHeight()
                ) {
                    Location testLoc = Location.get(testX, testY);
                    if (testLoc != null) {
                        double[] center = getHexCenter(testLoc);
                        double dist = Math.sqrt(
                            Math.pow(px - center[0], 2) +
                                Math.pow(py - center[1], 2)
                        );

                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = testLoc;
                        }
                    }
                }
            }
        }

        return closest;
    }

    /**
     * Handle mouse pressed events.
     * Delegates to the current mode via the mode manager.
     */
    private void handleMousePressed(MouseEvent event) {
        Location location = pixelToHex(event.getX(), event.getY());

        if (location != null && modeManager != null) {
            modeManager.delegateMousePressed(event, location);
        }
    }

    /**
     * Handle mouse released events.
     * Delegates to the current mode via the mode manager.
     */
    private void handleMouseReleased(MouseEvent event) {
        Location location = pixelToHex(event.getX(), event.getY());

        if (location != null && modeManager != null) {
            modeManager.delegateMouseReleased(event, location);
        }
    }

    /**
     * Handle mouse dragged events.
     * Delegates to the current mode via the mode manager.
     */
    private void handleMouseDragged(MouseEvent event) {
        Location location = pixelToHex(event.getX(), event.getY());

        if (location != null && modeManager != null) {
            modeManager.delegateMouseDragged(event, location);
        }
    }

    /**
     * Handle mouse moved events.
     * Delegates to the current mode via the mode manager.
     */
    private void handleMouseMoved(MouseEvent event) {
        Location location = pixelToHex(event.getX(), event.getY());
        if (location != null) {
            controller.trackLocation(location);

            if (modeManager != null) {
                modeManager.delegateMouseMoved(event, location);
            }
        }
    }

    /**
     * Handle context menu (right-click) events.
     * Matches Swing GameCommander.showPopup behavior.
     */
    private void handleContextMenu(ContextMenuEvent event) {
        Location location = pixelToHex(event.getX(), event.getY());

        // Ignore context menu if outside board
        if (location == null) {
            return;
        }

        Unit unitAtLocation = query.getUnitAtLocation(location);
        City cityAtLocation = query.getCityAtLocation(location);

        // Show unit orders menu if right-clicked on a unit
        if (unitAtLocation != null) {
            ContextMenu unitMenu = createUnitOrdersMenu(
                unitAtLocation,
                cityAtLocation
            );
            unitMenu.show(canvas, event.getScreenX(), event.getScreenY());
            event.consume();
        }
        // Show city menu if right-clicked on a city (and no unit there)
        else if (
            cityAtLocation != null &&
            cityAtLocation.getOwner() == query.getCurrentPlayer()
        ) {
            ContextMenu cityMenu = createCityMenu(cityAtLocation);
            cityMenu.show(canvas, event.getScreenX(), event.getScreenY());
            event.consume();
        }
    }

    /**
     * Create unit orders context menu.
     * Matches Swing OrderMenuBuilder.
     */
    private ContextMenu createUnitOrdersMenu(
        Unit unit,
        City cityAtSameLocation
    ) {
        ContextMenu menu = new ContextMenu();

        // Clear
        MenuItem clearItem = new MenuItem("Clear");
        clearItem.setOnAction(e -> {
            controller.selectUnit(null);
            refresh();
        });
        menu.getItems().add(clearItem);

        // Sentry/Load
        MenuItem sentryItem = new MenuItem("Sentry/Load");
        sentryItem.setOnAction(e -> {
            // Use proper command pattern for sentry operations
            if (unit.inSentryMode()) {
                // Clear orders by issuing a skip turn order
                controller.issueOrder(
                    unit,
                    new com.developingstorm.games.sad.orders.SkipTurn(
                        query.getGame(),
                        unit
                    )
                );
            } else {
                // Put in sentry mode
                controller.issueOrder(
                    unit,
                    new com.developingstorm.games.sad.orders.Sentry(
                        query.getGame(),
                        unit
                    )
                );
            }
            controller.resumeGame(unit);
            refresh();
        });
        menu.getItems().add(sentryItem);

        // Unload
        MenuItem unloadItem = new MenuItem("Unload");
        unloadItem.setOnAction(e -> {
            // Use proper command pattern for unload
            controller.issueOrder(
                unit,
                new com.developingstorm.games.sad.orders.Unload(
                    query.getGame(),
                    unit
                )
            );
            controller.resumeGame(unit);
            refresh();
        });
        menu.getItems().add(unloadItem);

        // Move (placeholder - would need mode switching)
        MenuItem moveItem = new MenuItem("Move");
        moveItem.setDisable(true); // TODO: Implement move mode
        menu.getItems().add(moveItem);

        // Explore
        MenuItem exploreItem = new MenuItem("Explore");
        exploreItem.setOnAction(e -> {
            // Use proper command pattern for explore
            controller.issueOrder(
                unit,
                new com.developingstorm.games.sad.orders.Explore(
                    query.getGame(),
                    unit
                )
            );
            controller.resumeGame(unit);
            refresh();
        });
        menu.getItems().add(exploreItem);

        // Head Home
        MenuItem headHomeItem = new MenuItem("Head Home");
        headHomeItem.setDisable(true); // TODO: Implement head home
        menu.getItems().add(headHomeItem);

        // Patrol
        MenuItem patrolItem = new MenuItem("Define Patrol...");
        patrolItem.setOnAction(e -> {
            enterPatrolMode(unit);
        });
        menu.getItems().add(patrolItem);

        // Bombard (for battleships and cruisers)
        if (unit.isBattleship() || unit.isCruiser()) {
            MenuItem bombardItem = new MenuItem("Bombard...");
            bombardItem.setDisable(true); // TODO: Implement attack mode
            menu.getItems().add(bombardItem);
        }

        // Escort (for sea units)
        if (unit.getTravel() == Travel.SEA) {
            MenuItem escortItem = new MenuItem("Escort...");
            escortItem.setDisable(true); // TODO: Implement escort mode
            menu.getItems().add(escortItem);
        }

        // Add city submenu if unit is at a city
        if (
            cityAtSameLocation != null &&
            cityAtSameLocation.getOwner() == query.getCurrentPlayer()
        ) {
            menu.getItems().add(new SeparatorMenuItem());

            // Create simplified city menu as submenu
            javafx.scene.control.Menu citySubmenu =
                new javafx.scene.control.Menu("City");
            addCityMenuItems(citySubmenu, cityAtSameLocation);
            menu.getItems().add(citySubmenu);
        }

        return menu;
    }

    /**
     * Create city context menu.
     * Matches Swing CityMenuBuilder (simplified version).
     */
    private ContextMenu createCityMenu(City city) {
        ContextMenu menu = new ContextMenu();
        javafx.scene.control.Menu cityMenu = new javafx.scene.control.Menu(
            "City"
        );
        addCityMenuItems(cityMenu, city);

        // Add the city menu items directly to context menu
        menu.getItems().addAll(cityMenu.getItems());

        return menu;
    }

    /**
     * Add city menu items (production, paths, etc).
     * Matches Swing CityMenuBuilder.fillMenu().
     */
    private void addCityMenuItems(
        javafx.scene.control.Menu parentMenu,
        City city
    ) {
        com.developingstorm.games.sad.UnitStats stats = city
            .getOwner()
            .unitStats();

        // Production menu items with radio buttons
        javafx.scene.control.ToggleGroup productionGroup =
            new javafx.scene.control.ToggleGroup();

        // Always available units
        addProductionMenuItem(
            parentMenu,
            productionGroup,
            city,
            Type.INFANTRY,
            stats
        );
        addProductionMenuItem(
            parentMenu,
            productionGroup,
            city,
            Type.ARMOR,
            stats
        );
        addProductionMenuItem(
            parentMenu,
            productionGroup,
            city,
            Type.FIGHTER,
            stats
        );
        addProductionMenuItem(
            parentMenu,
            productionGroup,
            city,
            Type.BOMBER,
            stats
        );
        addProductionMenuItem(
            parentMenu,
            productionGroup,
            city,
            Type.CARGO,
            stats
        );

        // Naval units (only for coastal cities)
        if (city.isCoastal()) {
            addProductionMenuItem(
                parentMenu,
                productionGroup,
                city,
                Type.TRANSPORT,
                stats
            );
            addProductionMenuItem(
                parentMenu,
                productionGroup,
                city,
                Type.DESTROYER,
                stats
            );
            addProductionMenuItem(
                parentMenu,
                productionGroup,
                city,
                Type.SUBMARINE,
                stats
            );
            addProductionMenuItem(
                parentMenu,
                productionGroup,
                city,
                Type.CRUISER,
                stats
            );
            addProductionMenuItem(
                parentMenu,
                productionGroup,
                city,
                Type.BATTLESHIP,
                stats
            );
            addProductionMenuItem(
                parentMenu,
                productionGroup,
                city,
                Type.CARRIER,
                stats
            );
        }

        parentMenu.getItems().add(new SeparatorMenuItem());

        // Path setting menu items
        EdictGovernor governor = city.getGovernor();

        // Air path
        MenuItem airPathItem = new MenuItem(
            governor.hastAirPath() ? "Cancel Air Path" : "Set Air Path..."
        );
        airPathItem.setOnAction(e -> {
            if (governor.hastAirPath()) {
                query
                    .getGame()
                    .postAndRunGameAction(() -> governor.clearAirPath());
                // Rebuild path sprites to remove the cleared path
                boolean[] visibility = getPathVisibility();
                refreshPaths(
                    city.getOwner(),
                    visibility[0],
                    visibility[1],
                    visibility[2]
                );
                refresh();
            } else {
                enterPathMode(city, Travel.AIR);
            }
        });
        parentMenu.getItems().add(airPathItem);

        // Land path
        MenuItem landPathItem = new MenuItem(
            governor.hasLandPath() ? "Cancel Land Path" : "Set Land Path..."
        );
        landPathItem.setOnAction(e -> {
            if (governor.hasLandPath()) {
                query
                    .getGame()
                    .postAndRunGameAction(() -> governor.clearLandPath());
                // Rebuild path sprites to remove the cleared path
                boolean[] visibility = getPathVisibility();
                refreshPaths(
                    city.getOwner(),
                    visibility[0],
                    visibility[1],
                    visibility[2]
                );
                refresh();
            } else {
                enterPathMode(city, Travel.LAND);
            }
        });
        parentMenu.getItems().add(landPathItem);

        // Sea path (only for coastal cities)
        if (city.isCoastal()) {
            MenuItem seaPathItem = new MenuItem(
                governor.hasSeaPath() ? "Cancel Sea Path" : "Set Sea Path..."
            );
            seaPathItem.setOnAction(e -> {
                if (governor.hasSeaPath()) {
                    query
                        .getGame()
                        .postAndRunGameAction(() -> governor.clearSeaPath());
                    // Rebuild path sprites to remove the cleared path
                    boolean[] visibility = getPathVisibility();
                    refreshPaths(
                        city.getOwner(),
                        visibility[0],
                        visibility[1],
                        visibility[2]
                    );
                    refresh();
                } else {
                    enterPathMode(city, Travel.SEA);
                }
            });
            parentMenu.getItems().add(seaPathItem);
        }

        // Air patrol checkbox
        javafx.scene.control.CheckMenuItem airPatrolItem =
            new javafx.scene.control.CheckMenuItem("Air Patrol");
        airPatrolItem.setSelected(governor.hasAirPatrol());
        airPatrolItem.setOnAction(e -> {
            query
                .getGame()
                .postAndRunGameAction(() -> {
                    if (governor.hasAirPatrol()) {
                        governor.clearAirPatrol();
                    } else {
                        governor.setAirPatrol();
                    }
                });
            refresh();
        });
        parentMenu.getItems().add(airPatrolItem);

        // Auto sentry checkbox
        javafx.scene.control.CheckMenuItem autoSentryItem =
            new javafx.scene.control.CheckMenuItem("Automatic Sentry");
        autoSentryItem.setSelected(governor.hasAutoSentry());
        autoSentryItem.setOnAction(e -> {
            query
                .getGame()
                .postAndRunGameAction(() -> {
                    if (governor.hasAutoSentry()) {
                        governor.clearAutoSenty();
                    } else {
                        governor.setAutoSentry();
                    }
                });
            refresh();
        });
        parentMenu.getItems().add(autoSentryItem);

        parentMenu.getItems().add(new SeparatorMenuItem());

        // Units dialog
        MenuItem unitsItem = new MenuItem("Units...");
        unitsItem.setOnAction(e -> showCityUnitsDialog(city));
        parentMenu.getItems().add(unitsItem);
    }

    /**
     * Add a production menu item with radio button.
     */
    private void addProductionMenuItem(
        javafx.scene.control.Menu parentMenu,
        javafx.scene.control.ToggleGroup group,
        City city,
        Type type,
        com.developingstorm.games.sad.UnitStats stats
    ) {
        String label = String.format(
            "%s (%d/%d)",
            type.getName(),
            stats.getCount(type),
            stats.getProduction(type)
        );

        javafx.scene.control.RadioMenuItem item =
            new javafx.scene.control.RadioMenuItem(label);
        item.setToggleGroup(group);

        // Select if this is the current production
        if (city.getProduction() == type) {
            item.setSelected(true);
        }

        item.setOnAction(e -> {
            query.getGame().postAndRunGameAction(() -> city.produce(type));
            // Refresh the canvas to show updated production
            javafx.application.Platform.runLater(() -> refresh());
        });

        parentMenu.getItems().add(item);
    }

    /**
     * Show dialog to select units at a city.
     * Matches Swing CityDialog behavior.
     */
    private void showCityUnitsDialog(City city) {
        List<Unit> unitsAtCity = query.getUnitsAtLocation(city.getLocation());

        if (unitsAtCity == null || unitsAtCity.isEmpty()) {
            return;
        }

        CityUnitsDialog dialog = new CityUnitsDialog(city, unitsAtCity);
        dialog
            .showAndWait()
            .ifPresent(selectedUnits -> {
                if (selectedUnits != null && !selectedUnits.isEmpty()) {
                    // Show context menu for selected units
                    ContextMenu unitMenu = createMultiUnitOrdersMenu(
                        selectedUnits,
                        city
                    );

                    // Show menu at city location on map
                    double[] cityCenter = getHexCenter(city.getLocation());
                    unitMenu.show(canvas, cityCenter[0], cityCenter[1]);
                }
            });
    }

    /**
     * Create context menu for multiple selected units.
     * Similar to createUnitOrdersMenu but works with multiple units.
     */
    private ContextMenu createMultiUnitOrdersMenu(
        List<Unit> units,
        City cityAtSameLocation
    ) {
        // For simplicity, just use the first unit's menu for now
        // TODO: Could enhance to only show options valid for all units
        return createUnitOrdersMenu(units.get(0), cityAtSameLocation);
    }

    /**
     * Activate a hex location (select unit, view city, etc).
     * Matches Swing GameModeController activate behavior.
     * Public for use by mode classes.
     */
    public void activate(Location location) {
        List<Unit> unitsAtLocation = query.getUnitsAtLocation(location);
        City cityAtLocation = query.getCityAtLocation(location);

        if (unitsAtLocation != null && !unitsAtLocation.isEmpty()) {
            if (unitsAtLocation.size() == 1) {
                // Single unit - just select it
                controller.selectUnit(unitsAtLocation.get(0));
            } else {
                // Multiple units - cycle through them or show selection
                Unit currentlySelected = query.getSelectedUnit();

                // Find currently selected unit in the list
                int currentIndex = -1;
                if (
                    currentlySelected != null &&
                    currentlySelected.getLocation().equals(location)
                ) {
                    for (int i = 0; i < unitsAtLocation.size(); i++) {
                        if (unitsAtLocation.get(i).id == currentlySelected.id) {
                            currentIndex = i;
                            break;
                        }
                    }
                }

                // Select next unit in the list (cycle around)
                int nextIndex = (currentIndex + 1) % unitsAtLocation.size();
                controller.selectUnit(unitsAtLocation.get(nextIndex));
            }
        } else if (cityAtLocation != null) {
            // Track the city location
            controller.trackLocation(location);
        } else {
            // Empty hex - deselect
            controller.selectUnit(null);
        }
    }

    /**
     * Move a unit to a destination.
     * Creates and issues a move order for the unit.
     * Public for use by mode classes.
     */
    public void moveUnit(Unit unit, Location destination) {
        // Use proper command pattern for move
        System.out.println(
            "Issuing move order for " + unit.name + " to " + destination
        );

        // Create and issue move order via controller
        com.developingstorm.games.sad.orders.Move moveOrder =
            new com.developingstorm.games.sad.orders.Move(
                query.getGame(),
                unit,
                destination
            );

        controller.issueOrder(unit, moveOrder);
        controller.resumeGame(unit);

        System.out.println("Move order issued to " + unit.name);
    }

    /**
     * Show patrol paths for the given player's units.
     * Matches Swing version's showPatrolPaths method.
     */
    public void showPatrolPaths(Player player) {
        removeSprites(this.patrolPaths);

        if (player == null || player.isRobot()) {
            this.patrolPaths = null;
            return;
        }

        this.patrolPaths = new ArrayList<>();

        // Iterate through all units and find those with patrol orders
        for (Unit unit : this.game.units()) {
            if (unit.getOwner() != player) {
                continue;
            }

            // Skip dead units
            if (unit.isDead()) {
                continue;
            }

            Order order = unit.getOrder();
            if (order != null && order.getType() == OrderType.PATROL) {
                Patrol patrol = (Patrol) order;
                List<Location> waypoints = patrol.getWaypoints();

                if (waypoints.size() < 2) {
                    continue;
                }

                // Choose color based on unit travel type (matching Swing)
                Color lineColor;
                if (unit.getTravel() == Travel.AIR) {
                    lineColor = Color.CYAN;
                } else if (unit.getTravel() == Travel.SEA) {
                    lineColor = Color.BLUE;
                } else {
                    lineColor = Color.GREEN;
                }

                // Draw lines between consecutive waypoints
                for (int i = 0; i < waypoints.size() - 1; i++) {
                    Location loc1 = waypoints.get(i);
                    Location loc2 = waypoints.get(i + 1);

                    double[] center1 = getHexCenter(loc1);
                    double[] center2 = getHexCenter(loc2);

                    FxLineSprite line = new FxLineSprite(lineColor, 1.0, false);
                    line.setLine(
                        center1[0],
                        center1[1],
                        center2[0],
                        center2[1]
                    );
                    this.patrolPaths.add(line);
                }
            }
        }

        // Show city air patrols
        Board board = query.getBoard();
        for (City city : board.getCities()) {
            if (city.getOwner() != player) {
                continue;
            }

            com.developingstorm.games.sad.edicts.CityAirPatrol cityAirPatrol =
                city.getGovernor().getCityAirPatrol();

            if (cityAirPatrol != null) {
                List<Location> waypoints = cityAirPatrol.getWaypoints();

                if (waypoints.size() < 2) {
                    continue;
                }

                // Use CYAN color to match air unit path color
                Color lineColor = Color.CYAN;

                // Draw dashed lines between consecutive waypoints
                for (int i = 0; i < waypoints.size() - 1; i++) {
                    Location loc1 = waypoints.get(i);
                    Location loc2 = waypoints.get(i + 1);

                    double[] center1 = getHexCenter(loc1);
                    double[] center2 = getHexCenter(loc2);

                    FxLineSprite line = new FxLineSprite(lineColor, 1.0, true); // Dashed
                    line.setLine(
                        center1[0],
                        center1[1],
                        center2[0],
                        center2[1]
                    );
                    this.patrolPaths.add(line);
                }
            }
        }

        addSprites(this.patrolPaths);
    }

    /**
     * Refresh and display production paths for the given player.
     * Clears old path sprites and creates new ones based on current city edicts.
     */
    public void refreshPaths(
        Player player,
        boolean air,
        boolean ground,
        boolean sea
    ) {
        removeSprites(this.airPaths);
        removeSprites(this.groundPaths);
        removeSprites(this.seaPaths);

        if (player == null || player.isRobot()) {
            this.airPaths = null;
            this.groundPaths = null;
            this.seaPaths = null;
            return;
        }

        this.airPaths = new ArrayList<>();
        this.groundPaths = new ArrayList<>();
        this.seaPaths = new ArrayList<>();

        List<City> cities = player.getCities();
        for (City c : cities) {
            City airPath = c.getGovernor().getAirPathDest();
            City groundPath = c.getGovernor().getLandPathDest();
            City seaPath = c.getGovernor().getSeaPathDest();

            double[] cityCenter = getHexCenter(c.getLocation());

            if (air && airPath != null) {
                double[] destCenter = getHexCenter(airPath.getLocation());
                FxArrowSprite arrow = new FxArrowSprite(Color.GRAY);
                arrow.setArrow(
                    cityCenter[0],
                    cityCenter[1],
                    destCenter[0],
                    destCenter[1]
                );
                this.airPaths.add(arrow);
            }

            if (sea && seaPath != null) {
                double[] destCenter = getHexCenter(seaPath.getLocation());
                FxArrowSprite arrow = new FxArrowSprite(Color.BLUE);
                arrow.setArrow(
                    cityCenter[0],
                    cityCenter[1],
                    destCenter[0],
                    destCenter[1]
                );
                this.seaPaths.add(arrow);
            }

            if (ground && groundPath != null) {
                double[] destCenter = getHexCenter(groundPath.getLocation());
                FxArrowSprite arrow = new FxArrowSprite(
                    Color.GREEN.darker().darker()
                );
                arrow.setArrow(
                    cityCenter[0],
                    cityCenter[1],
                    destCenter[0],
                    destCenter[1]
                );
                this.groundPaths.add(arrow);
            }
        }

        addSprites(this.airPaths);
        addSprites(this.groundPaths);
        addSprites(this.seaPaths);
    }

    /**
     * Enter path mode for setting city production paths.
     * Switches to PATH mode and initializes it with the city and travel type.
     * @param city The origin city
     * @param travel The type of path (LAND, SEA, or AIR)
     */
    public void enterPathMode(City city, Travel travel) {
        // Get the PathMode instance and configure it
        com.developingstorm.games.sad.fx.modes.PathMode pathMode =
            (com.developingstorm.games.sad.fx.modes.PathMode) modeManager.getMode(
                com.developingstorm.games.sad.fx.UIMode.PATHS
            );

        pathMode.setOrigin(city, travel);

        // Switch to PATHS mode
        modeManager.switchMode(com.developingstorm.games.sad.fx.UIMode.PATHS);
    }

    /**
     * Enter patrol mode for a unit.
     * Shows line from unit following cursor to set patrol destination.
     *
     * @param unit The unit to set patrol for
     */
    public void enterPatrolMode(Unit unit) {
        // Get the PatrolMode instance and configure it
        com.developingstorm.games.sad.fx.modes.PatrolMode patrolMode =
            (com.developingstorm.games.sad.fx.modes.PatrolMode) modeManager.getMode(
                com.developingstorm.games.sad.fx.UIMode.PATROL
            );

        patrolMode.setPatrolUnit(unit);

        // Switch to PATROL mode
        modeManager.switchMode(com.developingstorm.games.sad.fx.UIMode.PATROL);
    }

    /**
     * Center the viewport on a specific location.
     * This is called when tracking units during movement.
     */
    public void centerOnLocation(Location location) {
        if (location == null) return;

        double[] center = getHexCenter(location);
        double centerX = center[0];
        double centerY = center[1];

        // Get the scroll pane from parent hierarchy
        javafx.scene.Parent parent = getParent();
        while (
            parent != null &&
            !(parent instanceof javafx.scene.control.ScrollPane)
        ) {
            parent = parent.getParent();
        }

        if (parent instanceof javafx.scene.control.ScrollPane) {
            javafx.scene.control.ScrollPane scrollPane =
                (javafx.scene.control.ScrollPane) parent;

            // Calculate scroll position to center the location
            double hValue =
                (centerX - scrollPane.getViewportBounds().getWidth() / 2) /
                (getWidth() - scrollPane.getViewportBounds().getWidth());
            double vValue =
                (centerY - scrollPane.getViewportBounds().getHeight() / 2) /
                (getHeight() - scrollPane.getViewportBounds().getHeight());

            // Clamp values between 0 and 1
            hValue = Math.max(0, Math.min(1, hValue));
            vValue = Math.max(0, Math.min(1, vValue));

            scrollPane.setHvalue(hValue);
            scrollPane.setVvalue(vValue);
        }
    }
}
