# Robot AI Measurement + Commitments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Measure robot AI behavior headlessly, then stop land units and transports from
re-deciding their goal every turn by giving each unit a persistent, rule-governed commitment.

**Architecture:**
- **Measurement.** A pure `BehaviorTracker` computes metrics from per-player end-of-turn unit
  snapshots and a few events. An `AIBehaviorMonitor` adapts `Game` state and events into that
  tracker. `AITestCLI metrics` runs seeded games sequentially and compares the results against a
  committed baseline JSON.
- **Commitments.** A single `StrategyMemory` owned by `RobotBrain` holds a `CommitmentBook`.
  `CommitmentRules` decides whether each commitment is active, complete or invalid.
  `RobotBrain.orderFor` keeps a unit's current order while its commitment is active and not
  pre-empted. Land captains and the transport captain record commitments when they choose goals.

**Tech Stack:** Java 17+, Maven, JUnit 5. The project's own `JsonObj`, `JsonFormatter` and
`JsonParser` handle JSON.

**Spec:** `docs/superpowers/specs/2026-09-30-robot-ai-commitments-design.md`

## Global Constraints

- Build/test with `mvn clean test`; all existing tests must keep passing.
- Metrics games run **sequentially**. Static state (`RandomUtil`, A* statics in `MapState`,
  `LocationMap`, `GameNames`) is not thread-safe.
- Navy and air captains are not changed. They get no commitments, so they are re-planned every
  turn exactly as today.
- Brain state is not persisted. Commitments start empty after a load or a human↔robot swap.
- Log commitment releases as `Log.info(unit, "commitment <KIND> ended: <reason>")`.
- Hex adjacency uses `Location.getRing(1)`. Stacking checks use `Game.canPlaceUnit`.

## Review Focus

1. **Unit killed while carried.** The tracker must not count a landing or keep stale history for a
   dead unit. Test: `BehaviorTrackerTest.deadUnitHistoryDropped`.
2. **Committed unit whose target city was captured by someone else.** It should go to COMPLETE for
   us or stay ACTIVE for enemy-owned, never throw. Test:
   `CommitmentRulesTest.captureTargetTakenByEnemyStaysActive`.
3. **Commitment target unreachable after a new obstruction.** The no-progress expiry must release
   it within 3 turns. Test: `CommitmentRulesTest.noProgressExpires`.
4. **Old save with no `createdTurn` field.** It must load with 0 and not NPE. Test:
   `UnitSerializerCreatedTurnTest.missingFieldDefaultsToZero`.
5. **Player with zero units or cities in a snapshot.** Metrics divide safely and report 0. Test:
   `BehaviorTrackerTest.emptyPlayerReportsZeros`.

---

## Part 0: Measurement

