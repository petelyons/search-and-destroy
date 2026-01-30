# Architecture Reference Document - Search and Destroy Game

## Purpose
This document serves as the architectural reference for the Search and Destroy (SaD) game codebase. It establishes patterns, principles, and guidelines to ensure consistency during ongoing development, especially during the JavaFX migration and feature additions.

---

## 1. Core Architectural Principles

### 1.1 Separation of Concerns
- **UI Layer**: Displays state, captures user input, never mutates game state directly
- **Controller Layer**: Provides clean API boundary between UI and game logic
- **Game Logic Layer**: Owns all mutable state, processes commands sequentially
- **Domain Layer**: Subsystems (combat, pathfinding, AI) with single responsibilities

### 1.2 Thread Safety
- **Game Thread**: Owns all mutable game state, processes commands sequentially
- **UI Thread (EDT)**: Handles all UI rendering and event handling
- **No Cross-Thread Mutations**: UI never directly modifies game state
- **Lock-Free Communication**: ConcurrentLinkedQueue for command submission

### 1.3 Event-Driven Architecture
- Game publishes events for all state changes
- UI listens to events and updates automatically
- No manual UI refresh calls from game logic
- Automatic marshalling to EDT via GameEventBus

---

## 2. Communication Patterns

### 2.1 UI → Game: Command Pattern

**✅ CORRECT WAY:**
```java
// UI code
GameCommand command = new SelectUnitCommand(unit);
gameController.submitCommand(command);
```

**❌ INCORRECT WAY:**
```java
// Never do this from UI
game.setSelectedUnit(unit);  // Direct mutation from UI thread
```

**Guidelines:**
- All game state mutations MUST go through GameCommand implementations
- Commands are queued and executed on game thread
- Commands should be immutable and capture all needed data
- Place commands in `com.developingstorm.games.sad.commands` package

**Command Template:**
```java
public class MyCommand implements GameCommand {
    private final Object capturedData;
    
    public MyCommand(Object data) {
        this.capturedData = data;  // Capture at construction
    }
    
    @Override
    public void execute(Game game) {
        // Execute on game thread
        game.doSomething(capturedData);
    }
}
```

### 2.2 Game → UI: Event Pattern

**✅ CORRECT WAY:**
```java
// Game code
eventBus.publish(new UnitSelectedEvent(unit));

// UI code
gameEventBus.addListener(GameEventType.UNIT_SELECTED, event -> {
    UnitSelectedEvent evt = (UnitSelectedEvent) event;
    // Update UI automatically on EDT
    updateSelectionDisplay(evt.getUnit());
});
```

**❌ INCORRECT WAY:**
```java
// Never do this from game logic
SwingUtilities.invokeLater(() -> {
    frame.updateDisplay();  // Game shouldn't know about UI
});
```

**Guidelines:**
- Publish events for ALL state changes that UI might care about
- Events should contain snapshot data (not references to mutable game state)
- GameEventBus automatically marshals to EDT
- Place events in `com.developingstorm.games.sad.events` package

**Event Template:**
```java
public class MyEvent extends GameEvent {
    private final Object eventData;
    
    public MyEvent(Object data) {
        super(GameEventType.MY_EVENT);
        this.eventData = data;
    }
    
    public Object getEventData() {
        return eventData;
    }
}
```

### 2.3 MapUpdatedEvent Convention

**Automatic Refresh Pattern:**
- After processing ANY commands, Game automatically fires `MapUpdatedEvent`
- UI listens to this event and triggers full repaint
- Individual feature events (UnitSelectedEvent, CombatResolvedEvent) provide detail
- MapUpdatedEvent ensures UI never gets stale

**Implementation:**
```java
// Game.java - after command processing
private void processCommands() {
    GameCommand command;
    int processed = 0;
    
    while ((command = commandQueue.poll()) != null) {
        command.execute(this);
        processed++;
    }
    
    if (processed > 0) {
        eventBus.publish(new MapUpdatedEvent());  // Automatic refresh
    }
}
```

---

## 3. Game State Management

### 3.1 State Machine

**States:**
- `RUNNING`: Game is actively processing turns
- `AWAITING_ORDERS`: Game needs player input before continuing
- `PAUSED`: Game temporarily stopped
- `GAME_OVER`: Game finished

