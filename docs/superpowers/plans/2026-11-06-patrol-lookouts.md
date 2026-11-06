# Patrol Lookouts and Reports Panel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Human patrols stop and report the first time they see an unreported enemy, or when their route is blocked, and can be resumed with one action. The right-hand panel becomes a "Reports" log of battles and patrol events, drawn as facing cards.

**Architecture:** A unit can hold a suspended `Patrol` alongside its current order.
- `Player` keeps the set of enemy unit ids it has been told about.
- `PatrolLookout` decides whether a patrolling unit sees an unreported enemy.
- `Move` gains two opt-in switches for patrol moves: an interrupt check after each step, and "treat an enemy in the way as blocked".
- `Patrol` uses them. It suspends itself and publishes a `PatrolReportEvent` instead of cancelling.
- The UI side:
  - a `ResumePatrolCommand`, the `R` key and a menu item;
  - a JavaFX `ReportsPanel`, built on a JavaFX-free `ReportEntry`/`ReportText` model.

**Tech Stack:** Java 24, Maven, JUnit 5, JavaFX.

**Spec:** `docs/superpowers/specs/2026-11-06-patrol-lookouts-design.md`

## Global Constraints

- Only human-owned units (`!owner.isRobot()`) act as lookouts or publish patrol reports. Robot behaviour is unchanged.
- A patrol never starts combat: an enemy on the next step or at the waypoint counts as a blocked route.
- Skip turn keeps a suspended patrol. Every other order discards it.
- An enemy counts as reported until it leaves the player's current sight. When it reappears, it is unreported again.
- Seeing an enemy follows the vision rules: within `Type.getVisionDistance()` of the patroller, and `enemy.isVisible(patrollerType.getVision())`. Submarines are only visible to WATER or COMPLETE vision.
- Panel title "Reports". Filter chips: "All", "Battles", "Sightings".
  - Badges: VICTORY, LOST, SPOTTED, BLOCKED.
  - Card edge colours: green victory, red loss, amber for sighting and blocked.
  - Names are cut off with "…" and the full name shows in a tooltip. A unit with no name shows its type.
  - Cap: 50 cards across both kinds.
- Key `R` resumes the patrol.
- Tests log to `target/test-run.log`. Never touch `game_test.log`.

## Review Focus

1. **The patroller dies or gets loaded before the player looks.**
   - Expected: its card's Resume button does nothing harmful.
   - Test: `ResumePatrolCommandTest.deadUnitIsIgnored` (Task 4).
2. **An already-reported enemy sits on the patrol's route after Resume.**
   - Expected: a blocked report, no fight.
   - Test: `PatrolLookoutBehaviourTest.reportedEnemyOnRouteBlocksWithoutCombat` (Task 3).
3. **A save holds a woken lookout.**
   - Expected: after load, R still resumes the same route.
   - Test: `SuspendedPatrolTest.survivesSaveAndLoad` (Task 1).
4. **A city air patrol and a woken plane in that city.**
   - Expected: the edict leaves the plane waiting.
   - Test: `SuspendedPatrolTest.cityAirPatrolSkipsSuspended` (Task 1).
5. **Two patrols see the same new enemy in one turn.**
   - Expected: only the first wakes; the second carries on.
   - Test: `PatrolLookoutBehaviourTest.secondPatrolDoesNotWakeForSameEnemy` (Task 3).

---

