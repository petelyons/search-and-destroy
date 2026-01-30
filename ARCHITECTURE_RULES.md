# Architecture Rules - Search and Destroy Game

> **Purpose**: Concise reference for LLMs working with this codebase. See ARCHITECTURE_REFERENCE.md for detailed explanations.

---

## Critical Rules

### Thread Safety
1. **Game Thread owns all mutable state** - never mutate game state from UI thread
2. **UI mutations MUST use GameCommand** - submit via `gameController.submitCommand(command)`
3. **Game notifications MUST use GameEvent** - publish via `eventBus.publish(event)`
4. **Mark cross-thread fields `volatile`** - for visibility guarantees
5. **Never block threads** - no wait/notify, use command queue + polling

### Communication
```java
// UI → Game: Commands (JavaFX)
controller.issueOrder(unit, order);        // For unit orders
controller.submitCommand(new MyCommand()); // For custom commands

// UI → Game: Commands (Swing - DEPRECATED, use JavaFX patterns)
game.submitCommand(new MyCommand());

// Game → UI: Events  
eventBus.publish(new MyEvent(data));

// UI reads state: Query Service
Unit unit = query.getSelectedUnit();
Board board = query.getBoard();
```

### JavaFX vs Swing
- **JavaFX**: Active development, uses `GameController` interface
- **Swing**: DEPRECATED - files in `src/main/java/.../ui/` are maintenance-only
- **All new code**: Implement in JavaFX (`src/main/java/.../fx/`)

---

## Key Patterns

### 1. Command Pattern (UI → Game mutations)
```java
public class MyCommand implements GameCommand {
    private final long capturedId;  // Capture at construction
    
    public MyCommand(long id) { this.capturedId = id; }
    
    @Override
    public void execute(Game game) {
        // Execute on game thread
        game.doSomething(capturedId);
    }
}
```
**Location**: `com.developingstorm.games.sad.commands/`

### 2. Event Pattern (Game → UI notifications)
```java
public class MyEvent extends GameEvent {
    private final Object snapshotData;  // Immutable snapshot
    
    public MyEvent(Object data) {
        super(GameEventType.MY_EVENT);
        this.snapshotData = data;
    }
    
    public Object getData() { return snapshotData; }
}
```
**Location**: `com.developingstorm.games.sad.events/`

### 3. Manager Pattern (Subsystem responsibilities)
```java
public class MyManager {
    private final Game game;
    
    public MyManager(Game game) { this.game = game; }
    
    public void performAction(Unit unit) {
        // Manager logic
        game.getEventBus().publish(new ActionEvent(unit.id));
    }
}
```
**Examples**: UnitManager, CityManager, CombatResolver, PathCalculator, MovementResolver

### 4. Controller Pattern (UI boundary)
```java
// GameController - UI's ONLY way to control game
public interface GameController {
    void submitCommand(GameCommand command);
    void selectUnit(Unit unit);
    void resumeGame();
    // ... high-level operations
}

// GameQueryService - UI's way to READ state
public interface GameQueryService {
    Unit getSelectedUnit();
    Player getCurrentPlayer();
    // ... read-only queries
}
```

### 5. Strategy Pattern (AI)
```java
public interface IBrain {
    void startNewTurn(Player player);
    Order getOrders(Unit unit);
    Type getProduction(City city);
}
```
**Implementation**: RobotBrain → General → Specialized Captains

### 6. Template Method Pattern (Orders)
```java
public abstract class Order {
    protected Unit unit;
    public abstract OrderResponse execute();
    public abstract String getDescription();
}
```
**Location**: `com.developingstorm.games.sad.orders/`

### 7. Mode Pattern (UI interaction modes)
```java
public interface ModeController {
    void handleHexClick(Location loc);
    void handleRightClick(Location loc);
    void activate();
    void deactivate();
}
```
**Rule**: Delegate to mode, NO `if (mode == X)` conditionals in base UI

---

## State Management

### Game State Machine
- `RUNNING` → player needs orders → `AWAITING_ORDERS` → ResumeGameCommand → `RUNNING`
- Atomic transitions: `gameState.compareAndSet(expected, newState)`

### Concurrency Primitives
- `volatile` - simple field visibility (selectedUnit, currentPlayer, turn)
- `AtomicReference<GameState>` - atomic state transitions
- `ConcurrentLinkedQueue<GameCommand>` - lock-free command queue
- `ReadWriteLock` - many readers, single writer (UnitManager locations)
- `synchronized` - atomic multi-step operations

---

## Automatic Patterns

### MapUpdatedEvent Auto-Fire
After processing commands, Game automatically fires `MapUpdatedEvent` → UI repaints.
**Don't manually call `repaint()` from game logic.**

### Event Bus EDT Marshalling
GameEventBus automatically marshals events to EDT using `SwingUtilities.invokeLater()`.
**Don't manually wrap in invokeLater from game code.**

---

## Feature Development Flow

### Adding a Feature
1. Create `MyCommand implements GameCommand` (in commands/)
2. Create `MyEvent extends GameEvent` (in events/)
3. Add game logic (in Game or appropriate Manager)
4. Add controller method if needed (in GameController)
5. UI listens to events, submits commands

### Adding a UI Mode
1. Implement `ModeController` interface
2. Handle clicks via `gameController.submitCommand()`
3. Listen to events for state updates
4. Register in UIController mode switcher

### Adding AI Behavior
1. Implement `IBrain` interface, or
2. Extend RobotBrain/General hierarchy

