# Visibility and Detection Overhaul — Design Spec

## Goal

Replace the binary "visible or not" unit detection with a two-ring detection system (detection range / identification range) and a persistent ghost contact system. Destroyers and cruisers become the eyes and ears of the fleet. The AI gains access to ghost positions for smarter pursuit.

## Approach

Extend the existing visibility system (Approach 1). The current `Vision` enum and `visible[][]` array continue to handle terrain fog-of-war. A new `ContactMap` layer handles unit detection, identification, and ghost tracking. This is additive — existing game engine code is minimally affected.

---

## 1. Data Model

### 1.1 New Fields on `Type` Enum

Each unit type gains three new stats:

| Unit | detectionRange | identificationRange | spotterQuality |
|------|---------------|---------------------|----------------|
| Infantry | 1 | 1 | 0 |
| Armor | 1 | 1 | 0 |
| Fighter | 4 | 2 | 1 |
| Bomber | 4 | 2 | 1 |
| Cargo | 2 | 2 | 0 |
| Destroyer | 3 | 3 | 1 |
| Transport | 2 | 1 | 0 |
| Submarine | 2 | 2 | 1 |
| Cruiser | 4 | 4 | 2 |
| Carrier | 3 | 1 | 0 |
| Battleship | 3 | 2 | 1 |

Cities detect as: detectionRange=3, identificationRange=3, spotterQuality=2, vision=COMPLETE.

Design notes:
- Destroyers and cruisers have identificationRange == detectionRange (they always know what they're looking at).
- Air units detect far but identify only at close range.
- Transports and carriers are deliberately poor detectors — they need escorts.
- Cruisers are the best detectors in the game (range 4, quality 2).

### 1.2 Contact Class (New)

Represents a single enemy contact — what the player knows about an enemy unit.

```
Contact:
  unitId: long                — the actual enemy unit's ID (for dedup/tracking)
  location: Location          — where the contact is/was
  type: Type (nullable)       — null if unidentified, set if identified
  travelType: Travel          — always known (LAND/SEA/AIR), even when unidentified
  owner: Player               — which enemy player owns it
  turnDetected: int           — game turn when this contact was last refreshed
  identified: boolean         — was the unit type identified?
  spotterQuality: int         — ghost decay bonus from the best spotter (0-2)
```

A contact is either **current** (refreshed this turn) or a **ghost** (last refreshed on a prior turn).

### 1.3 ContactMap Class (New)

Owned by each `Player`. Holds all known contacts.

```
ContactMap:
  contacts: Map<Long, Contact>       — keyed by enemy unit ID

  methods:
    updateContact(unit, location, identified, spotterQuality, currentTurn)
    refreshContacts(currentTurn)      — decay/remove expired ghosts
    getContactAt(location): Contact   — best contact at a hex (for rendering)
    getAllContacts(): Collection<Contact>
    getGhosts(): Collection<Contact>
    getCurrentContacts(): Collection<Contact>
    removeContact(unitId)
```

### 1.4 Ghost Decay

Ghost lifetime formula: `maxAge = baseDuration + spotterQuality`

- `baseDuration` = 2 turns (universal)
- `spotterQuality` = 0, 1, or 2 (from the detecting unit)
- Ghost lifetime ranges from 2 turns (poor spotter) to 4 turns (cruiser sighting)

A contact expires when `currentTurn - turnDetected > maxAge`. Expired contacts are removed during `refreshContacts()`.

Contacts for dead units are also removed during refresh (check `unit.isDead()`).

### 1.5 Submarine Auto-Identification

Since Submarine is the only subsurface unit type, any contact detected via WATER or COMPLETE vision with `travelType == SEA` that is specifically a submarine is automatically identified. When a unit with WATER or COMPLETE vision detects a submarine, `identified` is set to true and `type` is set to `Type.SUBMARINE`.

### 1.6 Multiple Contacts at Same Hex

When rendering, only one contact is shown per hex. Priority for choosing which contact to display:

1. Known type (identified) beats unknown type — always prefer identified contacts
2. Among same identification level, longest remaining lifetime wins

`ContactMap.getContactAt(location)` implements this priority.

---

## 2. Detection Logic

### 2.1 Per-Turn Flow

Detection runs inside `Player.startNewTurn()`, after the existing visibility recalculation:

```
1. clearVisibility()                    — existing: reset visible[][] to NONE
2. adjustVisibility() for cities/units  — existing: mark terrain fog-of-war
3. buildCityLists()                     — existing
4. scanForContacts()                    — NEW: detect enemy units, populate ContactMap
5. contactMap.refreshContacts(turn)     — NEW: decay/remove expired ghosts
6. calcEnemyActivity()                  — existing (could be enhanced to use contacts)
```

Step 4 replaces the contact-tracking portion of `buildEnemyUnitList()`. The existing `enemyUnits` set is still populated for backwards compatibility (from current contacts only).

### 2.2 Contact Scan Algorithm

```
For each owned unit U:
  For each enemy unit E (across all enemy players):
    dist = Location.distance(U.location, E.location)  // hex distance

    // Can U detect E at this range?
    if dist > U.type.detectionRange: skip

    // Can U see E given vision type rules?
    // (Existing check: subs hidden from SURFACE, air/land hidden from WATER)
    if !E.isVisible(U.type.vision): skip

    // Is E identified?
    identified = (dist <= U.type.identificationRange)

    // Submarine auto-ID: if detected via WATER/COMPLETE vision, always identified
    if E.type == SUBMARINE and (U.type.vision == WATER or U.type.vision == COMPLETE):
      identified = true

    // Update contact with best info
    contactMap.updateContact(E, E.location, identified, U.type.spotterQuality, currentTurn)

// Repeat for each owned city (detection=3, identification=3, quality=2, COMPLETE vision)
For each owned city C:
  For each enemy unit E:
    dist = Location.distance(C.location, E.location)
    if dist > 3: skip
    // Cities have COMPLETE vision, so all units are visible
    contactMap.updateContact(E, E.location, true, 2, currentTurn)
```

### 2.3 Contact Update Rules

When the same enemy unit is detected by multiple friendly units in one turn:

- `location` is always the enemy's current position (it's visible right now)
- `identified` upgrades from false to true but never downgrades
- `spotterQuality` keeps the highest value seen this turn
- `turnDetected` refreshes to the current turn