### Task 1: BehaviorTracker + BehaviorReport (pure metric logic)

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/testing/UnitSnapshot.java`
- Create: `src/main/java/com/developingstorm/games/sad/testing/BehaviorTracker.java`
- Create: `src/main/java/com/developingstorm/games/sad/testing/BehaviorReport.java`
- Test: `src/test/java/com/developingstorm/games/sad/testing/BehaviorTrackerTest.java`

**Interfaces:**
- Produces:
  - `record UnitSnapshot(long id, Travel travel, Location loc, boolean carried, boolean idleExempt, boolean transport, int cargo, boolean openSea, Location moveDest, int landContinentId)`.
    `landContinentId` is -1 if not on land.
  - `BehaviorTracker.onTurnEnd(int playerId, List<UnitSnapshot> units, Map<Location,Integer> ownedCityContinents)`
  - `BehaviorTracker.onUnitKilled(int playerId, long unitId, Travel travel, boolean outOfFuel)`
  - `BehaviorTracker.onCombat(int attackerPlayerId, Travel attackerTravel)`
  - `BehaviorTracker.report() → BehaviorReport`
  - `BehaviorReport.get(String key) → double`, `keys()`, `toJson() → JsonObj`,
    `static fromJson(JsonObj)`, `static average(List<BehaviorReport>) → BehaviorReport`, and
    `stdErr(String key)`, which is populated by `average`.
  - Metric keys: `LAND.oscillation`, `SEA.oscillation`, `AIR.oscillation`, `LAND.retarget`,
    `SEA.retarget`, `AIR.retarget`, `LAND.idle`, `SEA.idle`, `AIR.idle`, `seaLoad`,
    `emptySailing`, `landings`, `overseasCaptures`, `fuelLosses`, `navalAttacksPer100`.
    All are averaged over players present.

**Metric computation (per player, then averaged over players):**
- A unit-turn is counted when the unit appears in two consecutive snapshots of the same player.
- **Moved** means `loc != prevLoc`.
- **Oscillation** = moved ∧ loc ∈ {loc at t−2, loc at t−3}, divided by moved unit-turns of that
  travel class.
- **Retarget**: the previous `moveDest` was D (non-null), `prevLoc != D`, `loc != D`, and the
  current `moveDest != D`. Reported per 10 unit-turns of that travel class.
- **Idle** = ¬moved ∧ ¬idleExempt, divided by unit-turns with ¬idleExempt.
- **seaLoad**: the mean `cargo` over transport unit-turns that moved and end with `openSea`.
- **emptySailing**: the share of those with cargo 0.
- **Landings**: per unit, remember `lastLandContinent` whenever `!carried && landContinentId >= 0`.
  A unit-turn where prev carried, now `!carried`, `landContinentId >= 0` and
  `landContinentId != lastLandContinent` (with lastLandContinent known) counts as one landing.
- **overseasCaptures**: a city location present now but not in the previous
  `ownedCityContinents`, whose continent id is not among the previous snapshot's values.
- **fuelLosses**: the count of `onUnitKilled` calls with `outOfFuel`.
- **navalAttacksPer100** = SEA attacker combats × 100 ÷ SEA moved unit-turns.
- **Dead units**: `onUnitKilled` removes the unit's history.

- [ ] **Step 1: Write failing tests.** Create `BehaviorTrackerTest` with these tests, each building
  snapshots via a helper `snap(id, travel, x, y, ...)`:
  - `oscillationCountsReturnToRecentHex`: land unit at A, B, A over turns 1–3. Turn 3's move
    returns to A (t−2), so LAND.oscillation = 0.5 (1 oscillating of 2 moves).
  - `retargetCountsDestinationChangeBeforeArrival`: turn 1 dest D1 at A; turn 2 at B dest D2 → 1
    retarget over 1 unit-turn, so LAND.retarget = 10.0.
  - `arrivalIsNotRetarget`: turn 1 dest D at A; turn 2 at D with dest null → 0.
  - `idleExcludesExemptUnits`: a stationary sentry (exempt) and a stationary non-exempt unit →
    LAND.idle = 1.0 (1 of 1 non-exempt).
  - `landingOnNewContinent`: on continent 1, then carried, then not carried on continent 2 →
    landings 1.
  - `fuelLossCounted`: onUnitKilled AIR outOfFuel=true, then report → fuelLosses 1.
  - `overseasCaptureCounted`: owned {c1 on cont 1}, then {c1 on 1, c2 on 2} → 1, and a further
    {c1, c2, c3 on 2} → still 1.
  - `deadUnitHistoryDropped`: unit killed, then the same id never reappears; no NPE, and it's not
    counted.
  - `emptyPlayerReportsZeros`: a snapshot with no units → every key 0.0.
  - `averageComputesMeanAndStdErr`: reports with LAND.idle 0.2 and 0.4 → mean 0.3, stdErr
    0.1 (sample std / √n = 0.1414/1.414).
  - `jsonRoundTrip`.
- [ ] **Step 2:** Run `mvn -q test -Dtest=BehaviorTrackerTest` and confirm it fails to compile or
  fails.
- [ ] **Step 3:** Implement the three classes per the definitions above.
- [ ] **Step 4:** Run the test and confirm it passes.
- [ ] **Step 5:** Commit with `feat(ai-metrics): pure behavior tracker and report`.

### Task 2: AIBehaviorMonitor + HeadlessGameRunner integration

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/testing/AIBehaviorMonitor.java`
- Modify: `src/main/java/com/developingstorm/games/sad/testing/HeadlessGameRunner.java`
- Modify: `src/main/java/com/developingstorm/games/sad/orders/Move.java`: add
  `public Location getDestination() { return loc; }`