**State Transitions:**
```
RUNNING
  │ (player needs orders)
  ▼
AWAITING_ORDERS
  │ (polling for resume commands)
  │ (user submits orders)
  │ (ResumeGameCommand submitted)
  ▼
RUNNING
```

**Implementation Pattern:**
```java
private final AtomicReference<GameState> gameState;

// Atomic transition
private boolean transitionState(GameState expected, GameState newState) {
    boolean success = gameState.compareAndSet(expected, newState);
    if (success) {
        eventBus.publish(new GameStateChangedEvent(newState));
    }
    return success;
}
```

### 3.2 Thread-Safe State Access

**Visibility:**
```java
// Use volatile for fields accessed across threads
private volatile Unit selectedUnit;
private volatile Player currentPlayer;
private volatile int turn;
```

**Atomicity:**
```java
// Use AtomicReference for complex state
private final AtomicReference<GameState> gameState;
```

**Mutual Exclusion:**
```java
// Use ReadWriteLock when multiple readers, single writer
private final ReadWriteLock locationLock = new ReentrantReadWriteLock();

public Set<Unit> getUnitsAtLocation(Location loc) {
    locationLock.readLock().lock();
    try {
        return new HashSet<>(locations[loc.x][loc.y]);
    } finally {
        locationLock.readLock().unlock();
    }
}
```

---

## 4. Subsystem Design Patterns

### 4.1 Manager Pattern

**Purpose:** Centralize responsibility for a specific domain

**Examples:**
- `UnitManager`: Unit lifecycle, location tracking, name assignment
- `CityManager`: City ownership, production management
- `VisionManager`: Fog-of-war, exploration state
- `CombatResolver`: Combat resolution logic
- `PathCalculator`: A* pathfinding
- `MovementResolver`: Movement execution

**Guidelines:**
- One manager per domain responsibility
- Managers are owned by Game instance
- Managers may have internal state but should be stateless where possible
- Managers expose clean API methods
- Thread safety is manager's responsibility

**Manager Template:**
```java
public class MyManager {
    private final Game game;  // Reference to game context
    
    public MyManager(Game game) {
        this.game = game;
    }
    
    public void performAction(Unit unit) {
        // Manager logic here
        // Publish events if state changes
        game.getEventBus().publish(new ActionPerformedEvent(unit));
    }
}
```

### 4.2 Strategy Pattern (AI)

**Purpose:** Pluggable AI decision-making

**Hierarchy:**
```
IBrain (interface)
  └─ RobotBrain (implementation)
       ├─ General (decides orders for units)
       │    └─ Specialized Captains (BattleshipCaptain, BomberCaptain, etc.)
       └─ Battleplan (strategic objectives)
```

**Interface:**
```java
public interface IBrain {
    void startNewTurn(Player player);
    Order getOrders(Unit unit);
    Type getProduction(City city);
}
```

**Guidelines:**
- Implement IBrain for new AI strategies
- Use existing General/Battleplan hierarchy for standard strategy AI
- AI should use GameQueryService to read game state
- AI should return decisions, not execute them directly

### 4.3 Template Method Pattern (Orders)

**Purpose:** Consistent order execution with pluggable behavior

**Base Class:**
```java
public abstract class Order {
    protected Unit unit;
    
    public abstract OrderResponse execute();
    public abstract String getDescription();
}
```

**Guidelines:**
- Extend Order for new order types
- Place in `com.developingstorm.games.sad.orders` package
- Return OrderResponse with execution result
- Fire events for order execution results

---

## 5. UI Architecture Patterns

### 5.1 Controller Pattern

**Single Entry Point:**
```java
// GameController - UI's only way to control game
public interface GameController {
    void issueOrder(Unit unit, Order order);
    void selectUnit(Unit unit);
    void resumeGame();
    void pauseGame();
    void setPath(Unit unit, Path path);
    void submitCommand(GameCommand command);
}
```

**Read-Only Queries:**
```java
// GameQueryService - UI's way to read game state
public interface GameQueryService {
    Unit getSelectedUnit();
    Player getCurrentPlayer();
    int getTurn();
    GameState getGameState();
    Set<Unit> getUnitsAtLocation(Location loc);
}
```

**Guidelines:**
- UI ONLY interacts with GameController and GameQueryService
- Never pass Game reference to UI code
- Controller methods should be high-level operations
- Query service should be efficient (avoid deep copies)