### Task 1: Suspended patrol on Unit, discard rules, persistence, edicts

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/Unit.java` (around `assignOrder`, line 934)
- Modify: `src/main/java/com/developingstorm/games/sad/persistence/UnitSerializer.java` (patrol serialization, around line 125)
- Modify: `src/main/java/com/developingstorm/games/sad/persistence/GameStateSerializer.java` (patrol restore, around line 807)
- Modify: `src/main/java/com/developingstorm/games/sad/edicts/CityAirPatrol.java:89`
- Modify: `src/main/java/com/developingstorm/games/sad/edicts/AirPatrol.java:29`
- Test: `src/test/java/com/developingstorm/games/sad/SuspendedPatrolTest.java`

**Interfaces:**
- Produces, on `Unit`:
  - `void suspendPatrol(Patrol p)`: clears the current order and remembers `p`.
  - `Patrol getSuspendedPatrol()`
  - `boolean resumePatrol()`: assigns the suspended patrol as the current order, clears the slot, and returns false if there was none.
  - `assignOrder(Order o)` now discards the suspended patrol unless `o.getType() == OrderType.SKIPTURN` or `o` is the suspended patrol itself.
- Produces, on `Patrol`: `boolean isReverseDirection()` (exists). Add a setter `void restoreState(int index, boolean reverse)` for loading.
- Persistence:
  - JSON key `"suspendedPatrol"` on the unit, with the same fields as a patrol order (waypoints, mode, `currentWaypointIndex`, plus `reverseDirection`).
  - Also add `reverseDirection` to the normal patrol-order JSON.
  - Old saves without these keys still load.

- [ ] **Step 1: Write the failing tests in `SuspendedPatrolTest`** (MedMap game, seed fixed with `RandomUtil.setSeed`, human player plus robot):
  - `resumeRestoresRouteAndProgress`:
    - Build a LINEAR patrol with 3 waypoints and set it to index 2, reverse true.
    - `suspendPatrol`, then `resumePatrol()` returns true.
    - `getOrder()` is the same `Patrol` instance with index 2 and reverse true.
    - `getSuspendedPatrol()` is null.
  - `skipTurnKeepsSuspended`: after a suspend, `assignOrder(newSkipTurn())` leaves `getSuspendedPatrol()` non-null.
  - `otherOrdersDiscard`: a Sentry, a Move and a new Patrol each make `getSuspendedPatrol()` null.
  - `resumeWithNothingIsFalse`: `resumePatrol()` returns false and the order is unchanged.
  - `survivesSaveAndLoad`:
    - Suspend a patrol on a human fighter and `saveGameToFile`.
    - `loadGame` with `MockSaDBoardContext`.
    - The loaded unit (same id) has a suspended patrol with equal waypoints, mode, index and reverse.
  - `cityAirPatrolSkipsSuspended`:
    - Put a `CityAirPatrol` edict on a human city, plus a human fighter in that city with a suspended patrol and no order.
    - Run the edict (`edict.execute(game)` or via `City.startNewTurn` as the edict is invoked).
    - The fighter still has no order and keeps its suspended patrol.
- [ ] **Step 2:** Run `mvn -q test -Dtest=SuspendedPatrolTest`. Expected: compile failure, then failures.
- [ ] **Step 3:** Implement the `Unit` methods and the `assignOrder` rule, `Patrol.restoreState`, serializer read/write, and the edict skip (`u.getSuspendedPatrol() != null` → `continue`).
- [ ] **Step 4:** Run `mvn -q test`. Expected: all pass.
- [ ] **Step 5:** Commit with `feat(patrol): units keep a suspended patrol that can be resumed`.

### Task 2: Reported enemies and lookout detection

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/Player.java`
  - Reported set.
  - Pruning in `startNewTurn` (line 1067), after visibility is rebuilt.
- Create: `src/main/java/com/developingstorm/games/sad/PatrolLookout.java`
- Test: `src/test/java/com/developingstorm/games/sad/PatrolLookoutTest.java`

**Interfaces:**
- Produces, on `Player`:
  - `boolean isReported(Unit enemy)`
  - `void markReported(Unit enemy)`
  - `void forgetUnseenReports()`: drops ids whose unit is dead, or whose hex the player can't currently see that unit on (`!enemy.isVisible(getVisibility(enemy.getLocation()))`). It is called at the end of `startNewTurn`.