- Test: `src/test/java/com/developingstorm/games/sad/testing/HeadlessMetricsSmokeTest.java`

**Interfaces:**
- Consumes: Task 1's `BehaviorTracker` and `UnitSnapshot`.
- Produces:
  - `AIBehaviorMonitor(Game game)`, which implements `GameEventListener` for `TURN_ENDED`,
    `UNIT_KILLED` and `COMBAT_RESOLVED`, plus `report()`.
  - `HeadlessGameRunner.setMonitorEnabled(boolean)`.
  - `GameResult.getBehaviorReport()`, which may be null.
  - `GameResult.getMeanTurnMs()`.

**Snapshot building (on `TurnCompleteEvent` for `event.getPlayer()`):** for each unit in
`player.getUnits()`:
- `idleExempt = carried || inSentryMode || (transport && board.isCity(loc))`
- `openSea` is true when the hex is water and no `getRing(1)` neighbor is land.
- `moveDest` is `((Move) order).getDestination()` when the order is an instance of `Move`,
  otherwise null.
- `landContinentId` is `board.getContinent(loc).getId()` if the hex is land, otherwise -1. Check
  the `Continent` id accessor name.
- `ownedCityContinents` maps each of the player's cities to its continent id.

**Other events:**
- On `UnitKilledEvent`: `outOfFuel = u.getTravel()==AIR && !u.life().hasFuel()`.
- On `CombatResolvedEvent`: use `result.getAttackerOwner().getId()` and
  `result.getAttackerTypeEnum().getTravel()`.

**Runner changes:**
- Attach the monitor in `initializeGame()` when enabled.
- `run(timeoutSeconds)` records its start time. `handleTurnStart` aborts with `TIMEOUT` once the
  elapsed time exceeds a positive timeout.
- Replace the player-index label computation with the round number. Print `Round N` instead of a
  player.
- Accumulate `TurnCompleteEvent` durations to compute the mean turn time.
- Attach `monitor.report()` and the mean turn time to every returned `GameResult`.

- [ ] **Step 1: Write the smoke test.** Use `HexBoardMap.loadMapAsResource(getClass(),"MedMap.sdm")`
  and a `MockSaDBoardContext` instance. Run a runner with turnLimit 8 and monitoring enabled, then
  assert `result.getBehaviorReport() != null`, every key ≥ 0, and `getMeanTurnMs() >= 0`.
  `MockSaDBoardContext` is in the `sad` test package, so import it.
- [ ] **Step 2:** Run it and confirm it fails.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the smoke test plus the full suite and confirm they pass.
- [ ] **Step 5:** Commit with `feat(ai-metrics): behavior monitor wired into headless runner`.

### Task 3: Seeded metrics command in AITestCLI

**Files:**
- Modify: `src/main/java/com/developingstorm/util/RandomUtil.java`: add
  `public static void setSeed(long seed) { s_rand = new Random(seed); }`
- Modify: `src/main/java/com/developingstorm/games/sad/testing/AITestCLI.java`
- Test: `src/test/java/com/developingstorm/games/sad/testing/MetricsComparisonTest.java`