### 5.2 Mode Pattern (Swing Reference)

**Purpose:** Clean separation of UI interaction modes

**Current Modes:**
- GameMode (default selection/movement)
- PathsMode (city path edict editing)
- ExploreMode (path exploration)
- PatrolMode (patrol path setup)
- AttackMode (target selection)
- EscortMode (escort assignment)

**Pattern:**
```java
// Mode interface
public interface ModeController {
    void handleHexClick(Location loc);
    void handleRightClick(Location loc);
    void activate();
    void deactivate();
}

// Mode switching
public class UIController {
    private ModeController currentMode;
    
    public void setMode(ModeController newMode) {
        if (currentMode != null) {
            currentMode.deactivate();
        }
        currentMode = newMode;
        newMode.activate();
    }
    
    public void handleClick(Location loc) {
        currentMode.handleHexClick(loc);  // Delegate to mode
    }
}
```

**Guidelines (for JavaFX migration):**
- Create MapCanvasMode interface for JavaFX
- Each mode is a separate class
- NO mode conditionals in base UI code (delegate to mode)
- Modes communicate via GameController, not direct game access
- Modes listen to events for state updates

---

## 6. Data Model Conventions

### 6.1 Immutability

**Immutable Types:**
- `Location` (hex coordinate - immutable value object)
- All `GameEvent` implementations (snapshot data)
- All `GameCommand` implementations (capture data at construction)

**Benefits:**
- Thread-safe by default
- Can be safely shared across threads
- No defensive copying needed

### 6.2 Ownership

**Clear Ownership:**
- Game owns: all mutable state, managers, command queue
- Player owns: units list, cities list, vision state
- Unit owns: order, life, carried units
- Board owns: terrain, continents, city locations

**Guidelines:**
- Don't create circular references without clear ownership
- Parent owns children
- Use references (ids, names) for non-ownership relationships

### 6.3 Type System

**Enum-Based Types:**
```java
// Unit types with attributes
public enum Type {
    INFANTRY(10, 2, 1, 0, 1, Travel.LAND, 1),
    ARMOR(20, 4, 2, 0, 2, Travel.LAND, 1),
    BOMBER(40, 6, 3, 8, 6, Travel.AIR, 1),
    // ... etc
    
    private final int cost, attack, defense, fuel, distance;
    private final Travel travel;
    private final int hits;
}
```

**Guidelines:**
- Use enums for fixed type systems
- Store attributes in enum constants
- Avoid parallel arrays or maps for type data

---

## 7. Concurrency Guidelines

### 7.1 Lock-Free Patterns

**Command Queue:**
```java
private final ConcurrentLinkedQueue<GameCommand> commandQueue;

// Submit from any thread
public void submitCommand(GameCommand command) {
    commandQueue.offer(command);
}

// Process on game thread
private void processCommands() {
    GameCommand command;
    while ((command = commandQueue.poll()) != null) {
        command.execute(this);
    }
}
```

**State Transitions:**
```java
private final AtomicReference<GameState> gameState;

private boolean transitionState(GameState expected, GameState newState) {
    return gameState.compareAndSet(expected, newState);
}
```

### 7.2 When to Use Locks

**ReadWriteLock Pattern:**
```java
// Many readers, single writer
private final ReadWriteLock lock = new ReentrantReadWriteLock();

public Data readData() {
    lock.readLock().lock();
    try {
        return data;
    } finally {
        lock.readLock().unlock();
    }
}

public void writeData(Data newData) {
    lock.writeLock().lock();
    try {
        data = newData;
    } finally {
        lock.writeLock().unlock();
    }
}
```

**Synchronized Methods:**
```java
// Use when atomic multi-step operations needed
public synchronized void complexOperation() {
    step1();
    step2();
    step3();
}
```

### 7.3 Threading Rules

**MUST FOLLOW:**
1. ✅ All game state mutations on game thread only
2. ✅ Use GameCommand for mutations from other threads
3. ✅ Use GameEventBus for cross-thread notifications
4. ✅ Mark cross-thread fields as volatile
5. ✅ Use ConcurrentLinkedQueue for work submission
6. ❌ Never block game thread on UI operations
7. ❌ Never block UI thread on game operations
8. ❌ Never use wait/notify (use polling + command queue)

