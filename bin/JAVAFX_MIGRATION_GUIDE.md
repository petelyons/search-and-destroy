# JavaFX Migration Guide

## Quick Start for JavaFX Development

This guide shows how to build a JavaFX UI using the new event-driven architecture.

## Three Core Components

### 1. GameController - Issue Commands
```java
GameController controller = new GameControllerImpl(game);

// Select a unit
controller.selectUnit(unit);

// Issue an order
controller.issueOrder(unit, new Move(destination));

// Resume game after orders
controller.resumeGame(unit);

// Pause the game
controller.pauseGame();

// Track location (center map)
controller.trackLocation(location);
```

### 2. GameQueryService - Read State
```java
GameQueryService query = new GameQueryServiceImpl(game);

// Get current state
GameState state = query.getGameState();  // RUNNING, AWAITING_ORDERS, etc.
Unit selected = query.getSelectedUnit();
Player current = query.getCurrentPlayer();
int turn = query.getTurn();

// Query locations
List<Unit> units = query.getUnitsAtLocation(location);
Unit unit = query.getUnitAtLocation(location);
City city = query.getCityAtLocation(location);
boolean hasCity = query.isCity(location);

// Get all entities
Player[] players = query.getPlayers();
List<Unit> allUnits = query.getAllUnits();
Board board = query.getBoard();
```

### 3. GameEventBus - React to Changes
```java
game.getEventBus().addListener(new GameEventListener() {
    @Override
    public void onGameEvent(GameEvent event) {
        // Already on JavaFX Application Thread (EDT)
        switch (event.getEventType()) {
            case UNIT_SELECTED:
                UnitSelectedEvent e = (UnitSelectedEvent) event;
                updateUnitDisplay(e.getUnit());
                break;
                
            case COMBAT_RESOLVED:
                CombatResolvedEvent e = (CombatResolvedEvent) event;
                showCombatAnimation(e.getLocation());
                break;
                
            case WAITING_FOR_ORDERS:
                WaitingForOrdersEvent e = (WaitingForOrdersEvent) event;
                enableOrderUI();
                break;
                
            case MAP_UPDATED:
                refreshMap();
                break;
        }
    }
    
    @Override
    public GameEventType[] getInterestedEventTypes() {
        // Filter events - only receive what you need
        return new GameEventType[] {
            GameEventType.UNIT_SELECTED,
            GameEventType.COMBAT_RESOLVED,
            GameEventType.WAITING_FOR_ORDERS,
            GameEventType.MAP_UPDATED
        };
    }
});
```

## Available Event Types

```java
public enum GameEventType {
    // Unit events
    UNIT_SELECTED,      // Unit selected/deselected
    UNIT_MOVED,         // Unit moved (not implemented yet)
    UNIT_DESTROYED,     // Unit killed (not implemented yet)
    UNIT_CREATED,       // Unit produced (not implemented yet)
    
    // Combat events
    COMBAT_RESOLVED,    // Combat completed
    
    // Turn events
    TURN_STARTED,       // New turn began (not implemented yet)
    TURN_ENDED,         // Turn ended (not implemented yet)
    
    // Game flow events
    GAME_PAUSED,        // Game paused (not implemented yet)
    GAME_RESUMED,       // Game resumed (not implemented yet)
    WAITING_FOR_ORDERS, // Game waiting for player input
    
    // City events
    CITY_CAPTURED,      // City ownership changed (not implemented yet)
    CITY_PRODUCTION_CHANGED, // City production changed (not implemented yet)
    
    // Map events
    MAP_UPDATED,        // Map needs refresh
    
    // General notification
    MESSAGE             // Generic message (not implemented yet)
}
```

## JavaFX UI Structure Example