**Interfaces:**
- Consumes: `HeadlessGameRunner`, `BehaviorReport.average`, `fromJson` and `toJson`.
- Produces: the command
  `metrics [--games N=20] [--turn-limit T=150] [--map M=MedMap] [--seed S=1000] [--out f.json] [--compare base.json]`,
  and `static String MetricsTable.render(BehaviorReport current, BehaviorReport baselineOrNull)`
  (a new small class in `testing`).
  - With a baseline, `render` flags a regression with `!` when |Δ| > 2·stdErr **and** the change
    is in the bad direction.
  - Bad directions: up is bad for `*.oscillation`, `*.retarget`, `*.idle`, `emptySailing` and
    `fuelLosses`. Down is bad for `seaLoad`, `landings`, `overseasCaptures` and
    `navalAttacksPer100`.

**Behavior:**
- Game *i* calls `RandomUtil.setSeed(S+i)` before constructing the runner.
- Games alternate sides by swapping the two (identical) configs.
- Wins are not tallied (same AI).
- Print the table and the mean turn ms.
- `--out` writes `JsonFormatter.format(report.toJson())`.
- `--compare` reads the file with `JsonParser.parse`.

- [ ] **Step 1: Write the failing tests.** Test `MetricsTable.render` flags a LAND.oscillation
  increase beyond 2·stdErr with `!`, and doesn't flag a decrease.
- [ ] **Step 2:** Run it and confirm it fails.
- [ ] **Step 3:** Implement `setSeed`, `MetricsTable` and `handleMetrics`, and extend
  `printUsage`.
- [ ] **Step 4:** Run the tests and a manual `metrics --games 2 --turn-limit 10` and confirm both
  work:
  `mvn -q compile exec:java -Dexec.mainClass=com.developingstorm.games.sad.testing.AITestCLI -Dexec.args="metrics --games 2 --turn-limit 10"`
- [ ] **Step 5:** Commit with `feat(ai-metrics): seeded metrics command with baseline compare`.

### Task 4: Record baselines

**Files:**
- Create: `ai-configs/baselines/2026-09-baseline-MedMap.json`
- Create: `ai-configs/baselines/2026-09-baseline-BorderLand.json`

- [ ] **Step 1:** Run
  `metrics --games 20 --turn-limit 150 --seed 1000 --map MedMap --out ai-configs/baselines/2026-09-baseline-MedMap.json`.
  Do the same for BorderLand. If 20 games × 150 turns takes longer than ~20 minutes per map,
  reduce to `--games 10`, record that in the commit message, and use the same N later.
- [ ] **Step 2:** Commit the JSON and the printed tables (in the commit body) with
  `chore(ai-metrics): baseline behavior metrics for current AI`.

---

## Part 1: One memory + commitments

### Task 5: Single persistent StrategyMemory

**Files:**
- Modify: `brain/Battleplan.java:66-100`: add a `StrategyMemory memory` constructor parameter,
  and replace `this.strategyMemory = new StrategyMemory();` with
  `this.strategyMemory = memory;`.
- Modify: `brain/RobotBrain.java:40`: pass `this.strategyMemory`.
- Test: `src/test/java/com/developingstorm/games/sad/brain/RobotBrainMemoryTest.java`

**Interfaces:**
- Produces: `Battleplan(Game, Robot, AIConfiguration, StrategyMemory)`, and
  `RobotBrain.getBattleplan()` (package-private, for tests).

- [ ] **Step 1: Write the failing test.**
  - Build a game with two Robots on MedMap, following the `FleetHarness` pattern.
  - Call `brain.startNewTurn()` twice.
  - Assert `getBattleplan().getStrategyMemory()` is the same instance both times.
  - Also assert it's the memory the coordinator uses. Expose `OperationsCoordinator.getMemory()`
    as package-private.
- [ ] **Step 2:** Run it and confirm it fails.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the test and the full suite and confirm they pass.
- [ ] **Step 5:** Commit with `fix(ai): one StrategyMemory shared by battleplan and coordinator`.

### Task 6: Commitment model and rules

**Files:**
- Create: `brain/commit/CommitmentKind.java`, an enum with priority:
  `OPERATION(5), DEFEND(4), CAPTURE_CITY(3), STAGE(2), EXPLORE(1)`, plus
  `boolean outranks(CommitmentKind o)`.
