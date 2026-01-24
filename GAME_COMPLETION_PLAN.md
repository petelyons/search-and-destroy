# Search and Destroy - Game Completion Plan

## Executive Summary

The Search and Destroy game is **substantially complete and playable**. The core game loop works, with 11 unit types, combat, movement, cities, production, fog of war, and both human and AI players. However, several features need completion and polish before the game is ready for full release.

**Current Status**: ~85% Complete
- ✅ Core gameplay loop functional
- ✅ All unit types implemented
- ✅ Combat and movement systems working
- ✅ AI players functional
- ⚠️ Some UI modes incomplete
- ⚠️ Save/load has bugs
- ⚠️ Air unit AI being refined
- ⚠️ Minimal test coverage

---

## Phase 1: Critical Bug Fixes (HIGH PRIORITY)

### 1.1 Fix Order Serialization
**Status**: BROKEN - Critical for save/load functionality
**File**: `src/main/java/com/developingstorm/games/sad/persistence/UnitSerializer.java:62`
**Issue**: Order-specific data (e.g., destination for Move orders) is not being serialized

**Tasks**:
- [ ] Implement serialization for Move order destinations
- [ ] Implement serialization for all other order types (Explore, HeadHome, etc.)
- [ ] Add deserialization logic to restore order state
- [ ] Test save/load with units having active orders
- [ ] Verify loaded units continue executing their orders correctly

**Acceptance Criteria**:
- Save a game with units mid-move
- Load the game
- Units should continue their movement to the correct destination

---

### 1.2 Fix Board Reinitialization
**Status**: INCOMPLETE
**File**: `src/main/java/com/developingstorm/games/sad/ui/SaDFrame.java:580`
**Issue**: Board canvas and controllers not properly reinitialized

**Tasks**:
- [ ] Investigate what happens when starting a new game after completing one
- [ ] Implement proper cleanup of old game state
- [ ] Reinitialize board canvas with new game data
- [ ] Reinitialize all UI controllers
- [ ] Test multiple new games in same session
- [ ] Test loading a saved game after playing

**Acceptance Criteria**:
- Start game → Complete game → Start new game → No errors, fresh state
- Start game → Save game → Load game → Board displays correctly

---

### 1.3 Complete Air Unit AI
**Status**: WORK IN PROGRESS (per recent commit "Working on air unit AI")
**Files**: 
- `src/main/java/com/developingstorm/games/sad/brain/FighterCaptain.java`
- `src/main/java/com/developingstorm/games/sad/brain/BomberCaptain.java`
- `src/main/java/com/developingstorm/games/sad/brain/CargoCaptain.java`

**Tasks**:
- [ ] Review current air unit AI behavior
- [ ] Implement fuel management logic (return to city when low)
- [ ] Implement attack priorities for fighters (target enemy air first)
- [ ] Implement bombing runs for bombers (target cities and ground units)
- [ ] Implement transport logic for cargo planes (move infantry strategically)
- [ ] Test AI air units against human player
- [ ] Verify air units don't crash due to fuel exhaustion

**Acceptance Criteria**:
- AI fighters engage enemy air units effectively
- AI bombers attack high-value targets (cities, armor)
- AI cargo planes transport infantry to strategic locations
- Air units return to cities before running out of fuel

---

## Phase 2: Complete UI Modes (MEDIUM PRIORITY)

### 2.1 Complete PATHS Mode
**Status**: STUB IMPLEMENTATION
**File**: `src/main/java/com/developingstorm/games/sad/ui/PathsKeyHandler.java`

**Tasks**:
- [ ] Implement key handling for PATHS mode
- [ ] Display unit movement paths on the board
- [ ] Show path costs and movement ranges
- [ ] Add visualization for blocked/accessible hexes
- [ ] Implement mode toggle (switch between GAME/PATHS modes)
- [ ] Test with various unit types and terrain