---

## Anti-Patterns (Never Do This)

### ❌ Direct game mutation from UI
```java
// WRONG - JavaFX
controller.postAndResume(() -> {
    unit.assignOrder(order);  // Direct manipulation
});

// WRONG - Swing
game.postAndRunGameAction(() -> {
    unit.assignOrder(order);  // Direct manipulation
});

// RIGHT - JavaFX
controller.issueOrder(unit, order);
controller.resumeGame(unit);
```

### ❌ Wrapping commands in Runnables
```java
// WRONG
controller.postAndResume(() -> {
    SetPathCommand cmd = new SetPathCommand(city, dest, travel);
    cmd.execute(game);
});

// RIGHT
controller.postGameAction(() -> {
    SetPathCommand cmd = new SetPathCommand(city, dest, travel);
    cmd.execute(game);
});
controller.resumeGame(null);
```

### ❌ Game calling UI directly
```java
// WRONG
frame.repaint();
canvas.refresh();

// RIGHT
eventBus.publish(new MapUpdatedEvent());
// UI listens and updates automatically
```

### ❌ Blocking game thread
```java
// WRONG
synchronized (lock) { lock.wait(); }

// RIGHT
transitionState(RUNNING, AWAITING_ORDERS);
```

### ❌ Mode conditionals
```java
// WRONG
if (mode == PATHS) { handlePathsClick(); }

// RIGHT
currentMode.handleHexClick(loc);
```

### ❌ Non-volatile cross-thread field
```java
// WRONG
private Unit selectedUnit;

// RIGHT
private volatile Unit selectedUnit;
```

---

## Data Model

### Immutable Types
- `Location` - hex coordinate value object
- `GameEvent` implementations - snapshot data
- `GameCommand` implementations - captured at construction

### Ownership
- **Game** owns: managers, command queue, mutable state
- **Player** owns: units, cities, vision state
- **Unit** owns: order, life, carried units
- **Board** owns: terrain, continents, city locations

### Type System
- Enum-based: `Type`, `Travel`, `GameState`, `GameEventType`
- Store attributes in enum constants, not parallel maps

---

## Package Structure

```
com.developingstorm.games.sad/
├── commands/          # GameCommand implementations
├── events/           # GameEvent implementations  
├── controller/       # GameController, GameQueryService
├── orders/           # Order implementations
├── brain/            # IBrain implementations
├── fx/               # JavaFX UI (new)
├── ui/               # Swing UI (legacy)
│   └── controls/     # Mode commanders/controllers
└── [domain]          # Game, Player, Unit, City, Board
```

---

## Naming Conventions

- Commands: `<Action>Command.java` (SelectUnitCommand)
- Events: `<Event>Event.java` (UnitSelectedEvent)
- Managers: `<Domain>Manager.java` (UnitManager)
- Controllers: `<Mode>Controller.java` (GameModeController)
- Commanders: `<Mode>Commander.java` (PathsCommander)

---

## Critical Files Reference

| File | Purpose |
|------|---------|
| `Game.java` | Game loop, command processing, state machine |
| `GameController.java` | UI control API |
| `GameEventBus.java` | Event system |
| `GameCommand.java` | Command interface |
| `GameEvent.java` | Event base class |
| `UnitManager.java` | Manager pattern example |
| `CombatResolver.java` | Combat subsystem |
| `PathCalculator.java` | A* pathfinding |
| `RobotBrain.java` | AI strategy pattern |
| `Order.java` | Template method pattern |

---

## JavaFX Patterns (Active Development)

### Issuing Orders
```java
// In MapCanvas or mode classes
controller.issueOrder(unit, new Sentry(query.getGame(), unit));
controller.resumeGame(unit);
```

### Setting Paths
```java
// In PathMode
controller.postGameAction(() -> {
    SetPathCommand cmd = new SetPathCommand(originCity, destCity, travel);
    cmd.execute(game);
});
controller.resumeGame(null);
```

### UI Thread Updates
```java
// Update UI on JavaFX Application Thread
javafx.application.Platform.runLater(() -> {
    canvas.refresh();
    canvas.resetPaths(player, airVis, seaVis, landVis);
});
```

### Mode Pattern
```java
public class MyMode extends AbstractMapCanvasMode {
    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        // Handle click - issue commands via controller
        controller.issueOrder(unit, order);
        controller.resumeGame(unit);
    }
}
```

### Thread Marshalling
- **Swing**: `SwingUtilities.invokeLater()` (deprecated, use JavaFX)
- **JavaFX**: `Platform.runLater()`
- **Game→UI**: GameEventBus auto-marshals to EDT/JavaFX thread

---

## Quick Checks

Before committing code, verify:
- [ ] All game mutations use GameCommand
- [ ] All state changes publish GameEvent
- [ ] No direct Game reference in UI code
- [ ] Cross-thread fields are volatile
- [ ] No blocking on UI or game threads
- [ ] Mode delegation, no conditionals
- [ ] Commands/Events are immutable
- [ ] Manager methods publish events
- [ ] JavaFX code uses `controller.issueOrder()` not `postAndResume()`
- [ ] No modifications to Swing UI files (`src/.../ui/`)

---

**Version**: 1.1  
**Last Updated**: 2026-01-28  
**See Also**: 
- ARCHITECTURE_REFERENCE.md (detailed explanations, examples, rationale)
- src/main/java/.../ui/DO_NOT_EDIT_SWING_DEPRECATED.md (Swing deprecation notice)