- Create: `brain/commit/Commitment.java` with fields `unit` (Unit), `kind`, `target` (Location,
  nullable), `startTurn`, `lastProgressTurn`, `bestDistance` (MAX_VALUE initially) and
  `quietTurns`.
- Create: `brain/commit/CommitmentStatus.java` with the values `ACTIVE`, `COMPLETE` and `INVALID`.
- Create: `brain/commit/CommitmentContext.java`, an interface with:
  - `int currentTurn()`
  - `boolean ownedByMe(Location city)`
  - `int pathDistance(Unit u, Location target)`, which returns -1 if there's no path
  - `boolean isLoadingPoint(Location l)`
  - `boolean isUnloadingPoint(Location l)`
  - `boolean enemyWithin(Location l, int dist)`
  - `boolean isCity(Location l)`
  - `boolean assignedToOperation(Unit u)`
- Create: `brain/commit/CommitmentRules.java` with `CommitmentStatus status(Commitment c, CommitmentContext ctx)`.
  It mutates the progress fields.
- Create: `brain/commit/CommitmentBook.java` with `commit(Unit, CommitmentKind, Location, int turn)`,
  `get(Unit)`, `release(Unit, String reason)`, `pruneDead()` and `size()`.
- Modify: `brain/StrategyMemory.java`:
  - Add a `CommitmentBook commitments` field with a `getCommitments()` accessor.
  - Call `commitments.pruneDead()` in `startNewTurn()`.
- Test: `src/test/java/com/developingstorm/games/sad/brain/commit/CommitmentRulesTest.java`

**Rules** (`status` checks in this order):
- **OPERATION**: ACTIVE if `assignedToOperation(unit)`, else COMPLETE.
- **DEFEND**: INVALID if `!ownedByMe(target)`.
  - If `enemyWithin(target,3)`, set `quietTurns=0` and return ACTIVE.
  - Otherwise `quietTurns++`, and if `quietTurns>=2` return COMPLETE.
  - Otherwise ACTIVE.
- **CAPTURE_CITY** (land unit): COMPLETE if `ownedByMe(target)`.
  - `d = pathDistance(unit,target)`. If `d<0`, INVALID.
  - If `d < bestDistance`, set `bestDistance=d` and `lastProgressTurn=currentTurn`.
  - If `currentTurn - lastProgressTurn >= 3`, INVALID; else ACTIVE.
- **CAPTURE_CITY** (transport, where `unit.isTransport()`): the target is the unloading hex.
  COMPLETE if `!unit.hasCargo()`.
  - INVALID if `!isUnloadingPoint(target)`.
  - Then run the same progress check as above using `pathDistance`. A transport at the target
    has d = 0, which counts as ACTIVE.
- **STAGE**: COMPLETE if `unit.isCarried()`. INVALID if `!isLoadingPoint(target)`. Else ACTIVE.
- **EXPLORE**: ACTIVE. (The "no reachable frontier" case is handled when the captain's explore()
  returns null. See Task 9.)

- [ ] **Step 1: Write failing tests** with a `FakeContext` class in the test:
  - `captureCompletesWhenOwned`
  - `captureTargetTakenByEnemyStaysActive`: not owned by me, path exists → ACTIVE
  - `noProgressExpires`: distance stays at 5 for turns 1..4 → INVALID at turn 4
  - `progressResetsExpiry`
  - `noPathInvalid`
  - `defendCompletesAfterTwoQuietTurns`
  - `defendInvalidWhenCityLost`
  - `stageCompletesWhenCarried`
  - `operationCompleteWhenUnassigned`
  - `transportCaptureCompletesWhenEmpty`
  - `kindPriorityOrder`: OPERATION outranks DEFEND outranks CAPTURE_CITY outranks STAGE outranks
    EXPLORE, and nothing outranks itself
  - `bookPrunesDeadUnits`

  Units come from `TestUnits.stubUnit` or `Unit.createForTest`. Set carried state with
  `transport.addCarried(u)`.
