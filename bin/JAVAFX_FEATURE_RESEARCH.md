# JavaFX Feature Parity Research Summary

## Research Date: 2026-01-25

This document summarizes the research into Swing UI features that need to be implemented in JavaFX.

---

## 1. Path Controllers and Animation

### What They Are
**City production paths** - not unit movement paths! Cities can be configured with three types of production paths:
- **Land Path** - Where newly produced ground units should go
- **Sea Path** - Where newly produced ships should go  
- **Air Path** - Where newly produced aircraft should go

### How They Work

**Data Model:**
- Stored in `EdictGovernor` class (each city has a governor)
- Paths are `EdictSendUnits` commands that auto-execute
- Three fields: `landPath`, `seaPath`, `airPath`
- Each path points to a destination city

**Path Rules:**
- **Land paths**: Must be on same continent as source city
- **Sea paths**: Destination must be a coastal city
- **Air paths**: Can go anywhere

**Controller:**
- `PathsModeController.java` - Handles mouse/keyboard input
- `PathsCommander.java` - Validates and sets path destinations
- User clicks source city, selects path type, then clicks destination

### UI Display

**Swing Implementation:**
- `drawPathsBoard()` method in BoardCanvas.java
- Currently minimal - just shows cities
- No animated path lines rendered (surprisingly!)
- Path mode is mostly for **setting** paths, not **viewing** them

**What needs implementing:**
1. Path setting UI (modal mode to select destination city)
2. Visual indicator on cities that have paths configured
3. Optional: Draw lines from city to destination to visualize paths
4. Menu items: "Set Land Path", "Set Sea Path", "Set Air Path", "Cancel Path"

### JavaFX Status
- ✅ Menu items exist in MapCanvas.java (lines 1003-1038)
- ❌ Marked as "TODO: not yet implemented"
- ❌ No path setting dialog
- ❌ No visual path indicators

### Implementation Strategy
1. Create PathsMode controller for JavaFX
2. Implement mouse selection for destination city
3. Validate destination (continent check for land, coastal check for sea)
4. Call `governor.setLandPathDest()` / `setSeaPathDest()` / `setAirPathDest()`
5. Visual indicator: small icon/arrow on cities with paths
6. Optional: Render paths as dashed lines on map

**Estimated Complexity: Medium** (4-6 hours)
- Not as complex as originally thought
- No animation needed (paths are static configs)
- Main work is the modal selection UI

---

## 2. Escort Mode

### What It Is
**Ship escort functionality** - One ship follows another ship to provide protection.

### How It Works

**Game Rules:**
- Only sea units can escort
- Can only escort friendly (same player) units
- Escort automatically follows escorted ship
- Escort tries to stay within 1 hex of escorted ship
- Escort ends if:
  - Escorted ship is killed
  - Order is cleared
  - Escorted ship changes ownership

**Implementation:**
- `Escort.java` order class (in `src/main/java/com/developingstorm/games/sad/orders/`)
- Implements automatic following behavior
- Uses pathfinding to catch up if escort falls behind
- Runs every turn automatically

**Controller:**
- `EscortModeController.java` - Handles mouse/keyboard for selection
- `EscortCommander.java` - Validates and creates escort orders
- User selects escorting ship, enters escort mode, clicks target ship

### UI Behavior

**Entering Escort Mode:**
1. Select a ship (the escort)
2. Press keyboard shortcut or menu item for "Escort"
3. UI highlights valid escort targets (friendly ships)
4. Click target ship to create escort order
5. ESC or right-click to cancel

**Visual Indicators:**
- Highlight valid escort targets (friendly ships)
- Show escort relationship on map (line connecting ships?)
- Show escort status in unit info panel
- Show in order display ("Escort: [Ship Name]")

**Swing Implementation:**
- `EscortModeController.java` - Mouse/keyboard handling
- Status message: "Escort Mode: Click on a highlighted ship to escort"
- ESC or right-click to cancel
- Simple click to select target

### JavaFX Status
- ❌ Not implemented
- ❌ No keyboard shortcut defined
- ❌ No menu item
- ❌ No visual escort indicators

