# Robot AI: Measurement + Unit Commitments

Sub-projects 0 and 1 of the robot AI improvement roadmap.

## Intent

**Goal (from Pete):** a *believable* opponent: plays sensibly and fairly, with intent a
human watching can read. Winning more is not the target.

**Priority symptoms (from Pete):** aimless units, failed invasions, wasted navy/air.
Defense matters less.

**Assumptions (not stated by Pete):**
- Fair means no omniscience: the AI acts only on what it has seen.
- Headless AI-vs-AI runs are the measuring stick for behavior, not for win rate.
- Keep the existing RobotBrain → General → Captain structure. Grow toward
  "missions" (approach B) later without a rewrite.

## Roadmap (context)

| # | Sub-project | Fixes |
|---|-------------|-------|
| 0 | Behavior metrics + baseline | Lets us prove each step helps |
| 1 | One persistent memory + unit commitments | Aimless units |
| 2 | Invasion pipeline on commitments | Failed invasions |
| 3 | Navy and air | Wasted navy/air |
| 4 | Remaining fairness + dead code + AIConfiguration | Cheating, clutter |
| B | Commitments become missions that issue their own orders; UI "why" | Readability |

**This spec covers 0 and 1 only.** Each later step gets its own spec.

---

## Sub-project 0: Behavior metrics

### What we measure

These metrics are AI-agnostic: they come from game state and events, not from AI internals. That
means the same numbers work for today's AI (the baseline) and for the improved one. All metrics are
per player and split by travel class (LAND / SEA / AIR) where noted.

| Metric | Definition | Split |
|--------|-----------|-------|
| **Oscillation rate** | Share of unit moves that enter a hex the same unit occupied in its previous 3 turns | L/S/A |
| **Retarget rate** | Changes of a unit's order destination per 10 unit-turns while the previous destination was not reached | L/S/A |
| **Idle rate** | Share of unit-turns with no movement. Excludes carried units, units in sentry, and transports in a city | L/S/A |
| **Sea load** | Mean cargo count of transports per move made on a hex with no adjacent land | — |
| **Empty sailing** | Share of those transport moves carrying 0 cargo | — |
| **Landings** | Land units that step from a transport onto a continent other than the one they boarded from | — |
| **Overseas captures** | Cities captured on a continent where the player owned no city at the start of that turn | — |
| **Fuel losses** | Air units killed with hits > 0 (i.e., out of fuel) | — |
| **Naval attacks** | Combats where the attacker is a SEA unit, per 100 SEA unit-moves | — |
| Also | Winner, turns, end reason, cities per player at end | — |

"Order destination" means the target of a `Move` order. Units with other orders (Explore,
Sentry, Patrol and so on) don't count toward retargets. A switch *from* Move to a different order
type before arriving counts as one retarget.

### Components

- **`AIBehaviorMonitor`** (new, `sad.testing`).
  - A `GameEventListener` for `UNIT_MOVED`, `UNIT_KILLED`, `COMBAT_RESOLVED` and `NEW_TURN`.
  - Also takes a snapshot of each player's units, carried state, orders and city ownership at
    every `NEW_TURN`.
  - Keeps a small per-unit history (last 3 turns of hexes, last destination, boarding continent).
  - Produces a `BehaviorReport` (per-player metric values plus raw counts).
- **`BehaviorReport`** (new): plain data. `toJson()`, `fromJson()`, and `average(List<BehaviorReport>)`.
- **`HeadlessGameRunner`** changes:
  - Optionally attach a monitor.
  - Include the report in `GameResult`.
  - Honor `run(timeoutSeconds)`.
  - Label the per-turn line by round, not by an assumed player index.
- **`RandomUtil.setSeed(long)`** (new): lets a run be repeated.
  - Determinism is best effort. Hash-order iteration may still vary, so comparisons use
    averages over many games, never single games.
- **`AITestCLI metrics`** (new command):
  `metrics [--games N] [--turn-limit T] [--map M] [--seed S] [--out file.json] [--compare baseline.json]`.
  - Runs N games **sequentially**. Parallel games share static state (RandomUtil, A* statics,
    LocationMap) and are not safe.
  - Swaps sides every game and seeds game *i* with `S + i`.
  - Prints a metric table: mean and standard error for each metric, averaged over both players.
    Both players run the same AI.
  - With `--compare`, adds a Δ column and flags regressions.