- [ ] **Step 2:** Run the tests and confirm they fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the tests and confirm they pass.
- [ ] **Step 5:** Commit with `feat(ai): commitment model and rules`.

### Task 7: Merge InfantryCaptain and ArmorCaptain into LandCaptain

**Files:**
- Create: `brain/LandCaptain.java`, which extends `UnitCaptain<Unit>` and has the body of
  `InfantryCaptain.plan`.
- Delete: `brain/InfantryCaptain.java` and `brain/ArmorCaptain.java`.
- Modify: `brain/General.java`: use one `land` field and route ARMOR_ID/INFANTRY_ID to
  `land.plan(u)`.
- Check: `grep -rn "InfantryCaptain\|ArmorCaptain" src` for other references.

- [ ] **Step 1:** Make the change. It's a pure refactor.
- [ ] **Step 2:** Run `mvn -q clean test` and confirm all tests pass.
- [ ] **Step 3:** Commit with `refactor(ai): merge identical infantry/armor captains into LandCaptain`.

### Task 8: Unit.createdTurn and the "just produced" fix

**Files:**
- Modify: `Unit.java`:
  - Add a `private volatile int createdTurn;` field.
  - Set it in the constructor that takes `Game`, using `game != null ? game.getTurn() : 0`.
  - Add `getCreatedTurn()` and `setCreatedTurn(int)`.
- Modify: `persistence/UnitSerializer.java`:
  - Write `json.put("createdTurn", unit.getCreatedTurn())`.
  - On read, `Integer ct = json.getInteger("createdTurn"); data.createdTurn = ct != null ? ct : 0;`
  - Add a `public int createdTurn;` field on the data class.
- Modify: `persistence/GameStateSerializer.java` near `:469`: add
  `unit.setCreatedTurn(unitData.createdTurn);`.
- Modify: `brain/LandCaptain.java`: `isInProducingCity(u)` becomes
  `u.getCreatedTurn() == plan.getGame().getTurn() && plan.getBoard().getCity(u.getLocation()) != null`.
- Test: `src/test/java/com/developingstorm/games/sad/persistence/UnitSerializerCreatedTurnTest.java`

- [ ] **Step 1: Write failing tests:** `roundTripsCreatedTurn` and `missingFieldDefaultsToZero`.
  Serialize a unit with `UnitSerializer`, remove the key from the `JsonObj`, and deserialize.
  Look at the existing persistence tests for the entry-point method names.
- [ ] **Step 2:** Run the tests and confirm they fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the tests and the full suite and confirm they pass.
- [ ] **Step 5:** Commit with `fix(ai): only freshly built units count as just produced`.

### Task 9: Captains commit, and RobotBrain keeps committed orders

**Files:**
- Create: `brain/commit/BattleplanCommitmentContext.java`, which implements `CommitmentContext`
  using `Battleplan`, `Game`, `OperationsCoordinator` and `ThreatMap`:
  - `pathDistance` uses `u.getPath(t)`: null means -1, otherwise `length()`. It returns 0 if the
    unit is already at the target.
  - `enemyWithin` uses `threatMap.getNearbyEnemies(l, d)` being non-empty.
  - `isUnloadingPoint` checks the expand or defense unloading sets.
- Modify: `brain/UnitCaptain.java`:
  - Add a `protected StrategyMemory memory() { return plan.getStrategyMemory(); }` helper.
  - Add
    `protected Order committed(Unit u, CommitmentKind k, Location target, Order o) { if (o != null) memory().getCommitments().commit(u, k, target, plan.getGame().getTurn()); return o; }`.
  - Add `public Order continueCommitment(Unit u, Commitment c)` with a default that returns null.
  - Add `public String preemption(Unit u, Commitment c)` with a default that returns null.
