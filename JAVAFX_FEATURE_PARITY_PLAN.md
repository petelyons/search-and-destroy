# JavaFX Feature Parity Plan

## Goal
Implement remaining Swing UI features in JavaFX to achieve full feature parity.

## Current Status (2026-01-25)

### What JavaFX Has
- ✅ Basic unit movement and selection
- ✅ City menus (production, paths)
- ✅ Unit info panel
- ✅ Status bar
- ✅ Map rendering with hex tiles
- ✅ Keyboard shortcuts
- ✅ Load game dialog
- ✅ macOS native menu bar

### Missing Features (From Swing)
- ❌ Land and Sea path controllers and animation
- ❌ Escort mode controller and animation
- ❌ Side panel fidelity with Swing
- ❌ Battle history fidelity with Swing

## Phase 1: Research Current Swing Implementation

### 1.1 Analyze Swing Path Controllers
**Goal:** Understand how Swing implements land/sea path display and animation

**Tasks:**
- [ ] Read SaDFrame path controller code
- [ ] Read Canvas/Board path rendering code
- [ ] Document path data structures (Path class, waypoints)
- [ ] Document path animation (frame rate, colors, line styles)
- [ ] Screenshot Swing land path display
- [ ] Screenshot Swing sea path display
- [ ] Document differences between land and sea paths

**Files to examine:**
- `src/main/java/com/developingstorm/games/sad/ui/SaDFrame.java` - Path controller logic
- `src/main/java/com/developingstorm/games/sad/ui/Canvas.java` - Path rendering
- `src/main/java/com/developingstorm/games/sad/Board.java` - Path data
- `src/main/java/com/developingstorm/games/sad/Path.java` - Path structure

**Questions to answer:**
- How are land paths displayed? (color, line width, animation)
- How are sea paths displayed? (different from land?)
- How does path animation work? (dashed lines, moving dots, etc.)
- How are multiple paths displayed? (different colors per unit?)
- How do paths update when units move?

### 1.2 Analyze Swing Escort Mode
**Goal:** Understand escort mode behavior and UI

**Tasks:**
- [ ] Read escort mode implementation
- [ ] Document escort mode rules (which units can escort)
- [ ] Document escort mode UI indicators
- [ ] Screenshot escort mode in action
- [ ] Document keyboard/mouse controls for escort mode
- [ ] Understand escort mode data model

**Files to examine:**
- `src/main/java/com/developingstorm/games/sad/Unit.java` - Escort relationships
- Grep for "escort" to find all related code

**Questions to answer:**
- What is escort mode? (ships escorting transports?)
- How is escort mode activated?
- How is escort mode displayed visually?
- What are the gameplay rules for escorts?
- How do escorts move with their escorted unit?

### 1.3 Analyze Swing Side Panel
**Goal:** Document all side panel features

**Tasks:**
- [ ] Screenshot Swing side panel
- [ ] List all information displayed
- [ ] Document interactive elements (buttons, dropdowns)
- [ ] Document unit detail display
- [ ] Document city detail display
- [ ] Document multi-unit stacks display

**Files to examine:**
- `src/main/java/com/developingstorm/games/sad/ui/UnitStatusBar.java`
- `src/main/java/com/developingstorm/games/sad/ui/UnitDetailsDialog.java`

**Comparison with JavaFX:**
- What does Swing show that JavaFX doesn't?
- What layout differences exist?
- What information is missing in JavaFX?

### 1.4 Analyze Swing Battle History
**Goal:** Document battle history features

**Tasks:**
- [ ] Screenshot Swing battle history panel
- [ ] Document what information is shown per battle
- [ ] Document how battles are grouped/sorted
- [ ] Document interactive features (click to jump to location?)
- [ ] Read battle history data model

**Files to examine:**
- `src/main/java/com/developingstorm/games/sad/ui/BattleHistoryPanel.java`
- `src/main/java/com/developingstorm/games/sad/CombatResult.java`

**Questions to answer:**
- What battle information is displayed?
- How are battles organized? (chronological, by location?)
- Can you click battles to see details?
- How long is battle history retained?
- Is there a maximum number of battles shown?

## Phase 2: Path Controllers and Animation

### 2.1 Design JavaFX Path Rendering
**Goal:** Design how paths will be rendered in JavaFX