```java
public class GameView extends BorderPane {
    private final GameController controller;
    private final GameQueryService query;
    private final Game game;
    
    private MapCanvas mapCanvas;
    private UnitInfoPanel unitInfo;
    private StatusBar statusBar;
    
    public GameView(Game game) {
        this.game = game;
        this.controller = new GameControllerImpl(game);
        this.query = new GameQueryServiceImpl(game);
        
        // Build UI
        mapCanvas = new MapCanvas(query);
        unitInfo = new UnitInfoPanel();
        statusBar = new StatusBar();
        
        setCenter(mapCanvas);
        setRight(unitInfo);
        setBottom(statusBar);
        
        // Register event listener
        registerEventHandlers();
        
        // Set up user interactions
        setupMouseHandlers();
        setupKeyboardHandlers();
    }
    
    private void registerEventHandlers() {
        game.getEventBus().addListener(new GameEventListener() {
            @Override
            public void onGameEvent(GameEvent event) {
                Platform.runLater(() -> {  // JavaFX equivalent of SwingUtilities.invokeLater
                    handleEvent(event);
                });
            }
        });
    }
    
    private void handleEvent(GameEvent event) {
        switch (event.getEventType()) {
            case UNIT_SELECTED:
                UnitSelectedEvent use = (UnitSelectedEvent) event;
                unitInfo.setUnit(use.getUnit());
                mapCanvas.highlightUnit(use.getUnit());
                break;
                
            case COMBAT_RESOLVED:
                CombatResolvedEvent cre = (CombatResolvedEvent) event;
                mapCanvas.showExplosion(cre.getLocation());
                break;
                
            case WAITING_FOR_ORDERS:
                statusBar.setStatus("Waiting for orders...");
                break;
                
            case MAP_UPDATED:
                mapCanvas.repaint();
                break;
        }
    }
    
    private void setupMouseHandlers() {
        mapCanvas.setOnMouseClicked(event -> {
            Location clicked = mapCanvas.getLocationAt(event.getX(), event.getY());
            
            // Query what's at the location
            Unit unit = query.getUnitAtLocation(clicked);
            City city = query.getCityAtLocation(clicked);
            
            if (unit != null) {
                // Select the unit
                controller.selectUnit(unit);
            } else if (city != null) {
                // Show city info
                unitInfo.setCity(city);
            }
        });
    }
    
    private void setupKeyboardHandlers() {
        setOnKeyPressed(event -> {
            Unit selected = query.getSelectedUnit();
            if (selected == null) return;
            
            switch (event.getCode()) {
                case M:
                    // Move order
                    showMoveDialog(selected);
                    break;
                case A:
                    // Attack order
                    showAttackDialog(selected);
                    break;
                case S:
                    // Skip turn
                    controller.issueOrder(selected, new Skip());
                    controller.resumeGame(selected);
                    break;
            }
        });
    }
    
    private void showMoveDialog(Unit unit) {
        // Show dialog to pick destination
        Location dest = pickLocationDialog();
        if (dest != null) {
            controller.issueOrder(unit, new Move(dest));
            controller.resumeGame(unit);
        }
    }
}
```

## Key Differences from Swing

### Threading
- **Swing**: Uses EDT (Event Dispatch Thread)
- **JavaFX**: Uses Application Thread
- **Event Bus**: Automatically marshals to correct thread
  - For Swing: uses `SwingUtilities.invokeLater()`
  - For JavaFX: you'll need to use `Platform.runLater()` wrapper

### Binding
JavaFX has built-in property binding. You can create observable properties from game state:

```java
public class GameStateProperties {
    private final GameQueryService query;
    private final ObjectProperty<Unit> selectedUnit = new SimpleObjectProperty<>();
    private final IntegerProperty turn = new SimpleIntegerProperty();
    private final ObjectProperty<GameState> gameState = new SimpleObjectProperty<>();
    
    public GameStateProperties(Game game) {
        this.query = new GameQueryServiceImpl(game);
        
        // Listen to events and update properties
        game.getEventBus().addListener(event -> {
            Platform.runLater(() -> {
                switch (event.getEventType()) {
                    case UNIT_SELECTED:
                        selectedUnit.set(query.getSelectedUnit());
                        break;
                    case TURN_STARTED:
                        turn.set(query.getTurn());
                        break;
                }
                // Always update game state
                gameState.set(query.getGameState());
            });
        });
    }
    
    public ObjectProperty<Unit> selectedUnitProperty() { return selectedUnit; }
    public IntegerProperty turnProperty() { return turn; }
    public ObjectProperty<GameState> gameStateProperty() { return gameState; }
}

// In UI code
GameStateProperties props = new GameStateProperties(game);
turnLabel.textProperty().bind(props.turnProperty().asString("Turn: %d"));
```

## Migration Strategy

### Phase 1: Proof of Concept
1. Create minimal JavaFX window
2. Get `GameController`, `GameQueryService`, `GameEventBus` from game
3. Display basic map (just hexes)
4. Handle mouse clicks (select units)
5. Show unit info when selected
6. Verify events firing

### Phase 2: Core Gameplay
1. Implement all order dialogs (Move, Attack, etc.)
2. Handle keyboard shortcuts
3. Show unit paths
4. Display cities and production
5. Show player info and turn counter
6. Implement scrolling/zooming

### Phase 3: Polish
1. Combat animations
2. Unit movement animations
3. Sound effects
4. Battle history panel
5. Save/load dialogs
6. Settings/preferences
7. Help/documentation

