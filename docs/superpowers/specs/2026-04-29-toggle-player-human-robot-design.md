# Toggle Player Between Human and Robot

## Goal

Allow the user to switch any player between human-controlled and AI-controlled at runtime, without restarting or reloading the game.

## Approach: factory swap, not refactor

`Robot extends Player` will stay. To switch, the existing instance produces an alternate-type instance carrying its full state, and the game rewires every reference to point at the new instance.

This is preferred over collapsing `Robot` into a `boolean isRobot` flag because:

- The state is small (~16 fields on `Player`, plus one `IBrain` on `Robot`).
- The reference rewiring is bounded and discoverable.
- It avoids touching every `instanceof Robot` / virtual-dispatch site.

## Player state to copy

All copyable as shallow references — these are the same logical objects either way:

| Field | Notes |
|---|---|
| `id`, `name` | Identity. |
| `board`, `game` | Back-references; `setGame` will reassign these on the new instance. |
| `cities`, `units` | Owned things. The list contents are reused; their `.owner` gets rewritten to the new instance. |
| `unownedCities`, `enemyCities`, `enemyUnits`, `enemyActivity` | Derived caches. Copy as-is — they will refresh on the next `buildCityLists` / vision recompute. |
| `visible[][]`, `explored[][]`, `contactMap` | Vision state. Copy as-is. |
| `pendingPlay`, `pendingOrders` | Work queues. Copy as-is — but see "Outstanding orders" below. |
| `endTurnRequested` | Volatile flag. Copy. |
| `edictFactory` | Holds `this` — must be reconstructed on the new instance. |

`Robot.brain` is recreated by `Robot.setGame()` when going Human → Robot. Going Robot → Human, the brain is dropped.

## References to rewire

A swap means finding everything pointing at the *old* instance and pointing it at the new one. The known holders:

1. **`Game.players[]`** — array slot for this player.
2. **`Game.currentPlayer`** — if it currently points at the swapped player.
3. **`City.owner`** — for every city in `oldPlayer.cities` (including unowned cities they "track" — actually those are not owned, just observed; only `cities` where `ownsCity(c)` is true need rewriting).
4. **`Unit.owner`** — for every unit in `oldPlayer.units`.
5. **`VisionManager.visionMaps`** — `Map<Player, byte[][]>` keyed by Player. Migrate the entry: `put(new, map.remove(old))`.
6. **`Fleet.owner`** — currently `final`. Must be made non-final, plus a package-private `replaceOwner(Player)` method on `Fleet`. Iterate `FleetRegistry.allFleets()` and rewrite any whose `owner == oldPlayer`.
7. **AI-internal back-refs** — `RobotBrain.player`, `EdictGovernor.player`, `Edict.player`, `OperationsCoordinator.player`, `ThreatMap.player`, `ContinentStrategy.player`, `ContinentClassifier.player`. These are reachable only from the old `Robot.brain`; going Robot → Human they're dropped with the brain. Going Human → Robot, `Robot.setGame()` constructs a fresh `RobotBrain` which builds these from scratch — no migration needed.

`MapState`'s static `player` field is per-pathfinding-call scratch and not a long-lived holder.

Event objects already in flight (`PlayerSelectedEvent`, `TurnCompleteEvent`, etc.) snapshot the old reference; that's fine — they're consumed and discarded.

## Prerequisite fix: `Player.equals()`

`Player.equals()` (line 71-78) currently does `if (getClass() != obj.getClass()) return false;`. With `hashCode()` using only `id`, this is already an inconsistency. After a swap, `VisionManager.visionMaps.get(swappedPlayer)` would silently return `null`.

**Fix**: drop the `getClass()` check; compare by `id` only. Standalone correctness fix that lands first.

## API additions

### `Player`

```java
/** Returns a Robot carrying this player's state. Subclasses override. */
public Player toRobotForm() { ... }   // base impl: builds new Robot, copies state

/** Returns a human Player carrying this player's state. Subclasses override. */
public Player toHumanForm() { ... }   // base impl: returns this (already human)
```

`Robot` overrides:

```java
@Override public Player toRobotForm() { return this; }
@Override public Player toHumanForm() { ... } // builds new Player, copies state
```

A protected `copyStateInto(Player target)` helper on `Player` does the shallow field copy. Subclass-specific state (the brain) is *not* copied — going Human → Robot, `setGame()` builds a brain; going Robot → Human, the brain is discarded.

### `Game`

```java
/**
 * Replace a player with a swapped-type instance built from the same state.
 * Rewrites every reference (players[], unit/city owners, fleets, vision map).
 * Must be called between turns.
 */
public void replacePlayer(Player oldPlayer, Player replacement) { ... }

/** Convenience: toggle the given player's type, returning the new instance. */
public Player togglePlayerType(Player p) {
    Player swapped = p.isRobot() ? p.toHumanForm() : p.toRobotForm();
    replacePlayer(p, swapped);
    return swapped;
}
```

### `Fleet`

- `owner` becomes non-final.
- Add package-private `void replaceOwner(Player newOwner)` used only by `Game.replacePlayer`.

## When the swap is allowed