---

## 8. Feature Development Checklist

### 8.1 Adding a New Feature

**Steps:**
1. **Define Command(s)**: Create GameCommand implementation(s) for user actions
2. **Define Event(s)**: Create GameEvent(s) for state changes
3. **Implement Game Logic**: Add logic to Game or appropriate Manager
4. **Extend Controller**: Add methods to GameController if needed
5. **Update UI**: Listen to events, submit commands
6. **Test**: Verify thread safety, event firing, state consistency

**Example - Adding "Repair Unit" Feature:**

```java
// 1. Command
public class RepairUnitCommand implements GameCommand {
    private final long unitId;
    
    public RepairUnitCommand(long unitId) {
        this.unitId = unitId;
    }
    
    @Override
    public void execute(Game game) {
        Unit unit = game.getUnitById(unitId);
        if (unit != null) {
            game.repairUnit(unit);
        }
    }
}

// 2. Event
public class UnitRepairedEvent extends GameEvent {
    private final long unitId;
    private final int newHits;
    
    public UnitRepairedEvent(long unitId, int newHits) {
        super(GameEventType.UNIT_REPAIRED);
        this.unitId = unitId;
        this.newHits = newHits;
    }
    
    public long getUnitId() { return unitId; }
    public int getNewHits() { return newHits; }
}

// 3. Game Logic
public void repairUnit(Unit unit) {
    int oldHits = unit.getLife().hits;
    int maxHits = unit.getType().hits;
    
    if (oldHits < maxHits) {
        unit.getLife().hits = maxHits;
        eventBus.publish(new UnitRepairedEvent(unit.id, maxHits));
    }
}

// 4. Controller
public void repairUnit(Unit unit) {
    submitCommand(new RepairUnitCommand(unit.id));
}

// 5. UI
gameEventBus.addListener(GameEventType.UNIT_REPAIRED, event -> {
    UnitRepairedEvent evt = (UnitRepairedEvent) event;
    showMessage("Unit repaired to " + evt.getNewHits() + " hits");
});

// User clicks repair button
repairButton.addActionListener(e -> {
    gameController.repairUnit(selectedUnit);
});
```

### 8.2 Adding a New UI Mode

**Steps:**
1. **Define Mode Interface Implementation**
2. **Create Mode Controller**
3. **Create Mode Commander** (if using Swing pattern)
4. **Register Mode in UIController**
5. **Add Mode Toggle UI**
6. **Handle Mode Activation/Deactivation**

**Template:**
```java
public class MyModeController implements ModeController {
    private final GameController gameController;
    private final GameQueryService queryService;
    
    public MyModeController(GameController controller, GameQueryService query) {
        this.gameController = controller;
        this.queryService = query;
    }
    
    @Override
    public void handleHexClick(Location loc) {
        // Handle click in this mode
        gameController.submitCommand(new MyModeCommand(loc));
    }
    
    @Override
    public void activate() {
        // Show mode-specific UI elements
    }
    
    @Override
    public void deactivate() {
        // Hide mode-specific UI elements
    }
}
```

---

## 9. Testing Strategies

### 9.1 Unit Testing

**Test Managers Independently:**
```java
@Test
public void testCombatResolver() {
    Game game = new Game();
    CombatResolver resolver = game.getCombatResolver();
    
    Unit attacker = createTestUnit(Type.ARMOR);
    Unit defender = createTestUnit(Type.INFANTRY);
    
    resolver.resolveUnitAttack(attacker, defender);
    
    assertTrue(attacker.isAlive() || defender.isAlive());
}
```

**Test Commands:**
```java
@Test
public void testSelectUnitCommand() {
    Game game = new Game();
    Unit unit = createTestUnit();
    
    SelectUnitCommand command = new SelectUnitCommand(unit.id);
    command.execute(game);
    
    assertEquals(unit, game.getSelectedUnit());
}
```

### 9.2 Integration Testing

**Test Command → Event Flow:**
```java
@Test
public void testUnitSelectionFlow() {
    Game game = new Game();
    GameEventBus eventBus = game.getEventBus();
    
    AtomicBoolean eventFired = new AtomicBoolean(false);
    eventBus.addListener(GameEventType.UNIT_SELECTED, event -> {
        eventFired.set(true);
    });
    
    Unit unit = createTestUnit();
    game.submitCommand(new SelectUnitCommand(unit.id));
    game.processCommandsForTest();  // Process immediately
    
    assertTrue(eventFired.get());
}
```