- Modify: `brain/LandCaptain.java`:
  - In `plan()`, wrap each branch:
    - the operation order → `committed(u, OPERATION, null, …)`
    - the defensive order → `committed(u, DEFEND, nearestOwnedCity(u), …)`, or no commitment if
      the unit owns no city
    - `occupyUnownedCity` and `moveTowardBestCity`'s direct path → `CAPTURE_CITY` with the city
      location
    - `moveTowardBestCity`'s loading-point branch and `goToLoadingPoint` → `STAGE` with the
      chosen loading point
    - `explore` → `EXPLORE`

    `occupyLandStrategy`/`moveTowardBestCity` live in `UnitCaptain`. Give them an overload or a
    returned record so `LandCaptain` knows which target was chosen. Suggested approach:
    `UnitCaptain.lastTargetCity` / `lastLoadingPoint` fields, set by those helpers and read right
    after the call. This keeps the helper signatures unchanged for other captains.
  - `continueCommitment`:
    - CAPTURE_CITY → `u.newMoveOrder(c.target)`
    - STAGE → `u.getLocation().equals(c.target) ? u.newSentryOrder() : u.newMoveOrder(c.target)`
    - EXPLORE → `explore(u)`, and if that returns null, release the commitment and return null
    - DEFEND → `executeDefensiveStrategy(u)`, falling back to `patrol(u)`
    - OPERATION → `coordinator.getOperationOrder(u)`
  - `preemption`:
    - "operation assignment" if `coordinator.isAssigned(u)` and `c.kind != OPERATION`
    - "defense" if `DEFEND.outranks(c.kind)`, `!u.isCarried()`, `shouldDefend(u)` and the
      closest enemy is within 3
    - "city opportunity" if `c.kind` is EXPLORE or STAGE and `plan.getBestCityTarget(u)` is a
      city on `u.getContinent()` with a path
    - otherwise null
- Modify: `brain/TransportCaptain.java`:
  - In `plan()`, when it picks `goToExpansionUnloadingPoint`, commit
    `CAPTURE_CITY(unloadingPoint)`. Operation orders commit OPERATION.
  - `continueCommitment`:
    - OPERATION → the operation order
    - CAPTURE_CITY → if `u.hasCargo() && atUnloadPoint(u)`, `unload(u)`; else
      `u.newMoveOrder(c.target)`
  - `preemption`: "operation assignment" as for land.
- Modify: `brain/General.java`: add `UnitCaptain<?> captainFor(Unit u)`. It returns `land` for
  infantry/armor, `transport` for transports, and null otherwise.
- Modify: `brain/RobotBrain.java`:

```java
Order orderFor(Unit u) {
    CommitmentBook book = strategyMemory.getCommitments();
    Commitment c = book.get(u);
    UnitCaptain captain = general.captainFor(u);
    if (c != null && captain != null) {
        CommitmentStatus s = rules.status(c, context);
        String pre = (s == CommitmentStatus.ACTIVE) ? captain.preemption(u, c) : null;
        if (s == CommitmentStatus.ACTIVE && pre == null) {
            Order cur = u.getOrder();
            if (cur != null && serves(cur, c)) return cur;
            Order next = captain.continueCommitment(u, c);
            if (next != null) return next;
            book.release(u, "no order to continue");
        } else {
            book.release(u, pre != null ? "pre-empted by " + pre : s.toString());
        }
    }
    return general.getOrders(u);
}

static boolean serves(Order o, Commitment c) {
    switch (c.kind) {
        case CAPTURE_CITY: case STAGE:
            return o instanceof Move && c.target.equals(((Move) o).getDestination());
        case EXPLORE:
            return o.getType() == OrderType.EXPLORE;
        default:
            return false;
    }
}
```

  - In `startNewTurn()`, replace the reassign loop with
    `Order o = orderFor(u); if (o != u.getOrder()) u.assignOrder(o);`.
  - `getOrders(u)` returns `orderFor(u)`.
  - Build `rules = new CommitmentRules()` once, and build `context` per turn from the new
    battleplan.