**Acceptance Criteria**:
- Press key to enter PATHS mode
- See visual paths for selected unit
- Paths show valid/invalid destinations
- Can switch back to GAME mode

---

### 2.2 Complete EXPLORE Mode
**Status**: STUB IMPLEMENTATION  
**File**: `src/main/java/com/developingstorm/games/sad/ui/ExploreKeyHandler.java`

**Tasks**:
- [ ] Implement key handling for EXPLORE mode
- [ ] Visualize unexplored/explored/visible areas differently
- [ ] Show fog of war boundaries
- [ ] Highlight areas within unit vision range
- [ ] Implement mode toggle
- [ ] Test with different vision ranges

**Acceptance Criteria**:
- Press key to enter EXPLORE mode
- See clear visualization of fog of war states
- Vision ranges displayed correctly
- Can switch back to GAME mode

---

## Phase 3: Game Save/Load Completion (MEDIUM PRIORITY)

### 3.1 Integrate Save/Load into UI
**Status**: INFRASTRUCTURE EXISTS, INTEGRATION UNCLEAR

**Tasks**:
- [ ] Add "Save Game" menu item to File menu
- [ ] Add "Load Game" menu item to File menu
- [ ] Implement file picker dialog for save/load
- [ ] Add default save location/naming convention
- [ ] Implement save confirmation dialog
- [ ] Implement load with game-in-progress warning
- [ ] Test save/load flow end-to-end

**Acceptance Criteria**:
- Can save game via menu
- Can load game via menu
- Saved games restore complete game state
- Multiple save slots supported

---

### 3.2 Test and Verify Serialization
**Status**: NEEDS COMPREHENSIVE TESTING

**Tasks**:
- [ ] Test save/load with all unit types
- [ ] Test save/load with carried units (transports, carriers)
- [ ] Test save/load with units at various fuel levels
- [ ] Test save/load mid-combat
- [ ] Test save/load with different map sizes
- [ ] Verify city production state is saved/loaded
- [ ] Verify fog of war state is saved/loaded
- [ ] Verify player resources are saved/loaded

**Acceptance Criteria**:
- All game state elements persist correctly
- No data loss or corruption
- Loaded game is indistinguishable from original

---

## Phase 4: City Edicts & Advanced Orders (MEDIUM PRIORITY)

### 4.1 Test and Integrate City Edicts
**Status**: CODE EXISTS, INTEGRATION UNCLEAR
**Files**: `src/main/java/com/developingstorm/games/sad/edicts/*.java`

**Edicts to Verify**:
- SendAirUnits
- SendLandUnits
- SendSeaUnits
- AirPatrol
- AutoSentry

**Tasks**:
- [ ] Review edict implementation code
- [ ] Add edict UI to city dialog
- [ ] Test each edict type
- [ ] Verify edicts execute correctly each turn
- [ ] Implement edict cancellation
- [ ] Test with AI players using edicts

**Acceptance Criteria**:
- Can assign edicts to cities via UI
- Edicts execute automatically each turn
- Units follow edict instructions
- Can cancel/change edicts

---

### 4.2 Complete PATHS Mode for City-to-City Unit Routes
**Status**: INFRASTRUCTURE EXISTS, UI INCOMPLETE
**Priority**: HIGH (user-requested feature)

**Current State**:
- PathsCommander class exists and implements path setting logic
- Cities can have air/land/sea path destinations via EdictGovernor
- Send edicts (SendAirUnits, SendLandUnits, SendSeaUnits) are implemented
- PATHS mode UI exists but key handling is incomplete

**What This Feature Does**:
- Allows player to establish automated routes between cities
- Units created in a city (or moved to that city) automatically follow the path
- Different paths for air, land, and sea units
- Useful for supply lines and unit deployment automation

**Tasks**:
- [ ] Complete PathsKeyHandler implementation (currently stub)
- [ ] Add UI to enter PATHS mode from city dialog or menu
- [ ] Display path arrows on board canvas
- [ ] Add ability to clear/modify existing paths
- [ ] Test path following for newly produced units
- [ ] Test path following for units moved to cities
- [ ] Verify paths persist through save/load