### 9.3 Thread Safety Testing

**Verify Concurrent Access:**
```java
@Test
public void testConcurrentCommandSubmission() throws Exception {
    Game game = new Game();
    
    int numThreads = 10;
    int commandsPerThread = 100;
    
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    
    for (int i = 0; i < numThreads; i++) {
        executor.submit(() -> {
            for (int j = 0; j < commandsPerThread; j++) {
                game.submitCommand(new TestCommand());
            }
            latch.countDown();
        });
    }
    
    latch.await();
    // Verify no exceptions, no lost commands
}
```

---

## 10. Migration to JavaFX

### 10.1 Architecture Preservation

**Keep These Patterns:**
- GameController interface
- GameEventBus for notifications
- Command pattern for mutations
- Thread separation (JavaFX Application Thread instead of EDT)
- Mode pattern for interaction modes

**Update These Patterns:**
- Use Platform.runLater() instead of SwingUtilities.invokeLater()
- Canvas → JavaFX Canvas
- Swing components → JavaFX controls
- AWT painting → JavaFX rendering

### 10.2 Parallel Implementation Strategy

**Current Approach (Good!):**
- Keep Swing UI functional
- Build JavaFX UI in parallel
- Both use same GameController/GameEventBus
- Share game logic completely
- Gradually migrate features

**Benefits:**
- No disruption to existing functionality
- Can compare implementations
- Incremental testing
- Rollback safety

---

## 11. Code Organization

### 11.1 Package Structure

```
com.developingstorm.games.sad/
├── commands/           # GameCommand implementations
├── controller/         # GameController, GameQueryService
├── events/            # GameEvent implementations
├── fx/                # JavaFX UI (new)
├── ui/                # Swing UI (legacy)
│   └── controls/      # Mode commanders/controllers
├── orders/            # Order implementations
├── brain/             # AI implementations
└── [domain classes]   # Game, Player, Unit, City, Board, etc.

com.developingstorm.games.astar/
└── AStar.java         # A* pathfinding algorithm

com.developingstorm.util/
└── GraphNode.java     # Graph data structure
```

### 11.2 File Naming Conventions

- Commands: `<Action>Command.java` (e.g., SelectUnitCommand)
- Events: `<Event>Event.java` (e.g., UnitSelectedEvent)
- Managers: `<Domain>Manager.java` (e.g., UnitManager)
- Controllers: `<Mode>Controller.java` (e.g., GameModeController)
- Commanders: `<Mode>Commander.java` (e.g., PathsCommander)

---

## 12. Common Pitfalls to Avoid

### ❌ Direct Game Mutation from UI
```java
// WRONG
button.addActionListener(e -> {
    game.setSelectedUnit(unit);  // Cross-thread mutation!
});

// RIGHT
button.addActionListener(e -> {
    gameController.selectUnit(unit);  // Queues command
});
```

### ❌ Manual UI Refresh Calls
```java
// WRONG
public void moveUnit(Unit unit, Location loc) {
    unit.setLocation(loc);
    frame.repaint();  // Game shouldn't know about UI!
}

// RIGHT
public void moveUnit(Unit unit, Location loc) {
    unit.setLocation(loc);
    eventBus.publish(new UnitMovedEvent(unit.id, loc));
    // UI listens and repaints automatically
}
```

### ❌ Blocking Game Thread on UI
```java
// WRONG
public void waitForUserInput() {
    synchronized (lock) {
        lock.wait();  // Blocks game thread!
    }
}

// RIGHT
public void awaitOrders() {
    transitionState(GameState.RUNNING, GameState.AWAITING_ORDERS);
    // Poll for resume command via command queue
}
```

### ❌ Mode Conditionals in Base UI
```java
// WRONG
public void handleClick(Location loc) {
    if (mode == Mode.PATHS) {
        handlePathsClick(loc);
    } else if (mode == Mode.ATTACK) {
        handleAttackClick(loc);
    }
    // ... many more conditions
}

// RIGHT
public void handleClick(Location loc) {
    currentModeController.handleHexClick(loc);  // Delegate!
}
```