**Between turns only.** Specifically: after a `TurnCompleteEvent` and before the next player's turn begins. Mid-turn swaps risk in-flight order steps reading torn state (Unit.owner already rewritten, but a strategy object on the old brain still referencing the old Player).

The `GameController` exposes `togglePlayerType(Player)` which posts a `TogglePlayerTypeCommand` onto the game-thread queue. The game thread executes it at the next safe boundary (between turns).

## UI

Menu addition: **Players → Toggle "<Name>" Human/AI**. Submenu lists each player; the label reflects current state ("Human" or "AI"). Disabled while a turn is in progress; enabled while the game is in `AWAITING_ORDERS` between turns. Available in `GameMenuBar`.

After the swap completes, fire a new `PlayerTypeChangedEvent` so the UI can refresh status displays. Existing `MAP_UPDATED` is also fired since the active controller changes.

## Save/load

No format change. `GameStateSerializer.serializePlayer` already writes `isHuman`. `deserializePlayer` already picks `Player` vs `Robot` based on that flag. The swap is a runtime operation that produces the same shape that the serializer already supports.

## Outstanding orders at swap time

When a Robot becomes Human:
- `pendingPlay` and `pendingOrders` are kept — the human will see whichever units the AI had queued for play and provide orders for them.
- Existing `Order`s on units are kept. The human can override via the UI.

When a Human becomes Robot:
- The new `Robot.setGame()` builds a fresh `RobotBrain`.
- On the next `unitsNeedOrders()` call, the brain assigns orders to any unit lacking them. Units with existing orders keep them — same as a fresh game load.

This avoids a "kill all orders" scrub, which would otherwise lose meaningful in-flight movement.

## Testing

1. **Equals fix**: a unit test that `new Player("X", 1).equals(new Robot("X", 1))` is true and they share a hash.
2. **State preservation**: build a player with cities, units, vision; call `togglePlayerType`; assert all of those survived on the new instance and that `Game.players[]`, every unit/city `owner`, the vision-map key, and every fleet's owner now point at the new instance.
3. **Round-trip idempotence**: `human → robot → human` produces a player whose `units`, `cities`, vision arrays, and pending queues are identical (by reference) to the start.
4. **Save round-trip after swap**: swap mid-game, save, load — both players in the loaded game have the post-swap types.
5. **Turn execution after swap**: swap a robot to human between turns; on its next turn the game enters `AWAITING_ORDERS` correctly. Swap a human to robot between turns; on its next turn the brain assigns orders without intervention.

## AI-vs-AI execution

Toggling all players to robot enables AI-vs-AI play. The game loop already supports this — when no current player is human, it never enters `AWAITING_ORDERS` and runs robot turns continuously (`HeadlessGameRunner` proves this works headlessly).

### Pause/resume

A continuously-running AI-vs-AI game needs a way for the user to halt processing and inspect state. `GameState.PAUSED` already exists in the enum but isn't currently entered by anything user-driven (the existing `Game.pause()` actually transitions to `AWAITING_ORDERS` — misnamed legacy). Wire it up.

**Game-side:**

- `Game.togglePauseExecution()` — flips `RUNNING` ↔ `PAUSED`. Ignored when state is `AWAITING_ORDERS` or `GAME_OVER`.
- `Game.checkPause()` — if state is `PAUSED`, blocks the game thread on `wait()` until resumed. Called once per main-loop iteration in `runGameLoop` (between players' turns). Per-turn granularity is sufficient and simpler than threading the check into `Player.play()`.
- `signalGameThread()` already wakes the thread; extend it to also handle `PAUSED → RUNNING`.
- Publish new `ExecutionPausedEvent` / `ExecutionResumedEvent` so the UI can update.

**UI-side:**

- `GameView` registers a Spacebar key handler on the scene that calls `controller.togglePauseExecution()`.
- Add *Game → Pause/Resume* menu item showing the shortcut, also calling the same controller method.
- When paused, show a "⏸ PAUSED" indicator (e.g., overlay on `turnLabel` or a separate status badge); restore on resume.

**Edge cases:**

- Pausing while a human is `AWAITING_ORDERS`: ignore (already effectively paused).
- After `GAME_OVER`: ignore.
- Save/load: don't persist the pause flag — loaded games start `RUNNING`.

### Out of scope (AI-vs-AI)

- A "New AI vs AI Game" entry point. Players can still toggle to AI from a default human-vs-AI start.
- Configurable per-step delay for visual pacing during AI-vs-AI playback.

## Out of scope

- A confirmation dialog before swapping. The action is reversible.
- Per-difficulty AI configuration when promoting to robot. Uses default `RobotBrain`.
- Hot-swapping mid-turn. Explicitly disallowed.

## Risks

- **Missed reference holder.** Mitigated by the rewiring test (#2). Anything we miss will manifest as the old instance still being referenced; tests assert otherwise.
- **`Fleet.owner` un-finalization.** Small surface; the setter is package-private and called only from `Game.replacePlayer`.
- **`Player.equals` change rippling.** Audit all `Set<Player>` / `Map<Player, ?>` usages to confirm no caller depended on the class-aware equality. Current candidates: `VisionManager.visionMaps` (one entry per player; rewriting the same id is the desired behavior) — no other suspects in the source tree.