### Phase 4: Cleanup
1. Remove Swing UI code
2. Remove legacy GameListener interface
3. Convert all game events to event bus
4. Add more event types as needed

## Important Notes

### Don't Modify Game State from UI Thread
```java
// BAD - modifying game state directly from UI
unit.setLocation(newLocation);  // Don't do this!

// GOOD - queue command for game thread
controller.issueOrder(unit, new Move(newLocation));
```

### Query State Safely
```java
// Game state is volatile - safe to read from any thread
Unit u = query.getSelectedUnit();  // OK from UI thread

// But be aware state can change between reads
Unit u1 = query.getSelectedUnit();
// ... some time passes ...
Unit u2 = query.getSelectedUnit();  // Might be different!
```

### Event Handlers Run on UI Thread
```java
// This is automatically on JavaFX Application Thread
public void onGameEvent(GameEvent event) {
    // Safe to update UI directly here
    label.setText("Unit selected!");
    
    // Don't do long-running work here - will freeze UI
    // If needed, offload to background thread
}
```

## Testing Both UIs Simultaneously

You can run both Swing and JavaFX UIs at the same time for comparison:

```java
// Start Swing UI (existing)
SaDFrame swingFrame = new SaDFrame();
Game game = swingFrame.getGame();

// Start JavaFX UI (new)
Platform.runLater(() -> {
    GameView fxView = new GameView(game);
    Stage stage = new Stage();
    stage.setScene(new Scene(fxView, 800, 600));
    stage.setTitle("Search and Destroy (JavaFX)");
    stage.show();
});

// Both UIs will receive the same events!
// Both UIs control the same game!
```

## Example: Complete Unit Info Panel

```java
public class UnitInfoPanel extends VBox {
    private final Label nameLabel = new Label();
    private final Label typeLabel = new Label();
    private final Label healthLabel = new Label();
    private final Label locationLabel = new Label();
    private final Label orderLabel = new Label();
    private final Button moveButton = new Button("Move");
    private final Button attackButton = new Button("Attack");
    
    private GameController controller;
    private Unit currentUnit;
    
    public UnitInfoPanel() {
        getChildren().addAll(
            new Label("Unit Info"),
            nameLabel,
            typeLabel,
            healthLabel,
            locationLabel,
            orderLabel,
            new HBox(moveButton, attackButton)
        );
        
        moveButton.setOnAction(e -> showMoveDialog());
        attackButton.setOnAction(e -> showAttackDialog());
    }
    
    public void setController(GameController controller) {
        this.controller = controller;
    }
    
    public void setUnit(Unit unit) {
        this.currentUnit = unit;
        
        if (unit == null) {
            nameLabel.setText("No unit selected");
            typeLabel.setText("");
            healthLabel.setText("");
            locationLabel.setText("");
            orderLabel.setText("");
            moveButton.setDisable(true);
            attackButton.setDisable(true);
        } else {
            nameLabel.setText(unit.getName());
            typeLabel.setText(unit.getType().toString());
            healthLabel.setText("Health: " + unit.life().hits);
            locationLabel.setText("Location: " + unit.getLocation());
            orderLabel.setText("Order: " + 
                (unit.getOrder() != null ? unit.getOrder().getType() : "None"));
            moveButton.setDisable(false);
            attackButton.setDisable(false);
        }
    }
    
    private void showMoveDialog() {
        if (currentUnit == null || controller == null) return;
        
        // Show dialog to pick destination
        // This would be a custom dialog in real implementation
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Move Unit");
        dialog.setHeaderText("Enter destination (x,y)");
        dialog.showAndWait().ifPresent(input -> {
            // Parse input and create move order
            Location dest = parseLocation(input);
            if (dest != null) {
                controller.issueOrder(currentUnit, new Move(dest));
                controller.resumeGame(currentUnit);
            }
        });
    }
    
    private void showAttackDialog() {
        // Similar to move dialog
    }
    
    private Location parseLocation(String input) {
        // Parse "x,y" format
        String[] parts = input.split(",");
        if (parts.length == 2) {
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                return new Location(x, y);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
```

## Summary

The new architecture makes JavaFX migration straightforward:

1. **Commands**: Use `GameController` to issue commands
2. **Queries**: Use `GameQueryService` to read state  
3. **Events**: Listen to `GameEventBus` for updates
4. **No Direct Access**: Never modify game state directly from UI
5. **Threading**: Event bus handles all thread marshalling

This clean separation means your JavaFX UI can be developed independently without touching game logic, and both UIs can coexist during migration.