- Test: `src/test/java/com/developingstorm/games/sad/brain/RobotBrainCommitmentTest.java`

- [ ] **Step 1: Write failing tests** on a MedMap game with two Robots (FleetHarness-style
  setup). Place an infantry for robot A on land far from any unowned city.
  - `activeCaptureKeepsOrderAcrossTurns`: commit CAPTURE_CITY to an unowned city with a path and
    assign `u.newMoveOrder(city)`. Call `startNewTurn()` and assert `u.getOrder()` is the **same
    instance**.
  - `completedCommitmentIsReplanned`: commit CAPTURE_CITY to a city, set its owner to A, call
    `startNewTurn()`, and assert the commitment is not CAPTURE_CITY of that city (released or a
    new one).
  - `exploreUpgradesToCityOpportunity`: commit EXPLORE with a nearby unowned city on the same
    continent. After `startNewTurn()`, assert the commitment kind is CAPTURE_CITY.
  - `navyUnitsHaveNoCommitment`: after `startNewTurn()`, a destroyer has no commitment.
  - `landUnitGetsCommitment`: after `startNewTurn()`, a fresh infantry has a commitment (any
    kind) or a skip-turn order.

  The tests need `RobotBrain`'s memory: expose package-private `getStrategyMemory()`.
- [ ] **Step 2:** Run the tests and confirm they fail.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the tests and the full suite. Also run
  `metrics --games 2 --turn-limit 30` as a crash check.
- [ ] **Step 5:** Commit with `feat(ai): land units and transports keep commitments across turns`.

### Task 10: Deterministic defend choice + fair city targets

**Files:**
- Modify: `brain/General.java:160`: `shouldDefend()` returns
  `!plan.getThreatMap().getThreatenedCities().isEmpty()`. Remove the `defendVsExplore` field if
  it becomes unused.
- Modify: `brain/TargetPrioritizer.java:61`: skip `!player.isExplored(city.getLocation())`, and
  remove the enemy-production penalty from `scoreCityTarget` and `getScoreReason`.
- Check: `grep -rn "RandomUtil" src/main/java/com/developingstorm/games/sad/brain` should show
  only `UnitCaptain.attackShipStrategy`, which is navy and out of scope.
- Test: `src/test/java/com/developingstorm/games/sad/brain/TargetPrioritizerFairnessTest.java`

- [ ] **Step 1: Write the failing test.** On a fresh MedMap game, robot A has explored only
  around its start. `prioritizeCities()` must return only cities with `A.isExplored(loc)`. Also
  assert that at least one board city is unexplored, so the test is meaningful.
- [ ] **Step 2:** Run it and confirm it fails.
- [ ] **Step 3:** Implement.
- [ ] **Step 4:** Run the test and the full suite and confirm they pass.
- [ ] **Step 5:** Commit with `fix(ai): deterministic defend choice; target only explored cities`.

### Task 11: Measure against baseline

**Files:**
- Create: `docs/superpowers/specs/2026-09-30-robot-ai-commitments-results.md`

- [ ] **Step 1:** Run `metrics` with the same N, turn limit and seed as Task 4, and
  `--compare` each map's baseline.
- [ ] **Step 2:** Check each success criterion from the spec:
  - LAND oscillation and retarget down at least 50%
  - LAND idle up at most 10% relative
  - landings and overseas captures down at most 10%
  - SEA/AIR within ±2 standard errors
  - mean turn ms up at most 25%
- [ ] **Step 3:** Write a results doc with both tables and a pass/fail per criterion.
- [ ] **Step 4:** If a criterion fails, investigate using `game_test.log` commitment release
  reasons, fix within this plan's scope, and re-measure. Record what was changed. If it can't be
  fixed within scope, record it as a finding for sub-project 2.
- [ ] **Step 5:** Commit with `docs(ai): commitments results vs baseline`.
