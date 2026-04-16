# Search and Destroy - Game Rules and Development Guide

# Style
- Be concise. Minimize explanations unless asked.
- Just make the changes — don't narrate what you're doing or why unless it's non-obvious.
- Skip summaries of what you changed after edits.
- 

## Overview

A hex-grid, turn-based strategy game. Players start with one city and one infantry unit. They must explore the map, capture cities, produce units, and destroy all enemies to win.

## Map

- Hex grid with two terrain types: **land** and **water**
- Land is grouped into **continents** (computed by flood-fill at board creation)
- Cities are always on land tiles
- Players start at coastal cities on different continents

## Victory and Defeat

A player loses when they have no cities AND no land units that could capture a city. The remaining player wins.

## Stacking Rules

**1 unit per hex**, with three exceptions:
1. **Cities** - units can stack freely in a city
2. **Transports/Carriers** - carried units stack up to carry capacity
3. **Air units** - can end their turn on the same hex as one friendly land or sea unit

Use `Game.canPlaceUnit(unit, location)` for all stacking checks.

## Units

### Unit Stats Table

| Type | Travel | Moves | Hits | Attack | Cost | Fuel | Vision | VDist | Weight |
|------|--------|-------|------|--------|------|------|--------|-------|--------|
| Infantry | LAND | 1 | 2 | 1 | 5 | - | SURFACE | 1 | 1 |
| Armor | LAND | 2 | 4 | 2 | 10 | - | SURFACE | 1 | 2 |
| Fighter | AIR | 5 | 2 | 1 | 10 | 20 | SURFACE | 3 | 1 |
| Bomber | AIR | 4 | 2 | 3 | 15 | 32 | SURFACE | 3 | 0 |
| Cargo Plane | AIR | 3 | 2 | 0 | 15 | 18 | SURFACE | 5 | 0 |
| Destroyer | SEA | 3 | 3 | 3 | 20 | - | COMPLETE | 2 | 0 |
| Transport | SEA | 2 | 2 | 0 | 30 | - | SURFACE | 1 | 0 |
| Submarine | SEA | 2 | 4 | 4 | 30 | WATER | 2 | 0 |
| Cruiser | SEA | 2 | 8 | 3 | 40 | - | COMPLETE | 3 | 0 |
| Carrier | SEA | 2 | 6 | 1 | 50 | - | SURFACE | 2 | 0 |
| Battleship | SEA | 2 | 12 | 4 | 50 | - | SURFACE | 2 | 0 |

- **Moves** = hexes per turn (`dist`)
- **Fuel** = `maxFuelMultiplier * dist` (-1 means unlimited). Air units die when fuel runs out.
- **Cost** = production turns in a city
- **Weight** = space consumed when carried (Infantry=1, Armor=2)

### Carrying

| Carrier Type | Capacity | Carries |
|-------------|----------|---------|
| Transport | 6 | Infantry, Armor |
| Cargo Plane | 1 | Infantry (only loads in owned cities) |
| Aircraft Carrier | 6 | Fighter |

### Movement Rules

- **LAND** units can only move on land hexes
- **SEA** units can only move on water hexes (and into cities)
- **AIR** units can move on any hex. They consume fuel every move and must land in a friendly city or on a carrier before fuel runs out or they die.
- Moving into your own city ends your turn and triggers repair (+1 hit) and refuel
- A unit's moves reset each turn via `Life.resetForTurn()`

## Transport Loading and Unloading

Transports and Cargo aircraft are the only way to move infantry and armor across the sea. This is fundamental to gameplay.

### Transport States

A transport has three states: **loading**, **unloading**, and **normal**.

- **Loading** = Sentry mode in a city or on the coast. `autoLoad()` picks up compatible units. A transport cannot move while in loading state.
- **Unloading** = `unloadingMode` is true. Set when the Unload order executes. A transport can still move while unloading.
- **Normal** = neither loading nor unloading.

### unloadingMode Lifecycle

- Set to `true` when the Unload order executes
- Persists across turns
- Cleared when the transport moves away from coast (no adjacent land hexes) OR when the cargo list becomes empty (`removeCarried()`)

### Loading Flow

1. **Auto-load at cities** (`Unit.autoLoad()`) - transports/carriers in sentry at a city automatically pick up compatible units that don't have active non-sentry orders
2. **Auto-load on movement** (`Unit.move()`) - land units moving onto a hex with a friendly transport auto-load; fighters auto-load onto carriers
3. **Movement resolver** (`MovementResolver.resolveLoad()`) - land units trying to move to water with a transport there get loaded

### Unloading Flow

- **AI players**: `Unit.unload()` bulk-moves all carried units to adjacent valid hexes
- **Human players**: `wakeDisembarkableUnits()` wakes carried units one at a time. The player is prompted to move each unit individually (or sentry it to stay aboard).
- A carried unit is only woken if `canDisembark()` returns true — there must be at least one adjacent hex that is travelable for that unit type AND passes `canPlaceUnit()` stacking rules.
- Wake checks happen at: start of turn (`beginTurn()`), and after each transport move step (`Unit.move()`)