### Implementation Strategy
1. Add "Escort" menu item to unit context menu (for sea units only)
2. Create EscortMode state in JavaFX UI
3. Highlight valid escort targets (friendly ships in range)
4. Implement mouse click to select target
5. Visual: Draw connecting line between escort and escorted ship
6. Show "Escort: [name]" in unit status panel
7. Add keyboard shortcut (E for Escort?)

**Estimated Complexity: Medium** (3-4 hours)
- Order logic already exists in Escort.java
- Main work is UI state management and visual indicators
- Need connecting line rendering

---

## 3. Side Panel Fidelity

### What It Shows

**Swing UnitStatusBar displays:**
- Turn number
- Unit type description
- Unit location (hex coordinates)
- Health status (hits remaining)
- Movement status (has moved this turn?)
- Cargo contents ("Carries: ...")
- Current order status

**Display Format:**
```
Turn: 42  :  Unit: Infantry  Location: (15,23)  Health: 10/10  
Moved: No  Carries: (none)  Status: Sentry
```

### Information Fields

1. **Turn Number** - Current game turn
2. **Unit Type** - "Infantry", "Battleship", etc.
3. **Location** - Hex coordinates as "(x,y)"
4. **Health** - "hits/maxHits" format
5. **Moved** - "Yes" or "No" (has unit moved this turn)
6. **Carries** - Description of cargo (for transports)
7. **Status** - Current order ("Sentry", "Escort: Battleship-7", "Move to (20,25)", etc.)

### Helper Methods Used
From Unit class:
- `typeDesc()` - Returns unit type description
- `locationDesc()` - Returns formatted location string
- `life().healthDesc()` - Returns health as "X/Y"
- `life().moveDesc()` - Returns "Yes" or "No" for moved status
- `carriesDesc()` - Returns cargo description
- `getOrder().toString()` - Returns order description

### JavaFX Status
- ✅ Has basic unit info panel
- ❌ May be missing some fields
- ❌ Layout differs from Swing
- ❌ Need side-by-side comparison to identify gaps

### Research Needed
1. Compare JavaFX unit info panel with Swing
2. List missing information fields
3. Check if helper methods are being called
4. Verify turn number display
5. Verify cargo display for transports
6. Verify order status display

**Estimated Complexity: Low to Medium** (2-3 hours)
- Mostly adding missing label fields
- Helper methods already exist in Unit class
- Main work is layout and ensuring all info is shown

---

## 4. Battle History Panel

### What It Shows

**Swing BattleHistoryPanel displays:**
- Scrollable list of recent combats
- Most recent battle at top
- Each battle shown as a row with:
  - Attacker unit tile/icon
  - Attacker unit name
  - VS indicator
  - Defender unit tile/icon
  - Defender unit name
  - Battle outcome (attacker won vs defender won)
  - Visual coloring based on outcome

### Battle Information

**Per Battle:**
- Attacker unit icon
- Attacker unit name
- Attacker owner (player color)
- Attacker initial health
- Attacker final health  
- Defender unit icon
- Defender unit name
- Defender owner (player color)
- Defender initial health
- Defender final health
- Battle outcome (who won)
- Location of battle

**From CombatResult class:**
```java
CombatResult fields:
- Unit attacker
- int attackerInitialHits
- Unit defender  
- int defenderInitialHits
- boolean attackerWon
```

### UI Features

**Visual Indicators:**
- Winner highlighted (green tint?)
- Loser grayed out or red tint
- Unit tiles with player colors
- Compact vertical layout

**Interaction:**
- Click battle to jump to location (optional)
- Scroll to see history
- Auto-scroll to show latest battle
- Limited to 50 battles (performance)

**Layout:**
```
+----------------------------------------+
|           Battle History               |
+----------------------------------------+
| [Attacker] [Name]  VS  [Defender] [Name] | <- Most recent
| [Attacker] [Name]  VS  [Defender] [Name] |
| [Attacker] [Name]  VS  [Defender] [Name] |
| ...                                    |
+----------------------------------------+
```

### JavaFX Status
- ✅ Has BattleHistoryPanel in JavaFX (src/main/java/com/developingstorm/games/sad/fx/BattleHistoryPanel.java?)
- ❌ Need to verify completeness
- ❌ Check if it's connected to CombatResolvedEvent
- ❌ Verify visual indicators match Swing