- Produces: `static Unit PatrolLookout.unreportedEnemyInSight(Game game, Unit patroller)`.
  - It returns the nearest enemy (owner ≠ patroller's owner) within `getType().getVisionDistance()` that the patroller can see and its owner hasn't reported.
  - It returns null otherwise.
  - Candidates come from `game.units()`.

- [ ] **Step 1: Write the failing tests in `PatrolLookoutTest`** (MedMap, seeded):
  - `seesEnemyInRange`: a human destroyer, and a robot transport at distance ≤ the destroyer's vision distance → it returns the transport.
  - `ignoresOutOfRange`: the transport at vision distance + 1 → null.
  - `submarineHiddenFromSurfaceVision`: a human transport (SURFACE vision) and a robot submarine at distance 1 → null. A human destroyer (COMPLETE) in the same place → the sub.
  - `reportedIsIgnored`: `markReported(transport)` → null.
  - `forgottenWhenOutOfSight`:
    - `markReported(transport)`, then move the transport (`Unit.move` or `changeLoc` via a move) far from every human unit and city.
    - Call `human.startNewTurn()`; `isReported(transport)` is false.
  - `stillReportedWhileVisible`: same as above, but the transport stays in sight → it stays reported.
- [ ] **Step 2:** Run `mvn -q test -Dtest=PatrolLookoutTest`. Expected: compile failure.
- [ ] **Step 3:** Implement as specified.
- [ ] **Step 4:** Run `mvn -q test`. Expected: all pass.
- [ ] **Step 5:** Commit with `feat(patrol): track reported enemies and detect unreported ones in sight`.

### Task 3: Patrol suspends and reports instead of cancelling

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/ResponseCode.java`: add `INTERRUPTED("Interrupted")`.
- Modify: `src/main/java/com/developingstorm/games/sad/orders/Move.java`. Two opt-ins, both off by default:
  - `void setInterrupt(java.util.function.BooleanSupplier check)`: after each successful step with moves left, if `check` returns true, return `INTERRUPTED`.
  - `void setStopAtEnemies(boolean)`: before resolving a step whose hex holds any unit not owned by the mover (visible or not), or a non-own city that isn't the mover's, return `BLOCKED` without moving.
- Modify: `src/main/java/com/developingstorm/games/sad/orders/Patrol.java` (`executeInternal`)
- Create: `src/main/java/com/developingstorm/games/sad/events/PatrolReportEvent.java`
- Modify: `src/main/java/com/developingstorm/games/sad/events/GameEventType.java`: add `PATROL_REPORT`.
- Test: `src/test/java/com/developingstorm/games/sad/PatrolLookoutBehaviourTest.java`

**Interfaces:**
- Consumes: Task 1's `suspendPatrol`, and Task 2's `PatrolLookout.unreportedEnemyInSight` and `Player.markReported`.
- Produces:
  - `PatrolReportEvent`:
    - `enum Kind { SIGHTING, BLOCKED }`
    - `getKind()`, `getPatroller()`, `getEnemy()` (null for BLOCKED unless an enemy is the blocker), `getLocation()` and `getTurn()`.
  - `Patrol` behaviour for human owners:
    - **Sighting.** Check at the start of `executeInternal`, and via `Move.setInterrupt` after each step. If `unreportedEnemyInSight` returns an enemy:
      - `markReported(enemy)`;
      - `unit.suspendPatrol(this)`;
      - publish SIGHTING, with location = the enemy's hex;
      - return `ORDER_COMPLETE`.
    - **Blocked.** The inner move returns `BLOCKED` (enemy in the way) or `CANCEL_ORDER` (no path or refused):
      - `suspendPatrol(this)`;
      - publish BLOCKED, with location = the waypoint and enemy = the blocking unit, if any;
      - return `ORDER_COMPLETE`.
    - **Waiting.** `YIELD_PASS` keeps today's wait-and-retry.
  - Robot owners: no interrupt, no stop-at-enemies and no events, so behaviour is identical to today.

- [ ] **Step 1: Write the failing tests in `PatrolLookoutBehaviourTest`.** Use MedMap and a seeded game. Set up a human destroyer with a LOOP patrol over open water and call `beginTurn()` first. Listen for `PATROL_REPORT`.
  - `wakesOnUnreportedEnemy`:
    - Place a robot transport so it comes into vision after one step.
    - `attemptTurn()`.
    - The destroyer has no order, `getSuspendedPatrol()` is the patrol, and one SIGHTING event names both units with the transport's hex.
    - `isReported(transport)` is true.
  - `doesNotWakeTwiceForSameEnemy`: after the above, `resumePatrol()` then `attemptTurn()` with the transport still in sight → no new event and the patrol is still the current order.
  - `wakesAgainAfterEnemyReturns`:
    - The transport leaves sight, then `human.startNewTurn()`.
    - The transport comes back, then `attemptTurn()` → a second SIGHTING.
  - `reportedEnemyOnRouteBlocksWithoutCombat`:
    - Mark a robot unit as reported and park it on the next waypoint.
    - `attemptTurn()` → a BLOCKED event, the patrol is suspended, both units are alive with full hits, and no `COMBAT_RESOLVED` was published.
  - `noPathBlocksInsteadOfCancelling`:
    - Make the waypoint unreachable (surround it with land via `makeLand`, or use a waypoint on land for a ship).
    - `attemptTurn()` → BLOCKED and a suspended patrol, not a cleared order without a suspension.
  - `friendlyInTheWayStillWaits`: a human unit on the next step → no event, and the patrol is still the current order.
  - `secondPatrolDoesNotWakeForSameEnemy`: two human patrolling destroyers both able to see a new enemy → after both `attemptTurn()`, exactly one SIGHTING, and the second destroyer still has its patrol as the current order.
  - `robotPatrolsAreUnchanged`: a robot destroyer on a patrol with a human unit in sight → no event and no suspension.
- [ ] **Step 2:** Run `mvn -q test -Dtest=PatrolLookoutBehaviourTest`. Expected: failures.
- [ ] **Step 3:** Implement the `Move` opt-ins, `ResponseCode.INTERRUPTED`, the event and the `Patrol` changes.
- [ ] **Step 4:** Run `mvn -q test`. Expected: all pass, including existing patrol and move tests.
- [ ] **Step 5:** Commit with `feat(patrol): patrols wake and report on new enemies or a blocked route`.

### Task 4: Resume patrol command, R key and menu

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/commands/ResumePatrolCommand.java`
- Modify: `src/main/java/com/developingstorm/games/sad/controller/GameController.java` and `GameControllerImpl.java`: add `void resumePatrol(Unit unit)`.
- Modify: `src/main/java/com/developingstorm/games/sad/fx/GameView.java`: add a `case R:` beside the other order keys.
- Modify: `src/main/java/com/developingstorm/games/sad/fx/MapCanvas.java` (`createUnitOrdersMenu`): add a "Resume patrol" item, shown only when `unit.getSuspendedPatrol() != null`.
- Test: `src/test/java/com/developingstorm/games/sad/commands/ResumePatrolCommandTest.java`

**Interfaces:**
- Consumes: Task 1's `Unit.resumePatrol()`.
- Produces:
  - `ResumePatrolCommand(Unit unit)`. `execute(Game)` calls `unit.resumePatrol()` when the unit is alive, owned by a human and has a suspended patrol, and then pushes the unit to pending play.
  - `GameController.resumePatrol(Unit)` submits the command and then resumes the game, like `issueOrder` followed by `resumeGame`.

- [ ] **Step 1: Write the failing tests:**
  - `resumesSuspendedPatrol`: after execute, the order is the patrol.
  - `noSuspendedPatrolIsNoOp`: the order is unchanged and nothing throws.
  - `deadUnitIsIgnored`: kill the unit first; execute does not throw and does nothing.
- [ ] **Step 2:** Run `mvn -q test -Dtest=ResumePatrolCommandTest`. Expected: compile failure.
- [ ] **Step 3:** Implement the command, the controller method, the R key (on the selected unit) and the menu item.
- [ ] **Step 4:** Run `mvn -q clean compile` then `mvn -q test`. Expected: all pass.
- [ ] **Step 5:** Commit with `feat(patrol): resume a suspended patrol with R or the unit menu`.

### Task 5: Reports panel

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/fx/ReportEntry.java` (JavaFX-free model)
- Create: `src/main/java/com/developingstorm/games/sad/fx/ReportText.java` (JavaFX-free wording)
- Create: `src/main/java/com/developingstorm/games/sad/fx/ReportsPanel.java`. It replaces `BattleHistoryPanel`; delete the old class.
- Modify: `src/main/java/com/developingstorm/games/sad/fx/GameView.java`
  - Build `ReportsPanel` in place of the battle panel.
  - On `COMBAT_RESOLVED`, add `ReportEntry.battle(result, game.getTurn(), human)`.
  - On `PATROL_REPORT`, add `ReportEntry.patrol(event)`.
  - Keep the click-to-centre listener.
  - Wire the Resume button to `controller.resumePatrol(unit)`.
- Test: `src/test/java/com/developingstorm/games/sad/fx/ReportTextTest.java`

**Interfaces:**
- Consumes: `CombatResult` getters, Task 3's `PatrolReportEvent`, and Task 4's `GameController.resumePatrol`.
- Produces:
  - `ReportEntry`. Fields:
    - `kind` (BATTLE_WON, BATTLE_LOST, SIGHTING, BLOCKED);
    - `turn` and `location`;
    - left and right unit info (name or null, `Type`, owner `Player`, hit fraction or null);
    - `patroller` (Unit, or null for battles).
  - `ReportText`:
    - `static String header(ReportEntry e, Player human)`;
    - `static String badge(ReportEntry e)`;
    - `static String verb(ReportEntry e)`;
    - `static String displayName(String name, Type type)`.

**Card rules:**
- **Battle, left = attacker, right = defender.**
  - The kind is BATTLE_WON when the human's side won, otherwise BATTLE_LOST.
  - The header is `"Turn N · You attacked"` or `"Turn N · Enemy attacked"`, by attacker owner.
- **Verb.**
  - When the attacker won, it depends on the defender's travel: `"sank"` for SEA, `"shot down"` for AIR, `"destroyed"` for LAND.
  - When the attacker lost: `"beaten by"`.
- **Sighting.**
  - Left = patroller, right = enemy. Verb `"spotted"`.
  - Header `"Turn N · Patrol sighting · (x,y)"`.
  - Badge SPOTTED.
- **Blocked.**
  - Left = patroller, right = blocking enemy, or empty.
  - Verb `"blocked"`.
  - Header `"Turn N · Route blocked · (x,y)"`.
  - Badge BLOCKED.
- **Badges for battles:** VICTORY or LOST.
- **`displayName`:** the name if it isn't null or blank, otherwise the type's display name.

- [ ] **Step 1: Write the failing tests in `ReportTextTest`.** Build `ReportEntry` values directly:
  - The header, badge and verb for each kind as listed above, including all three "attacker won" verbs and "beaten by".
  - `displayName(null, Type.DESTROYER)` returns the type name; `displayName("Valiant", …)` returns `"Valiant"`.
  - `humanDefenderWinning`: the enemy attacks and the human defender wins → BATTLE_WON, VICTORY, `"Turn N · Enemy attacked"`.
- [ ] **Step 2:** Run `mvn -q test -Dtest=ReportTextTest`. Expected: compile failure.
- [ ] **Step 3:** Implement `ReportEntry` and `ReportText`. Then build `ReportsPanel` to the spec's facing layout. Each card has:
  - the header row with the badge;
  - two unit columns (icon on owner colour, name label with ellipsis and a `Tooltip` holding the full name, then the type in grey for sightings or a health bar for battles);
  - the verb in the middle;
  - a coloured left border;
  - an amber tint for SIGHTING and BLOCKED cards;
  - a "Resume patrol" button on SIGHTING and BLOCKED cards, enabled only while the patroller is alive and `getSuspendedPatrol() != null`.

  The panel also has the chips All / Battles / Sightings (Sightings shows SIGHTING and BLOCKED), newest card first, the 50-card cap, and an empty state reading "No reports yet".
- [ ] **Step 4:** Run `mvn -q clean compile` and `mvn -q test`. Expected: all pass. Then check by hand in the app if possible:
  - a battle card;
  - a patrol wake-up and its card;
  - the filters;
  - click to centre;
  - the Resume button and R.

  If the app can't be driven from here, say so for Pete.
- [ ] **Step 5:** Commit with `feat(ui): Reports panel with facing cards for battles and patrol sightings`.