### ❌ Race Conditions with Non-Volatile Fields
```java
// WRONG
private Unit selectedUnit;  // Not visible across threads!

// RIGHT
private volatile Unit selectedUnit;  // Visible to all threads
```

---

## 13. Performance Considerations

### 13.1 Event Frequency
- Avoid publishing events in tight loops
- Batch updates where possible
- Use MapUpdatedEvent for general refresh (already automatic)

### 13.2 Path Calculation Caching
- PathCalculator already caches results
- Don't recalculate paths unnecessarily
- Use existing Path objects where possible

### 13.3 Location Tracking
- UnitManager uses ReadWriteLock for concurrent queries
- Read operations don't block each other
- Write operations block reads (necessary for consistency)

---

## 14. Documentation Standards

### 14.1 Architectural Decision Records
- Create markdown files for significant architectural changes
- Include: context, decision, rationale, alternatives considered
- Examples: REACTIVE_UI_ARCHITECTURE.md, CONCURRENCY_IMPROVEMENTS.md

### 14.2 Code Comments
- Document WHY, not WHAT
- Explain non-obvious design decisions
- Reference ADR documents where applicable
- Mark deprecated patterns clearly

### 14.3 API Documentation
- JavaDoc for public interfaces (GameController, IBrain, Order, etc.)
- Document thread-safety guarantees
- Document event firing behavior
- Document command execution timing

---

## 15. Quick Reference

### Common Operations

| Task | Pattern |
|------|---------|
| Mutate game state | Create GameCommand, submit via GameController |
| Notify UI of change | Publish GameEvent via GameEventBus |
| Read game state | Use GameQueryService or read volatile fields |
| Add new feature | Command + Event + Logic + Controller method |
| Add UI mode | Create ModeController implementation |
| Add AI behavior | Implement IBrain or extend RobotBrain |
| Add order type | Extend Order abstract class |
| Add unit type | Add to Type enum |

### Key Classes

| Class | Purpose | Thread Ownership |
|-------|---------|------------------|
| Game | Main game loop, command processing | Game Thread |
| GameController | UI control API | Called from UI Thread |
| GameEventBus | Event publishing/subscription | Thread-safe |
| GameCommand | State mutation interface | Executed on Game Thread |
| GameEvent | State change notification | Created on Game Thread, delivered to UI Thread |
| UnitManager | Unit lifecycle management | Game Thread (with ReadWriteLock) |
| CombatResolver | Combat resolution | Game Thread |
| PathCalculator | A* pathfinding | Any thread (read-only) |

---

## Implementation Plan

### Critical Files to Reference
- `Game.java` - Core game loop, command processing, state machine
- `GameController.java` / `GameControllerImpl.java` - Controller API
- `GameEventBus.java` - Event system implementation
- `GameCommand.java` - Command interface
- `GameEvent.java` - Event base class
- `UnitManager.java` - Manager pattern example
- `RobotBrain.java` - Strategy pattern example
- `Order.java` - Template method pattern example

### Verification Steps
1. **Test Command Flow**: Create test command, verify execution on game thread
2. **Test Event Flow**: Verify events published after commands, delivered to UI thread
3. **Test Thread Safety**: Verify no cross-thread mutations, no race conditions
4. **Test Mode Pattern**: Verify mode switching, delegation working correctly
5. **Integration Test**: Full UI → Command → Game → Event → UI cycle
6. **Performance Test**: Verify no blocking, responsive UI, smooth gameplay

### Document Structure
- Start with overview and principles (done)
- Detail communication patterns (done)
- Explain subsystem patterns (done)
- Provide implementation templates (done)
- Include anti-patterns and pitfalls (done)
- Add quick reference and checklists (done)

---

## Success Criteria

This architecture reference document succeeds if:
1. ✅ New developers can understand the system architecture quickly
2. ✅ Feature development follows consistent patterns
3. ✅ Thread safety is maintained across all new code
4. ✅ JavaFX migration preserves architectural integrity
5. ✅ Code reviews can reference specific patterns/guidelines
6. ✅ Common mistakes are avoided through clear anti-pattern examples
7. ✅ Testing strategies are well-defined and followed

---

**Document Version**: 1.0  
**Last Updated**: 2026-01-28  
**Maintained By**: Architecture Team