**Acceptance Criteria**:
- Can set air/land/sea paths between cities via UI
- Units automatically follow assigned paths
- Paths display visually on map
- Can modify or clear paths
- Paths work with AI and human players

---

### 4.3 Implement Hunt Order
**Status**: NOT YET IMPLEMENTED  
**Priority**: HIGH (user-requested feature)

**What This Feature Does**:
- Similar to Explore order but focuses on finding and destroying enemies
- Unit moves toward last known enemy positions
- Pursues spotted enemies within vision range
- More aggressive than Explore (which focuses on revealing fog of war)

**Tasks**:
- [ ] Add HUNT to OrderType enum
- [ ] Create Hunt.java order class (extends Order)
- [ ] Implement hunt logic:
  - Track last known enemy positions for player
  - Move toward closest enemy sighting
  - Attack enemies when in range
  - Explore if no known enemies
- [ ] Add Hunt order to unit order menu
- [ ] Add serialization support for Hunt orders
- [ ] Test Hunt behavior with various unit types
- [ ] Balance aggression vs safety (don't suicide into superior forces)

**Acceptance Criteria**:
- Hunt order available in unit menu
- Units actively seek out enemies
- Units attack enemies when found
- Units don't foolishly attack overwhelming forces
- Hunt order saves/loads correctly

---

### 4.4 Additional Unit Order Enhancements (Optional)
**Status**: FUTURE ENHANCEMENTS
**Priority**: LOW

**Potential Additional Orders**:
- [ ] **Patrol Order**: Move between two points repeatedly
- [ ] **Guard Order**: Defend a specific location, attack enemies in range
- [ ] **Follow Order**: Follow another unit
- [ ] **Attack-Move**: Move to location, attacking enemies encountered
- [ ] **Build Queue**: Queue multiple units in cities

**Tasks** (Optional):
- [ ] Design new order types
- [ ] Implement order classes
- [ ] Add UI for assigning orders
- [ ] Test with various scenarios

**Acceptance Criteria**:
- New orders function as designed
- Orders can be queued/canceled
- AI can use new orders appropriately

---

## Phase 5: Testing & Quality Assurance (HIGH PRIORITY)

### 5.1 Add Unit Tests
**Status**: MINIMAL (only 1 test file exists)

**High-Priority Test Suites**:

#### Core Game Mechanics
- [ ] **GameTest.java**
  - Game initialization
  - Turn progression
  - Player switching
  - Victory detection
  - Game over handling

#### Combat System
- [ ] **CombatTest.java**
  - Unit vs unit combat
  - Unit vs city combat
  - Damage calculations
  - Unit destruction
  - City capture

#### Movement & Pathfinding
- [ ] **MovementTest.java**
  - Valid/invalid moves
  - Terrain restrictions
  - Fuel consumption
  - Pathfinding accuracy
  - Blocked hex handling

#### Unit Management
- [ ] **UnitTest.java**
  - Unit creation
  - Unit destruction
  - Carrying/loading units
  - Fuel regeneration
  - Vision calculations

#### City & Production
- [ ] **CityTest.java**
  - City ownership
  - Production tracking
  - Unit building
  - City capture
  - Resource management

#### Vision & Fog of War
- [ ] **VisionTest.java**
  - Vision state transitions
  - Unit vision ranges
  - Submarine detection
  - Vision updates

#### Serialization
- [ ] **SerializationTest.java**
  - Save game state
  - Load game state
  - State equality verification
  - All unit types serialization
  - All order types serialization

**Acceptance Criteria**:
- Minimum 70% code coverage on core game logic
- All tests pass consistently
- No flaky tests
- Tests run in <10 seconds total

---

### 5.2 Manual Playtesting
**Status**: NEEDS SYSTEMATIC TESTING

**Test Scenarios**:
- [ ] **Complete Game: Human vs AI** (Small map)
  - Play through to victory
  - Verify AI makes reasonable decisions
  - Test all unit types
  - Test combat outcomes
  - Verify victory detection

- [ ] **Complete Game: Human vs Human** (Medium map)
  - Two human players alternate turns
  - Test all orders and commands
  - Verify fog of war per player
  - Test city capture and production

- [ ] **Complete Game: AI vs AI** (Large map)
  - Watch AI play itself
  - Verify no crashes or infinite loops
  - Verify reasonable game completion time
  - Check for AI strategic variety

- [ ] **Edge Cases**
  - Submarine detection by various units
  - Air units running out of fuel
  - Transports sinking with units aboard
  - City with no production selected
  - All cities captured scenario
  - Single unit remaining scenario

- [ ] **Save/Load Testing**
  - Save at turn 5, load, continue to turn 20
  - Save mid-combat, load, verify combat resolves
  - Save with units in transports, load, verify
  - Save with air units low on fuel, load, verify

**Acceptance Criteria**:
- Can complete full games without crashes
- No game-breaking bugs discovered
- All major features work as expected
- User experience is smooth and intuitive

---

## Phase 6: Polish & User Experience (MEDIUM PRIORITY)

### 6.1 Improve AI Strategy
**Status**: BASIC AI WORKS, COULD BE SMARTER

**Potential Improvements**:
- [ ] Implement difficulty levels (Easy, Medium, Hard)
- [ ] Improve unit coordination (combined arms tactics)
- [ ] Enhance city targeting (prioritize strategic cities)
- [ ] Add production planning (build balanced armies)
- [ ] Implement defensive positioning
- [ ] Add exploration priority (fog of war clearance)

**Tasks**:
- [ ] Analyze current AI decision-making
- [ ] Identify weak strategic areas
- [ ] Implement improvements incrementally
- [ ] Test against human players
- [ ] Balance difficulty levels

**Acceptance Criteria**:
- AI provides reasonable challenge
- AI doesn't make obviously bad moves
- AI explores map effectively
- AI builds diverse unit mix

---

### 6.2 UI/UX Enhancements
**Status**: FUNCTIONAL BUT COULD BE IMPROVED

**Potential Improvements**:
- [ ] Add tooltips for units (show stats on hover)
- [ ] Add tooltips for terrain (show type and effects)
- [ ] Improve unit selection feedback (highlight, outline)
- [ ] Add sound effects (combat, movement, production)
- [ ] Add background music (optional, toggle-able)
- [ ] Improve city production dialog (show unit stats)
- [ ] Add minimap (overview of entire board)
- [ ] Add turn summary (events that occurred)
- [ ] Add unit production queue display
- [ ] Add keyboard shortcuts reference (F1 help)

**Acceptance Criteria**:
- UI feels responsive and intuitive
- New players can understand game state
- Visual feedback is clear
- Sounds enhance experience without being annoying

---

### 6.3 Game Balance
**Status**: NEEDS TESTING AND TUNING

**Areas to Review**:
- [ ] Unit costs vs. effectiveness
- [ ] Combat randomness (too random? too predictable?)
- [ ] City production rates
- [ ] Map balance (starting positions, city placement)
- [ ] Submarine detection balance
- [ ] Air unit fuel consumption rates
- [ ] Carrier vs. Battleship value proposition

**Tasks**:
- [ ] Collect playtest feedback
- [ ] Analyze win rates by strategy
- [ ] Identify overpowered/underpowered units
- [ ] Adjust values incrementally
- [ ] Re-test after changes

**Acceptance Criteria**:
- Multiple viable strategies exist
- No single unit type dominates
- Games feel balanced and competitive
- Average game length is reasonable (30-60 minutes?)

---

## Phase 7: Documentation (LOW PRIORITY)

### 7.1 Player Documentation
**Status**: MINIMAL

**Documents to Create**:
- [ ] **Game Manual** (GAME_MANUAL.md)
  - How to play
  - Unit types and capabilities
  - Combat mechanics
  - Movement and terrain
  - City management
  - Victory conditions
  - Tips and strategies

- [ ] **Quick Start Guide** (QUICKSTART.md)
  - Installation
  - Starting first game
  - Basic controls
  - First 10 turns walkthrough

- [ ] **Keyboard Reference** (CONTROLS.md)
  - All keyboard shortcuts
  - Menu navigation
  - Unit commands
  - Debug keys

**Acceptance Criteria**:
- New player can learn game from documentation
- All features are documented
- Documentation is accurate and up-to-date

---

### 7.2 Developer Documentation
**Status**: MINIMAL (only README.md exists)

**Documents to Create/Update**:
- [ ] **Architecture Overview** (ARCHITECTURE.md)
  - System design
  - Class relationships
  - Key patterns used
  - Package organization

- [ ] **Build & Run Guide** (BUILD.md)
  - Build requirements
  - Build commands
  - Running from source
  - Creating release builds
  - Packaging for distribution

- [ ] **Contributing Guide** (CONTRIBUTING.md)
  - Code style
  - Testing requirements
  - Pull request process
  - Development workflow

- [ ] **Update README.md**
  - Add screenshots
  - Add feature list
  - Add current status
  - Add build badges (if applicable)

**Acceptance Criteria**:
- New developers can understand codebase
- Build process is clear
- Contribution process is documented

---

## Phase 8: Release Preparation (LOW PRIORITY)

### 8.1 Create Executable Release
**Status**: NOT STARTED

**Tasks**:
- [ ] Set up release build configuration
- [ ] Create executable JAR with dependencies
- [ ] Test JAR on clean system (no dev environment)
- [ ] Create startup scripts (Windows .bat, Unix .sh)
- [ ] Package resources (images, maps) correctly
- [ ] Add version numbering
- [ ] Create release notes

**Deliverables**:
- `search-and-destroy-v1.0.jar`
- `run.bat` (Windows)
- `run.sh` (Mac/Linux)
- `RELEASE_NOTES.md`

**Acceptance Criteria**:
- Double-click JAR starts game
- Startup scripts work on target platforms
- All resources load correctly
- Version displayed in About dialog

---

### 8.2 Platform Testing
**Status**: NOT STARTED

**Platforms to Test**:
- [ ] Windows 10/11
- [ ] macOS (Intel)
- [ ] macOS (Apple Silicon)
- [ ] Linux (Ubuntu/Debian)
- [ ] Linux (Fedora/RHEL)

**Tasks**:
- [ ] Test on each platform
- [ ] Verify graphics rendering
- [ ] Verify keyboard/mouse input
- [ ] Verify file I/O (save/load)
- [ ] Document platform-specific issues
- [ ] Fix critical platform bugs

**Acceptance Criteria**:
- Game runs on all major platforms
- No platform-specific crashes
- UI renders correctly everywhere

---

## Implementation Roadmap

### Sprint 1: Critical Fixes (2-3 weeks)
**Goal**: Fix game-breaking bugs and complete core features

1. Fix order serialization (Phase 1.1)
2. Fix board reinitialization (Phase 1.2)
3. Complete air unit AI (Phase 1.3)
4. Manual playtest: Human vs AI (Phase 5.2)

**Milestone**: Game is fully playable without crashes or major bugs

---

### Sprint 2: UI Completion (1-2 weeks)
**Goal**: Complete all UI modes and polish interface

1. Complete PATHS mode (Phase 2.1)
2. Complete EXPLORE mode (Phase 2.2)
3. Integrate save/load into UI (Phase 3.1)
4. Test serialization thoroughly (Phase 3.2)

**Milestone**: All UI features are functional and accessible

---

### Sprint 3: Testing & Quality (2-3 weeks)
**Goal**: Ensure game quality through comprehensive testing

1. Add unit tests (Phase 5.1)
2. Manual playtesting all scenarios (Phase 5.2)
3. Test and integrate city edicts (Phase 4.1)
4. Fix all bugs discovered during testing

**Milestone**: Game is stable and well-tested

---

### Sprint 4: Polish & Balance (1-2 weeks)
**Goal**: Improve user experience and game balance

1. UI/UX enhancements (Phase 6.2)
2. Game balance tuning (Phase 6.3)
3. Improve AI strategy (Phase 6.1)
4. Enhanced unit orders (Phase 4.2) - Optional

**Milestone**: Game is polished and fun to play

---

### Sprint 5: Documentation & Release (1 week)
**Goal**: Prepare for public release

1. Write player documentation (Phase 7.1)
2. Write developer documentation (Phase 7.2)
3. Create executable release (Phase 8.1)
4. Platform testing (Phase 8.2)

**Milestone**: Game is ready for v1.0 release

---

## Success Criteria

The game is considered **COMPLETE** when:

### Must Have (Version 1.0)
- ✅ All critical bugs fixed (order serialization, board reinitialization)
- ✅ All UI modes functional (GAME, PATHS, EXPLORE)
- ✅ Save/load works perfectly
- ✅ Air unit AI is complete and functional
- ✅ Game can be played from start to victory without crashes
- ✅ Minimum 70% test coverage on core logic
- ✅ All playtesting scenarios pass
- ✅ Player documentation exists (game manual, quick start)
- ✅ Executable release is available

### Should Have (Version 1.1)
- ✅ City edicts fully integrated and tested
- ✅ Enhanced unit orders implemented
- ✅ AI strategy improvements (difficulty levels)
- ✅ UI/UX polish (tooltips, better feedback)
- ✅ Game balance is tuned
- ✅ Sound effects added

### Nice to Have (Version 2.0)
- ✅ Advanced AI tactics
- ✅ More game modes (team play, scenarios)
- ✅ More unit types or special abilities
- ✅ Map editor
- ✅ Multiplayer over network
- ✅ Campaign mode with story

---

## Current Branch Strategy

**Active Branches**:
- `master`: Stable code, game features
- `refactor`: Code modernization work (enums, managers, logging)

**Recommendation**:
1. Continue feature work on `master`
2. Keep code quality improvements on `refactor`
3. Merge `refactor` into `master` periodically
4. Create `release-1.0` branch when ready for release preparation

---

## Risk Assessment

### High Risk
- **Order serialization bug**: Could delay save/load feature significantly if complex
- **Air unit AI complexity**: May require substantial AI redesign
- **Platform compatibility**: Unknown issues on untested platforms

### Medium Risk
- **Game balance**: May require multiple iteration cycles
- **UI mode completion**: May uncover additional technical debt
- **Test coverage**: Time-consuming to achieve high coverage

### Low Risk
- **Documentation**: Straightforward but time-consuming
- **UI polish**: Nice-to-have features, can be deferred
- **Release packaging**: Standard process

---

## Resources & Tools

### Development Tools
- **IDE**: IntelliJ IDEA or Eclipse
- **Build**: Maven
- **Testing**: JUnit Jupiter 5.10.1
- **Version Control**: Git

### Testing Tools
- **Unit Testing**: JUnit
- **Code Coverage**: JaCoCo (can be added to pom.xml)
- **Static Analysis**: SpotBugs, Checkstyle (optional)

### Documentation Tools
- **Markdown**: For all documentation
- **Screenshots**: For player manual
- **Diagrams**: PlantUML or Draw.io for architecture docs (optional)

---

## Notes

- This plan assumes a single developer working part-time
- Timeline estimates are approximate and may vary
- Priorities can be adjusted based on feedback
- Some features can be moved to later versions
- Focus on getting a solid v1.0 release, then iterate

---

*Last Updated: 2026-01-09*
*Game Status: 85% Complete - Playable but needs polish*
*Next Milestone: Sprint 1 - Critical Fixes*