## Combat

### Unit vs Unit
- Attacker and defender trade blows (50% hit chance per round for each)
- Damage = `ceil(baseAttack * effectivenessMultiplier)` from `UnitMatchups`
- Continues until one unit dies (hits reach 0)
- Loser is killed; winner survives with remaining hits

### City Capture
- Only **land units** can capture cities
- The attacking unit is always consumed on capture (win or lose)
- **Unowned city**: 50% capture chance
- **Enemy city**: attacker fights all land defenders in sequence, then 50% capture. Non-land defenders have a 25% chance of being killed as collateral.
- **Bombers** can attack enemy cities (sets back production) but cannot capture

### Bombardment
- One-way attack, no return fire
- Multiple shots with 50% hit chance each

## Cities

- Cities produce one unit type at a time, taking `Type.getCost()` turns
- Cities have **edicts** (standing orders): auto-route units, air patrol, auto-sentry
- Cities repair (+1 hit) and refuel units that enter them
- `bombCity()` resets production progress

## Vision

Four levels: `NONE`, `SURFACE` (sees everything except subs), `WATER` (sees only naval), `COMPLETE` (sees everything).

- Visibility recalculated every turn: cleared, then rebuilt from all owned units and cities
- Cities provide COMPLETE vision at distance 3
- Each unit type has a visionDistance and Vision type (see stats table)
- Submarines are only visible to COMPLETE or WATER vision

## Orders

Orders are the game-logic layer for unit behavior. Key order types:

| OrderType | Behavior |
|-----------|----------|
| MOVE | Pathfind to destination, one step per move point |
| EXPLORE | Find nearest unexplored frontier, move toward it |
| SENTRY | Sleep until activated (loading state for transports) |
| UNLOAD | For transport: enter unloading mode. For carried unit: activate it |
| PATROL | Move between waypoints (LINEAR or LOOP) |
| ATTACK | Move to target for attack |
| ESCORT | Follow another unit |
| HEAD_HOME | Pathfind to nearest friendly city |
| SKIPTURN | Explicitly skip one turn |
| DISBAND | Remove the unit |
| Directional | MOVE_EAST, MOVE_WEST, etc. - single hex in a direction |

Orders return `ResponseCode`: STEP_COMPLETE, TURN_COMPLETE, ORDER_AND_TURN_COMPLETE, ORDER_COMPLETE, YIELD_PASS, DIED, BLOCKED, CANCEL_ORDER.

## Architecture

### Threading Model

The game runs on its own thread. When it needs human input, it enters `AWAITING_ORDERS` and polls a `ConcurrentLinkedQueue<GameCommand>`. The UI submits commands to this queue from the EDT.

### Key Layers

1. **Game model** (`com.developingstorm.games.sad`) - Unit, Game, City, Board, Player, Order, Type
2. **Orders** (`...orders`) - Concrete Order subclasses (Move, Explore, Sentry, etc.)
3. **Commands** (`...commands`) - GameCommand pattern for thread-safe UI-to-game communication (AssignOrderCommand, ResumeGameCommand, etc.)
4. **Controller** (`...controller`) - GameController (writes, queued to game thread) + GameQueryService (reads, any thread)
5. **Events** (`...events`) - GameEventBus with ~20 event types, CopyOnWriteArrayList for thread safety, marshalled to EDT
6. **AI** (`...brain`) - RobotBrain -> General -> type-specific Captains (InfantryCaptain, TransportCaptain, etc.) + Battleplan + OperationsCoordinator
7. **UI** - JavaFX (`...fx`) with MapCanvasModeManager + mode strategy pattern. Swing UI (`...ui`) is legacy.

### Human vs AI Player

- `Player` is the human player class. `Robot extends Player` is the AI.
- `Player.isRobot()` returns false; `Robot.isRobot()` returns true
- Human: `unitsNeedOrders()` pauses game, selects a unit, waits for UI input
- Robot: `unitsNeedOrders()` consults the brain/captain system for each unit

### Key Patterns

- **Hex adjacency**: Always use `Location.getRing(1)` for neighbors. Never use `dx/dy` loops (that's square grid).
- **Stacking**: Always use `Game.canPlaceUnit(unit, location)`. Never check `size() < N` directly.
- **Flyweight locations**: `Location.get(x, y)` returns cached instances. Equality is reference-safe.
- **Save/Load**: ZIP-based format via GameStateSerializer/UnitSerializer. Carry relationships restored in two passes (create units, then resolve ID references).

## Build and Test

```
mvn clean compile    # Build
mvn clean test       # Build + run all tests (42 tests)
```
### Patrols
- `Patrol` is a type of order that instructs a unit to move between a set of waypoints. If the waypoints complete a loop, the unit should continue patrolling indefinitely.  If the waypoints do not complete a loop, the unit should reverse direction when reaching the end.

### Escorts
- `Escort` is a type of order that instructs a unit to follow another unit. The escorting unit should move towards the escorted unit and maintain a specified distance.