**Design decisions:**
- Use JavaFX `Path` or `Polyline` for rendering
- Use `Timeline` for animation
- Store paths in observable collections for automatic updates
- Use different `StrokeStyle` for land vs sea
- Use CSS for styling (colors, widths)

**Data structures:**
```java
// In SaDFxMapCanvas or similar
private Map<Unit, PathOverlay> unitPaths;

class PathOverlay {
    Unit unit;
    Path path;  // Game path data
    Polyline renderedPath;  // JavaFX rendering
    Timeline animation;  // Animated dashed lines
    Color color;
    PathType type;  // LAND, SEA
}
```

### 2.2 Implement Path Display
**Tasks:**
- [ ] Create PathOverlay class for JavaFX
- [ ] Implement path rendering (convert game Path to JavaFX Polyline)
- [ ] Implement land path styling (color, width, dashing)
- [ ] Implement sea path styling (different from land)
- [ ] Add path to canvas overlay layer
- [ ] Handle path updates when unit moves
- [ ] Handle path clearing

**Estimated time:** 4-6 hours

### 2.3 Implement Path Animation
**Tasks:**
- [ ] Create Timeline for animated dashed line effect
- [ ] Implement "marching ants" or moving dashes
- [ ] Tune animation speed
- [ ] Handle multiple animated paths simultaneously
- [ ] Pause/resume animation with game state

**Estimated time:** 2-3 hours

### 2.4 Path Controller Integration
**Tasks:**
- [ ] Subscribe to path-related events (if any)
- [ ] Implement keyboard shortcuts for path display (if any)
- [ ] Implement path toggling (show/hide)
- [ ] Test with various path scenarios:
  - [ ] Long paths across map
  - [ ] Multiple units with paths
  - [ ] Land vs sea paths
  - [ ] Paths that cross land/sea boundaries

**Estimated time:** 2-3 hours

**Total Phase 2 time: 8-12 hours**

## Phase 3: Escort Mode

### 3.1 Design Escort Mode UI
**Goal:** Design how escorts are displayed and controlled

**Design decisions:**
- Visual indicator connecting escort to escorted unit
- Color coding or icons for escort relationships
- Keyboard shortcut for escort mode
- Mouse interaction for assigning escorts

**UI mockup needs:**
- How to show escort relationships on map
- How to show in side panel
- How to activate/deactivate escort mode

### 3.2 Implement Escort Display
**Tasks:**
- [ ] Create visual link between escort and escorted unit
- [ ] Add escort indicator to unit rendering
- [ ] Update side panel to show escort relationships
- [ ] Show escort status in unit info panel

**Estimated time:** 3-4 hours

### 3.3 Implement Escort Controllers
**Tasks:**
- [ ] Add keyboard shortcut for escort mode (check Swing)
- [ ] Implement mouse interaction for assigning escorts
- [ ] Subscribe to escort-related events
- [ ] Handle escort movement (automatic following)
- [ ] Test escort mode:
  - [ ] Assign escort
  - [ ] Move escorted unit (escort follows)
  - [ ] Remove escort
  - [ ] Multiple escorts

**Estimated time:** 3-4 hours

**Total Phase 3 time: 6-8 hours**

## Phase 4: Side Panel Fidelity

### 4.1 Audit Current Side Panel
**Tasks:**
- [ ] Create side-by-side comparison: Swing vs JavaFX
- [ ] List all missing information fields
- [ ] List all missing interactive elements
- [ ] Identify layout differences

**Estimated time:** 1 hour

### 4.2 Implement Missing Information
**Tasks:**
- [ ] Add missing unit properties to side panel
- [ ] Add missing city properties to side panel
- [ ] Improve multi-unit stack display
- [ ] Add tooltips/help text
- [ ] Match Swing layout more closely

**Common missing items (to be confirmed):**
- Detailed unit statistics
- Movement points remaining
- Fuel levels (for air units)
- Transport capacity
- Cargo contents
- Production queues
- City improvements

**Estimated time:** 4-6 hours

### 4.3 Improve Side Panel UX
**Tasks:**
- [ ] Add interactive elements (buttons, toggles)
- [ ] Implement selection highlighting
- [ ] Add context menus (right-click)
- [ ] Polish styling to match game aesthetic
- [ ] Add smooth transitions/animations

**Estimated time:** 2-3 hours

**Total Phase 4 time: 7-10 hours**

## Phase 5: Battle History Fidelity

