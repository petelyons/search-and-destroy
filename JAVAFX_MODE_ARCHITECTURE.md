# JavaFX Mode Architecture Design

## Current Problem

Currently, the JavaFX implementation has mode-specific logic scattered across MapCanvas and GameView:
- `if (pathMode)` checks in mouse handlers
- Mode-specific fields (pathOriginCity, pathTravel, pathArrow) in MapCanvas
- Tight coupling between MapCanvas and mode logic

As we add more modes (EXPLORE, PATROL, ATTACK, ESCORT), this approach will lead to:
- Increasingly complex conditional logic
- Difficult to maintain and test
- Coupling between modes and core UI components
- Risk of mode state conflicts

## Swing Architecture (Reference)

The Swing version uses a clean separation:

```
UIController (mode switcher)
  ├─ Mode (container)
  │   ├─ Commander (game logic interface)
  │   └─ Controller (event handlers)
  │
  ├─ GameMode (default)
  │   ├─ GameCommander
  │   └─ GameModeController
  │
  ├─ PathsMode
  │   ├─ PathsCommander
  │   └─ PathsModeController
  │
  ├─ ExploreMode, PatrolMode, AttackMode, EscortMode...
```

**Key Benefits:**
1. **Single responsibility**: Each mode controller handles only its behavior
2. **Delegation**: UIController delegates all events to active mode
3. **No conditionals**: Zero `if (mode == X)` checks in UI code
4. **Easy to extend**: Add new mode by creating new Commander/Controller pair

## Proposed JavaFX Architecture

### Core Abstractions

```java
// Base mode handler interface
interface MapCanvasMode {
    void onMousePressed(MouseEvent event, Location location);
    void onMouseReleased(MouseEvent event, Location location);
    void onMouseMoved(MouseEvent event, Location location);
    void onMouseDragged(MouseEvent event, Location location);
    void onKeyPressed(KeyEvent event);
    void draw(GraphicsContext gc);  // For mode-specific rendering
    void enter();  // Called when entering this mode
    void exit();   // Called when leaving this mode
}

// Mode manager
class MapCanvasModeManager {
    private MapCanvasMode currentMode;
    private Map<UIMode, MapCanvasMode> modes;
    
    void switchMode(UIMode mode);
    void delegateEvent(Event event);
}
```

### Mode Implementations

```java
// Default game mode
class GameMode implements MapCanvasMode {
    // Current drag/select logic from MapCanvas
}

// Path setting mode
class PathMode implements MapCanvasMode {
    private City originCity;
    private Travel travelType;
    private FxArrowSprite arrow;
    
    void setOrigin(City city, Travel travel);
    // Path-specific logic
}

// Future modes
class ExploreMode implements MapCanvasMode { }
class PatrolMode implements MapCanvasMode { }
class AttackMode implements MapCanvasMode { }
class EscortMode implements MapCanvasMode { }
```

### Integration with MapCanvas

```java
class MapCanvas {
    private MapCanvasModeManager modeManager;
    
    private void handleMousePressed(MouseEvent event) {
        Location loc = pixelToHex(event.getX(), event.getY());
        modeManager.delegateMousePressed(event, loc);
    }
    
    public void enterPathMode(City city, Travel travel) {
        PathMode pathMode = (PathMode) modeManager.getMode(UIMode.PATHS);
        pathMode.setOrigin(city, travel);
        modeManager.switchMode(UIMode.PATHS);
    }
    
    private void drawSprites() {
        // Regular sprites
        for (FxSprite sprite : sprites) {
            sprite.draw(gc);
        }
        
        // Delegate mode-specific rendering
        modeManager.drawCurrentMode(gc);
        
        sprites.removeIf(FxSprite::done);
    }
}
```

## Benefits of This Approach

1. **Separation of Concerns**
   - MapCanvas handles hex rendering and coordinate conversion
   - Modes handle interaction logic
   - No mode conditionals in MapCanvas

2. **Testability**
   - Each mode can be unit tested independently
   - Mock MapCanvas for mode testing
   - Clear contracts via interface

3. **Maintainability**
   - New modes don't modify MapCanvas
   - Mode-specific state isolated in mode classes
   - Easy to find and fix mode-specific bugs

4. **Consistency**
   - All modes follow same pattern
   - Uniform event handling
   - Predictable behavior

5. **Future-Proof**
   - Adding EXPLORE, PATROL, ATTACK, ESCORT is straightforward
   - No risk of breaking existing modes
   - Can add mode-specific features without affecting others

## Implementation Plan

1. **Phase 1: Create abstractions**
   - Define `MapCanvasMode` interface
   - Create `MapCanvasModeManager` class
   - Define `UIMode` enum for JavaFX

2. **Phase 2: Extract GameMode**
   - Move current drag/select logic to `GameMode` class
   - Test that default behavior works

3. **Phase 3: Refactor PathMode**
   - Extract path mode logic to `PathMode` class
   - Remove path mode conditionals from MapCanvas
   - Test path setting works

4. **Phase 4: Integrate with MapCanvas**
   - Replace direct event handling with delegation
   - Update `enterPathMode()` to use mode manager
   - Clean up removed code

5. **Phase 5: Add remaining modes**
   - Implement ExploreMode, PatrolMode, etc.
   - Wire up menu items and keyboard shortcuts
   - Test all modes work correctly

## Example: Path Mode Before/After

### Before (Current)
```java
// In MapCanvas
private boolean pathMode = false;
private City pathOriginCity = null;
private Travel pathTravel = null;
private FxArrowSprite pathArrow = null;

private void handleMousePressed(MouseEvent event) {
    // ...
    if (pathMode) {
        City clickedCity = query.getCityAtLocation(location);
        if (clickedCity != null && isValidPathDestination(clickedCity)) {
            setPathDestination(clickedCity);
            exitPathMode();
        }
        return;
    }
    // ... rest of logic
}
```

### After (Proposed)
```java
// In PathMode class
class PathMode implements MapCanvasMode {
    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        City clickedCity = query.getCityAtLocation(location);
        if (clickedCity != null && isValidDestination(clickedCity)) {
            setPathDestination(clickedCity);
            modeManager.switchMode(UIMode.GAME);
        }
    }
}

// In MapCanvas - clean and simple
private void handleMousePressed(MouseEvent event) {
    Location loc = pixelToHex(event.getX(), event.getY());
    modeManager.delegateMousePressed(event, loc);
}
```

## Conclusion

This architecture provides a clean, maintainable foundation for implementing multiple UI modes in JavaFX, following proven patterns from the Swing implementation while adapting to JavaFX conventions.