### 2.4 Hex Distance

The detection scan uses proper hex distance via `Location.distance()`, not the square-grid iteration used by the existing `markRegion()`. This is a deliberate improvement for the contact system only. Terrain fog-of-war (`markRegion`) is left unchanged to avoid regression.

---

## 3. AI Integration

### 3.1 New Query Methods on Player

In addition to the existing `getKnownEnemies()` (currently visible enemy units):

- `getAllContacts()` — returns current + ghost contacts from ContactMap
- `getGhostContacts()` — returns only ghosts (stale but not yet expired)
- `getIdentifiedContacts()` — returns contacts where type is known (current or ghost)

`getKnownEnemies()` continues to work as today for backwards compatibility. It returns actual `Unit` references for currently visible enemies only.

### 3.2 ThreatMap Ghost Layer

`ThreatMap` gains an optional ghost threat layer. Ghost positions contribute to threat assessment at reduced weight:

- Current contact: full threat weight (1.0)
- Ghost, 1 turn old: 50% threat weight (0.5)
- Ghost, 2+ turns old: 25% threat weight (0.25)

This ensures the AI doesn't ignore a battleship it saw 2 turns ago just because it moved out of sight. It will still patrol the area and allocate appropriate counters.

### 3.3 Captain Pursuit

Captains that pursue enemies (DestroyerCaptain, CruiserCaptain, FighterCaptain, BomberCaptain) can target ghost positions as move destinations:

- If a ghost is identified (type known), the captain can make smart choices (e.g., send a destroyer after a submarine ghost, don't send infantry after a sea ghost)
- If a ghost is unidentified, the captain only knows the travel type — it can send an appropriate unit category (naval vs land vs air) to investigate

### 3.4 No Changes to Combat

Combat still requires actual current visibility. You cannot attack a ghost. You move to the ghost position to investigate, and if the enemy is still there (or nearby), normal detection kicks in and combat can proceed.

---

## 4. UI Rendering

### 4.1 Four Visual States

| State | Position | Type | Rendering |
|-------|----------|------|-----------|
| Fully visible | Current | Known | Normal unit icon, full opacity (today's behavior) |
| Detected | Current | Unknown | Solid silhouette by travel class, full opacity |
| Ghost-identified | Stale | Known | Semi-transparent unit icon |
| Ghost-unidentified | Stale | Unknown | Semi-transparent silhouette by travel class |

### 4.2 Silhouette Icons

Three new icon assets needed, one per travel class:
- **Land silhouette** — generic soldier/vehicle shape
- **Sea silhouette** — generic ship shape
- **Air silhouette** — generic aircraft shape

Silhouettes are drawn in the enemy player's color so the player knows whose contact it is, just not what type.

### 4.3 Ghost Rendering

- Ghosts render at a single reduced opacity level (e.g., 40-50%). No gradual fade by age.
- No turn counter badge on ghosts.
- Ghosts either exist or they don't — binary presence.
- One ghost per hex. When multiple ghosts overlap, display the best one per the priority in section 1.6.

### 4.4 MapCanvas Changes

The unit draw loop changes from the current visibility check to reading from `ContactMap`:

```
For each contact in player.contactMap.getAllContacts():
  loc = contact.location
  isCurrent = (contact.turnDetected == currentTurn)
  isIdentified = contact.identified

  if isCurrent and isIdentified:
    draw normal unit icon at full opacity
  else if isCurrent and !isIdentified:
    draw travel-class silhouette at full opacity
  else if !isCurrent and isIdentified:
    draw normal unit icon at ghost opacity
  else: // ghost, unidentified
    draw travel-class silhouette at ghost opacity
```

Own-unit rendering is unchanged. Terrain fog-of-war (explored vs unexplored hexes) is unchanged.

### 4.5 Contacts on Unexplored Hexes

Contacts can appear on unexplored (dark) hexes. If a cruiser detects a ship 4 hexes into unexplored territory, the contact marker renders on the dark hex. Detection does not auto-explore the hex — the player gets a "radar blip" without learning the terrain.

---

## 5. Backwards Compatibility

### 5.1 What Doesn't Change

- `Vision` enum — still NONE, SURFACE, WATER, COMPLETE
- `visible[][]` array — still governs terrain fog-of-war
- `explored[][]` array — still governs permanent exploration
- `Unit.isVisible(Vision)` — still used for the detection check (subs hidden from SURFACE, etc.)
- `markRegion()` — still uses square distance for terrain fog-of-war
- `getKnownEnemies()` — still returns currently visible enemy Unit references
- Combat system — no changes
- Movement system — no changes
- Save/load — ContactMap is not serialized to disk. On game load, it starts empty and repopulates from current visibility on the next turn. Ghosts from before the save are lost, which is acceptable given their short lifetime (2-4 turns)

### 5.2 What Gets Replaced

- `lastSeenEnemies` (Map<Long, LastSeenInfo>) — replaced by ContactMap. LastSeenInfo is removed.
- The ghost cleanup logic in `cleanupLastSeenEnemies()` — replaced by ContactMap.refreshContacts()
- The rendering check in MapCanvas that queries `getVisibility(loc) != NONE` for enemy units — replaced by ContactMap queries

### 5.3 What Gets Added

- `Contact` class (new file)
- `ContactMap` class (new file)
- Three new fields on `Type` enum: detectionRange, identificationRange, spotterQuality
- Three silhouette icon assets (land/sea/air)
- New query methods on Player: getAllContacts(), getGhostContacts(), getIdentifiedContacts()
- Ghost threat layer in ThreatMap
- Ghost pursuit logic in relevant captains

---

## 6. Existing Bug: Square Vision Range

The current `markRegion()` method uses square iteration (`x-dist..x+dist, y-dist..y+dist`) rather than hex distance for terrain fog-of-war. This means vision range is a square, not a hex circle. This spec does NOT fix that — the new contact system uses hex distance via `Location.distance()`, but terrain fog-of-war retains its existing behavior to avoid regression. Fixing `markRegion()` is a separate task.
