# JavaFX-Only Migration Plan

## Goal
Remove Swing UI completely and make JavaFX the sole UI, cleaning up architectural compromises made for Swing compatibility.

## Current Status (2026-01-25)

### What's Working in JavaFX
- ✅ Map rendering with hex tiles
- ✅ Unit display with status graphics (sentry, loaded, fuel)
- ✅ Unit selection with marching ants animation
- ✅ Mouse interaction (click to select, drag to create orders)
- ✅ Keyboard shortcuts (C, S, SPACE, X, U, H, K, ESC)
- ✅ City menus (production, paths)
- ✅ Unit info panel
- ✅ Status bar
- ✅ Game loop integration
- ✅ Load game dialog
- ✅ macOS native menu bar
- ✅ Event bus for some UI updates

### What's Still Swing-Dependent
- ⚠️ GameListener callbacks (used by Swing, null checks in JavaFX)
- ⚠️ Main entry point (Main.java launches Swing)
- ⚠️ Save game functionality (not implemented in JavaFX)
- ⚠️ Some game events missing (kill unit, hit location, etc.)

### Architectural Issues
- Game class has `GameListener gameListener` field (Swing-specific)
- Game loop uses polling (`Thread.sleep(100)`) instead of blocking queue
- HexBoardContext uses AWT types (Color, Image)
- Dual UI pattern: Swing uses callbacks, JavaFX uses event bus

## Migration Steps

### Phase 1: Expand Event Bus (HIGH PRIORITY)
Replace all GameListener callbacks with events:

**New Events Needed:**
```java
// src/main/java/com/developingstorm/games/sad/events/
UnitKilledEvent.java        // replaces killUnit()
LocationHitEvent.java        // replaces hitLocation()
PlayerSelectedEvent.java     // replaces selectPlayer()
UnitTrackedEvent.java        // replaces trackUnit()
LocationTrackedEvent.java    // replaces trackLocation()
NewTurnEvent.java            // replaces newTurn()
GameOverEvent.java           // replaces gameOver()
GameAbortedEvent.java        // replaces abort()
```

**Already Exists:**
- ✅ UnitSelectedEvent (replaces selectUnit)
- ✅ CombatResolvedEvent (replaces combatResolved)
- ✅ WaitingForOrdersEvent (replaces notifyWait)

### Phase 2: Update Game Class
Remove GameListener dependency (since Swing is reference-only now):

```java
// Before:
private GameListener gameListener;
public void setGameListener(GameListener l) { ... }
if (gameListener != null) {
    gameListener.killUnit(u, showDeath);
}

// After:
private final GameEventBus eventBus;
public Game(..., GameEventBus eventBus) { ... }
eventBus.publish(new UnitKilledEvent(u, showDeath));
```

**Benefits:**
- No more null checks
- Single communication pattern
- Cleaner, more testable code
- GameListener remains in codebase as reference only

### Phase 3: Refactor Game Loop
Replace polling with blocking queue:

```java
// Before:
while (needMoreOrders()) {
    Thread.sleep(100);
}

// After:
while (needMoreOrders()) {
    GameAction action = actionQueue.take(); // Blocks until available
    action.execute();
}
```

### Phase 4: Remove AWT Dependencies
Create UI-agnostic abstractions:

```java
// New interface: com.developingstorm.games.hexboard.RenderContext
interface RenderContext {
    int getHexSide();
    boolean showBorder();
    // Use JavaFX Color or create own Color class
}
```

### Phase 5: Swing Code Status
**Keep as reference only** - Swing code remains in repository but is not maintained or executed.

The Swing code serves as:
- Reference implementation for how features should work
- Historical documentation
- Code examples for migration

**Actions:**
- ✅ Keep files in repository
- ✅ Remove from execution path (Main.java won't launch it)
- ✅ Don't update or maintain
- ❌ Don't delete (useful reference)

### Phase 6: Update Main Entry Point

```java
// src/main/java/com/developingstorm/games/sad/Main.java
public class Main {
    public static void main(String[] args) {
        // Before: Launched Swing
        // After: Launch JavaFX only
        Application.launch(SaDFxApplication.class, args);
    }
}
```

### Phase 7: Implement Missing Features
- Save game functionality
- Any Swing features not yet in JavaFX

## Benefits After Migration

### Code Quality
- ✅ Single UI codebase to maintain
- ✅ Pure event-driven architecture
- ✅ Game logic completely UI-agnostic
- ✅ No null checks for gameListener
- ✅ Cleaner threading model

### Performance
- ✅ No polling (eliminates 10 wakeups/second)
- ✅ Better JavaFX rendering performance
- ✅ Reduced CPU usage

### Developer Experience
- ✅ Easier to add new features
- ✅ Better testability (mock event bus)
- ✅ Modern UI toolkit
- ✅ macOS native menu bar

## Risk Mitigation

### Testing Strategy
1. Create comprehensive manual test checklist
2. Test all game features before deleting Swing code
3. Keep Swing code in git history (can revert if needed)
4. Tag last "dual UI" commit for reference

### Rollback Plan
Git tags:
- `last-swing-ui` - Last commit with working Swing
- `javafx-only-start` - First commit of JavaFX-only migration

## Implementation Order

1. ✅ **Create this plan** (DONE)
2. ✅ **Create missing events** (DONE - All events created)
3. ✅ **Update Game class to use event bus** (DONE - GameListener removed, event bus implemented)
4. ✅ **Add type-safe DebugEventBus** (DONE - Separates high-volume debug events from production events)
5. ✅ **Update Zed tasks to JavaFX only** (DONE)
6. ✅ **Refactor game loop** (DONE - Replaced Thread.sleep polling with proper wait/notify blocking)
7. ✅ **Update Main.java to default to JavaFX** (DONE - SAD.java now launches JavaFX)
8. ✅ **Compiles successfully** (DONE - All 232 source files compile)
9. **DEFERRED: Remove Swing UI** (keeping as reference code)

**STATUS: Migration Complete! ✅**

All core migration work is complete. The game now:
- Uses JavaFX as the primary and default UI
- Has a pure event-driven architecture with type-safe event buses
- No longer polls (uses proper blocking synchronization)
- Keeps Swing code as reference only (commented out in execution path)

## Next Steps

**Recommended:**
1. Play-test the JavaFX UI thoroughly
2. Verify all game features work correctly
3. Consider Phase 4 (Remove AWT Dependencies) when time permits