### Implementation Details

**Event Subscription:**
- Subscribe to `CombatResolvedEvent` from event bus
- Extract CombatResult from event
- Call `addBattle(result)` to update panel

**Battle Row Creation:**
- Unit icon rendering
- Player color indicators
- Win/loss visual styling
- Compact layout

**Swing Code Reference:**
- `BattleHistoryPanel.java` - Main panel (lines 1-150+)
- `createBattleRow()` - Creates individual battle display
- `createUnitTile()` - Renders unit icon
- `addBattle()` - Adds new battle to history

**Estimated Complexity: Medium** (3-4 hours)
- Panel structure might exist
- Need to verify event subscription
- Polish visual styling
- Test with multiple battles

---

## Priority Assessment

Based on research, recommended implementation order:

### High Priority
1. **Side Panel Fidelity** (2-3 hours)
   - High visibility feature
   - Frequently used
   - Relatively simple implementation
   - Just adding missing fields

2. **Battle History** (3-4 hours)
   - Important for gameplay feedback
   - Event already exists (CombatResolvedEvent)
   - Moderate complexity

### Medium Priority  
3. **Path Controllers** (4-6 hours)
   - Less frequently used than expected
   - Not animated (simpler than thought)
   - Important for production management

4. **Escort Mode** (3-4 hours)
   - Less common feature
   - Order logic already exists
   - Mainly UI work

---

## Key Findings

### Surprises
1. **Paths are not animated** - They're static city production configs, not unit movement visualization
2. **Paths mode is for setting, not viewing** - Less visual work than expected
3. **Battle history already exists** - Need to verify JavaFX version completeness
4. **Side panel uses helper methods** - Easy to replicate in JavaFX

### Challenges
1. **Path destination validation** - Need continent/coastal checks
2. **Escort visual indicators** - Connecting lines between ships
3. **Battle history styling** - Win/loss visual feedback
4. **Event subscriptions** - Ensure all features subscribe to correct events

### Dependencies
- All features use existing game logic (Order classes, EdictGovernor, etc.)
- No new backend code needed
- Purely UI implementation
- Event bus already provides necessary events

---

## Next Steps

1. ✅ Research complete
2. **Start with Side Panel** - Quick win, high visibility
3. **Then Battle History** - Verify/enhance existing implementation
4. **Then Path Controllers** - More complex, less urgent
5. **Finally Escort Mode** - Least common feature

---

## Files Reference

### Swing Implementation
- `src/main/java/com/developingstorm/games/sad/ui/controls/PathsModeController.java`
- `src/main/java/com/developingstorm/games/sad/ui/controls/PathsCommander.java`
- `src/main/java/com/developingstorm/games/sad/ui/controls/EscortModeController.java`
- `src/main/java/com/developingstorm/games/sad/ui/controls/EscortCommander.java`
- `src/main/java/com/developingstorm/games/sad/ui/UnitStatusBar.java`
- `src/main/java/com/developingstorm/games/sad/ui/BattleHistoryPanel.java`

### Game Logic (Already Exists)
- `src/main/java/com/developingstorm/games/sad/orders/Escort.java`
- `src/main/java/com/developingstorm/games/sad/EdictGovernor.java`
- `src/main/java/com/developingstorm/games/sad/CombatResult.java`

### JavaFX Files to Update/Create
- `src/main/java/com/developingstorm/games/sad/fx/MapCanvas.java` - Path menu items exist
- JavaFX unit info panel (need to locate)
- JavaFX battle history panel (need to verify)
- New: JavaFX paths mode controller
- New: JavaFX escort mode controller

---

## Estimated Total Time

| Feature | Complexity | Time |
|---------|-----------|------|
| Side Panel | Low-Medium | 2-3 hours |
| Battle History | Medium | 3-4 hours |
| Path Controllers | Medium | 4-6 hours |
| Escort Mode | Medium | 3-4 hours |
| **Total** | | **12-17 hours** |

This is less than the original 43-60 hour estimate because:
- Paths are simpler than expected (no animation)
- Battle history might already exist
- Side panel just needs field additions
- All game logic already exists