### Baseline

Before any AI change, run `metrics --games 20 --turn-limit 150 --seed 1000` on `MedMap.sdm` and
`BorderLand.sdm`. Commit the results to `ai-configs/baselines/2026-09-baseline-{map}.json`.

### Testing

- Unit tests for `AIBehaviorMonitor`. Feed it synthetic events and snapshots, and assert each
  metric's arithmetic: oscillation, retarget, idle exclusions, landing detection, fuel-loss
  detection.
- One smoke test that runs a 10-turn headless game with the monitor attached and asserts that the
  report exists and its counts are non-negative. No assertions on AI quality.

---

## Sub-project 1: One memory + unit commitments

### Problems addressed

1. `RobotBrain.startNewTurn` (`RobotBrain.java:51-53`) reassigns every unit's order every turn.
   Nothing a unit starts survives the turn.
2. `Battleplan` creates a fresh `StrategyMemory` each turn (`Battleplan.java:99`), separate from
   the one `OperationsCoordinator` keeps. Roles (DEFENDER, ESCORT, INVADER) vanish.
3. Random choices: `General.shouldDefend` (`General.java:161`) is a coin flip per call.
   (`UnitCaptain.attackShipStrategy`'s random pick belongs to sub-project 3.)
4. Target choice sees the whole board: `TargetPrioritizer.prioritizeCities` scans every city,
   including unexplored ones, and reads enemy production.
5. `InfantryCaptain` and `ArmorCaptain` are the same code.
6. `isInProducingCity` treats every city hex as "just produced", so land units in cities never
   defend and always leave.

### Design

**One memory.**
- `RobotBrain` owns the single `StrategyMemory`, as it already does for the coordinator.
- `Battleplan` receives it as a constructor argument instead of creating one.
- Anything the memory stores lasts across turns. `StrategyMemory.startNewTurn()` still prunes
  dead units.

**Commitments.**
- A `Commitment` is what a unit is currently trying to do:

  ```
  Commitment { Kind kind; Location target; long targetUnitId (optional);
               int startTurn; int lastProgressTurn; int bestDistance }
  ```

- `StrategyMemory` holds a `Map<unitId, Commitment>`, plus `commit(unit, kind, target)`,
  `get(unit)` and `release(unit, reason)`.
- `Kind` is an enum with a fixed priority. Each kind defines when it is complete and when it is
  invalid:

| Kind (priority high→low) | Set by | Complete when | Invalid when |
|---|---|---|---|
| OPERATION | coordinator assignment | unit released by coordinator | operation gone or unit not in it |
| DEFEND(city) | defensive strategy | no known enemy within 3 of city for 2 consecutive turns | city lost |
| CAPTURE_CITY(city) | `occupyUnownedCity`, `moveTowardBestCity` (target on the unit's continent) | we own the city | no progress for 3 turns; or no land path from the unit to the city |
| STAGE(loadingPoint) | `goToLoadingPoint`, and `moveTowardBestCity` when the best target is overseas | unit is carried | hex is no longer a loading point |
| EXPLORE | `explore` | no reachable frontier | — |

- "Progress" means the path distance to the target shrank. `lastProgressTurn` and `bestDistance`
  track it. A no-progress limit is what stops a unit from being permanently stuck.
- The kinds and their rules live in one class, `CommitmentRules`, with pure methods:
  `status(commitment, unit, knowledge) → ACTIVE | COMPLETE | INVALID`. Captains don't hold these
  rules, which makes them unit-testable.

**The replacement rule.** This is what fixes aimlessness. A unit's commitment can end in exactly
three ways:
1. **Complete**: its goal is met.
2. **Invalid**: the rules say it can no longer succeed.
3. **Pre-empted by a strictly higher-priority kind.**
   - The coordinator assigns the unit to an operation.
   - The defensive trigger fires for a city the unit can reach: an enemy within 3 hexes, or the
     unit is the nearest land unit to a threatened city.
   - A known unowned city becomes reachable for a unit that is EXPLOREing or STAGEing (an upgrade
     to CAPTURE_CITY).

Anything else keeps the commitment. Each release is logged with its reason. The log is written as
`Log.info(unit, "commitment <kind> ended: <reason>")`, which is readable in `game_test.log` and is
the seed of the later "why" UI.

**One decision path.**
- `RobotBrain.orderFor(unit)` replaces both the start-of-turn loop and `getOrders`.
- `Robot.unitsNeedOrders` calls it for units that finish their order mid-turn.

```
orderFor(u):
  c = memory.get(u)
  if c != null:
     s = rules.status(c, u, knowledge)
     if s == ACTIVE and no pre-emption applies:
        if u.hasOrders() and orderServes(u.getOrder(), c): return KEEP   // don't reassign
        return captain.continueCommitment(u, c)                         // fresh order to same target
     memory.release(u, s or "pre-empted by X")
  return general.getOrders(u)   // captain picks a new goal and calls memory.commit(...)
```

- `orderServes(order, c)`: the order is a `Move` whose destination is `c.target` (or the loading
  point, for STAGE), or an `Explore` for EXPLORE, or the operation's current phase order for
  OPERATION. It is false for any other order.
- At turn start, `RobotBrain` calls `orderFor` for each unit and assigns only when the result is
  not KEEP.
- Units whose captains don't commit (navy and air, until sub-project 3) have no commitment, so
  they are re-planned every turn exactly as today. This keeps sub-project 1's behavior change to
  land units and transports.

**Transports.**
- A transport with cargo that has picked an unloading point commits `CAPTURE_CITY` to the target
  city near that point. The reason: the unloading point is chosen *for* a target, and the
  commitment follows the target, not the hex.
- Full invasion rework is sub-project 2. Here the goal is only that a transport stops re-picking a
  destination every turn.

**Determinism.** Replace `General.shouldDefend`'s coin flip: use the defense zone when any owned
city is threatened, otherwise the expansion zone.

**Fair targets.**
- `TargetPrioritizer.prioritizeCities` only considers cities on hexes the player has explored.
- Scoring drops the enemy-production term.
- It still uses current ownership of explored cities. Last-seen ownership memory is deferred to
  sub-project 4.

**Land captain merge.** `InfantryCaptain` and `ArmorCaptain` become one `LandCaptain`. `General`
routes both types to it.

**"Just produced" fix.**
- Add `Unit.getCreatedTurn()`, set when the unit is created.
  - Persist it in `UnitSerializer`.
  - Old saves default to 0, so their units count as not just produced.
- `isInProducingCity` becomes `createdTurn == currentTurn`.

### Error handling

- If a captain returns null for a committed unit, release the commitment and fall back to
  `general.getOrders`.
- `General.getOrders` already falls back to skip-turn.
- Commitments for dead units are pruned in `StrategyMemory.startNewTurn()`.
- Brain state is not saved. After loading a game, or swapping a player between human and robot,
  commitments start empty, as today.

### Testing

**Unit tests for `CommitmentRules`:**
- each kind's complete and invalid conditions;
- no-progress expiry;
- the priority table and pre-emption rules.

**`RobotBrain` tests on a small built game:**
- A unit with an active CAPTURE_CITY keeps its order across a turn (no reassign).
- A completed commitment is released and re-planned.
- An EXPLOREing unit upgrades to CAPTURE_CITY when a known unowned city becomes reachable.
- An operation assignment pre-empts.

**Fairness test:** `prioritizeCities` never returns a city on an unexplored hex.

**Metrics:** run `metrics` against the committed baseline on both maps.

### Success criteria (vs. baseline, 20 games, both maps)

- LAND oscillation rate down ≥ 50%.
- LAND retarget rate down ≥ 50%.
- LAND idle rate not up by more than 10% (relative).
- Landings and overseas captures not down by more than 10%.
- SEA and AIR metrics unchanged within noise (±2 standard errors). Those units aren't touched yet.
- Mean turn time not up by more than 25%.
- All existing tests pass.

If a criterion fails, stop and review before continuing to sub-project 2.

## Out of scope (later sub-projects)

- Invasion pipeline fixes: load thresholds, rally points, orphaned operation units, retry bug
  (2).
- Warship targeting, air fuel safety, odds checks (3).
- Remaining omniscience (`ContinentClassifier`, adjacency), ownership memory, dead code,
  `AIConfiguration`, production-pair lock-in (4).
- Mission objects and UI "why" display (B).