### 5.1 Audit Battle History
**Tasks:**
- [ ] Compare Swing vs JavaFX battle history
- [ ] List missing battle information
- [ ] Identify layout differences
- [ ] Document interactive features to add

**Estimated time:** 1 hour

### 5.2 Implement Battle History Panel
**Tasks:**
- [ ] Create/enhance BattleHistoryPanel in JavaFX
- [ ] Display full battle information:
  - [ ] Attacker and defender units
  - [ ] Initial and final hit points
  - [ ] Battle outcome
  - [ ] Location
  - [ ] Turn number
  - [ ] Timestamps
- [ ] Match Swing layout
- [ ] Add battle icons/graphics

**Estimated time:** 3-4 hours

### 5.3 Battle History Interaction
**Tasks:**
- [ ] Implement click-to-jump-to-location
- [ ] Add context menu (right-click for details)
- [ ] Implement battle history filtering (by unit type, player, etc.)
- [ ] Add clear history button
- [ ] Limit history size (performance)
- [ ] Subscribe to CombatResolvedEvent to update history

**Estimated time:** 2-3 hours

### 5.4 Battle History Polish
**Tasks:**
- [ ] Add visual indicators (win/loss colors)
- [ ] Add battle statistics summary
- [ ] Smooth scrolling
- [ ] Auto-scroll to latest battle
- [ ] Polish styling

**Estimated time:** 1-2 hours

**Total Phase 5 time: 7-10 hours**

## Phase 6: Testing and Polish

### 6.1 Feature Testing
**Tasks:**
- [ ] Test paths with all unit types
- [ ] Test escort mode with various scenarios
- [ ] Test side panel with all unit/city types
- [ ] Test battle history with long game sessions
- [ ] Cross-reference with Swing for missed details

**Estimated time:** 4-5 hours

### 6.2 Integration Testing
**Tasks:**
- [ ] Test all features together
- [ ] Verify event bus subscriptions work correctly
- [ ] Check for memory leaks (path animations, history)
- [ ] Performance testing (many paths, long battle history)
- [ ] Test save/load with new features

**Estimated time:** 2-3 hours

### 6.3 Polish and Bug Fixes
**Tasks:**
- [ ] Fix any bugs found during testing
- [ ] Tune animations and transitions
- [ ] Improve visual consistency
- [ ] Add missing keyboard shortcuts
- [ ] Update user documentation

**Estimated time:** 3-4 hours

**Total Phase 6 time: 9-12 hours**

## Summary

### Time Estimates by Phase
| Phase | Feature | Estimated Time |
|-------|---------|----------------|
| 1 | Research Swing Implementation | 6-8 hours |
| 2 | Path Controllers and Animation | 8-12 hours |
| 3 | Escort Mode | 6-8 hours |
| 4 | Side Panel Fidelity | 7-10 hours |
| 5 | Battle History Fidelity | 7-10 hours |
| 6 | Testing and Polish | 9-12 hours |
| **Total** | **All Features** | **43-60 hours** |

### Implementation Order (Recommended)

1. **Phase 1** - Research (do first, informs all other phases)
2. **Phase 4** - Side Panel (high visibility, frequently used)
3. **Phase 5** - Battle History (medium complexity)
4. **Phase 2** - Path Controllers (complex animation)
5. **Phase 3** - Escort Mode (depends on understanding from research)
6. **Phase 6** - Testing and Polish (after all features implemented)

### Dependencies

- Phase 1 should be completed before other phases (research informs implementation)
- Phase 2-5 can be done in any order (minimal dependencies)
- Phase 6 requires all features implemented

### Success Criteria

- [ ] All Swing features replicated in JavaFX
- [ ] Visual parity with Swing (or better)
- [ ] No regressions in existing JavaFX features
- [ ] Performance is equal or better than Swing
- [ ] Code is clean and maintainable
- [ ] Features work with event bus architecture

## Next Steps

1. Start with Phase 1: Research Swing Implementation
2. Create detailed task breakdown for each sub-phase
3. Begin with highest priority feature (likely Side Panel)
4. Implement features incrementally with testing after each

## Notes

- Keep Swing UI as reference during implementation
- Take screenshots/videos of Swing features for reference
- Document any gameplay rules discovered during research
- Consider UX improvements over Swing where appropriate
- Maintain event-driven architecture (no polling!)
