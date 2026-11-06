# Patrol Lookouts and the Reports Panel

## Intent

**Why (Pete):** patrols don't work well. They get cancelled too easily when the unit spots
something, and once cancelled the route is lost. The point of a patrol is to spot enemy units
and bring them to the player's attention, but the game has no good way to surface new
findings.

**What success looks like:**
- A patrol runs until it sees something new.
- It then stops, tells you what it saw, and waits for you.
- One action puts it back on its route.
- The right-hand panel becomes a lively log of fights and sightings, not a dull grey list.

## Goals

### Patrols are lookouts (human player)

- **Wake on a new enemy.** A patrolling unit that sees an enemy unit not yet reported stops,
  wakes and waits for orders. Its patrol is suspended, not discarded.
- **Only unreported enemies wake it** (Pete). An enemy already reported doesn't wake any
  patrol again while it stays in the player's sight.
  - If that enemy drops out of sight and later reappears, it counts as unreported again (Pete).
- **Resume patrol** puts the unit back on its suspended route: same waypoints, same mode, the
  waypoint it was heading for, and the same direction for back-and-forth patrols.
  - It's available as a context-menu item on the unit, the `R` key, and a button on the
    sighting's report card.
  - It only appears or works when the unit has a suspended patrol.
- **A blocked route wakes the unit instead of dropping the patrol** (my call). Today, if the
  next waypoint can't be reached (no path, or the step is refused), the patrol is cancelled
  silently. Instead, the unit:
  - stops and wakes with its patrol suspended;
  - shows a "route blocked" report card.

  A friendly unit merely in the way keeps today's behaviour: wait and retry next turn.
- **An enemy on the route wakes the unit too; a patrol never fights its way through.** Today
  the pathfinder lets a path end on an enemy-occupied hex, and moving into an enemy starts
  combat. That can happen when the waypoint holds an enemy, when an enemy steps onto the
  planned path, or when an unseen submarine sits on it.
  - For a patrol's moves, a step or waypoint occupied by an enemy is treated as a blocked
    route: suspend, wake, and show the "Route blocked" card. This follows Pete's "wake up and
    let the player decide".
  - It matters more after Resume, because the enemy is already reported and won't
    re-trigger a sighting.
- **Skip turn keeps the suspended patrol** (my call). Space, or the menu's Skip, defers the
  decision, so Resume still works later. Ending the turn with the unit still waiting also
  keeps it.
- **Every other order discards the suspended patrol**, including Sentry, Move and a new
  Patrol (my call).
- **City air-patrol edicts** (`edicts/CityAirPatrol.java`, `AirPatrol.java`) hand out
  `Patrol` orders to idle fighters and bombers. These patrols are lookouts too (my call):
  they're how most air patrols get made, and sightings only fire for unreported enemies, so
  they won't flood the panel.
  - The edicts skip any unit that has a suspended patrol, so a woken plane keeps waiting for
    the player instead of being silently sent off again.

### The Reports panel (replaces "Battle History")

- **Title:** "Reports".
- **Filter chips:** All, Battles, Sightings.
- **One card per event, newest first.** Cards use the "facing" layout Pete chose from the
  mockup (`.superpowers/brainstorm/.../report-cards-names.html`, option "Facing"):
  - **Header:** turn number, event kind ("You attacked", "Enemy attacked", "Patrol sighting",
    "Route blocked"), the hex, and a badge (VICTORY / LOST / SPOTTED / BLOCKED).
  - **Body:** the two units side by side in columns. Each column shows the icon on its
    owner's colour, the unit's **name** (Pete: names, not types), and either the type in grey
    (sightings) or a health bar after the fight (battles). The action ("destroyed", "shot
    down", "spotted", …) sits in the middle.
  - **Long names** are cut off with "…" and never wrap. The full name shows in a tooltip.
  - Units without a name show their type instead.
  - **Coloured left edge:** green for victory, red for loss, amber for sighting and blocked.
    Sighting cards have a faint amber background.
- **Clicking a card** centres the map on the event's hex, as today.
- **Sighting and blocked cards** also carry a "Resume patrol" button. It works while that unit
  still has a suspended patrol.
- **Keep the existing 50-entry cap**, counted across both kinds.

## Non-goals

- **Non-patrolling units don't create sighting cards.** No change to how other units see or
  report enemies.
- **No change to robot behaviour or the AI.** The robot's units don't use the patrol order for
  lookouts (its captains issue their own moves); only the human player gets wake-ups and cards.
- **No sound or pop-up dialogs.**
- **No combat changes.** Patrols never attack on their own. An enemy in the way wakes the
  unit, as above.
- **Fleets and escorts** are a separate project (merging them, and showing linked ships on the
  leading ship's panel).

## Anti-goals

- **A patrol must not wake you every turn for the same enemy** sitting in view.
- **A patrol must not silently disappear.** You should always learn why it stopped.

## Constraints and existing system

**The patrol order (`orders/Patrol.java`):**
- `LOOP` and `LINEAR` modes, with `currentWaypointIndex` and `reverseDirection`, moving one
  inner `Move` toward each waypoint.
- Its enemy-detection code is commented out.
- It cancels on any inner-move result other than complete, turn complete, died or yield.
- Patrols are saved and loaded with their state (`GameStateSerializer` ~line 807).

**What "sees" means:**
- A unit sees within its type's vision distance (`Type.getVisionDistance()`) and vision kind.
  Submarines are visible only to COMPLETE or WATER vision; see `Unit.isVisible(Vision)` and
  the vision rules in CLAUDE.md.
- The player's sight, and contacts (current and ghost), come from `Player` / `ContactMap`.
- "Reported" is per player, keyed by enemy unit.
- An enemy stops counting as reported once it's no longer in the player's current sight, so
  a reappearance after it has gone counts as new.
- Note that visibility is recalculated at each turn start and refreshed mid-turn after moves
  (`Game.refreshAllContacts`).

**When to check:**
- After each step a patrolling unit takes, and at the start of its turn before it moves.
  An enemy that moved into view during the enemy's turn wakes the patrol at once instead of
  after another step.

**Events and UI:**
- The UI hears about events through `GameEventBus`.
- Battles already publish `CombatResolvedEvent` carrying a `CombatResult`, which has names,
  types, owners, hits and location.
- Sightings and blocks need a new event carrying: the patrolling unit, the enemy unit (for
  sightings), the hex and the turn.

**Panel:**
- Today's panel is `fx/BattleHistoryPanel.java`, built in `GameView`, listening to
  `COMBAT_RESOLVED`.
- Unit icons and player colours come from `TerrainImages` and the existing per-player
  colours.

**Keys and orders:**
- `R` is currently unused in `GameView`'s key handling.
- New orders and resumes go through `GameController` / `AssignOrderCommand` (game thread), as
  other orders do.

**Saving:**
- A suspended patrol is part of the unit's state and survives save and load (my call).
- Report cards and the set of reported enemies are not saved. After a load, enemies in view
  may be reported once more, which is acceptable (my call).

## Testing

**Game-side tests:**
- A patrolling unit with an unreported enemy coming into its vision wakes, has no current
  order, has a suspended patrol, and a sighting event is published with the right units and
  hex.
- The same enemy still in view next turn does not wake the resumed patrol. Once the enemy has
  left the player's sight and comes back, it does.
- A submarine outside COMPLETE/WATER vision doesn't trigger a sighting.
- Resume restores waypoints, mode, waypoint index and direction.
- Skip turn keeps the suspended patrol; any other order discards it.
- An enemy on the next step or at the waypoint suspends the patrol with a blocked event, and
  no combat happens.
- A city air-patrol edict leaves a unit with a suspended patrol alone.
- A blocked route (no path to the waypoint) suspends and publishes a blocked event instead of
  cancelling.
- A friendly unit in the way still waits and retries.
- A suspended patrol survives save and load.
- Robot-owned units on patrol orders publish no sighting events (if robots can hold one).

**UI** (check by hand, plus unit tests where logic can be pulled out of JavaFX):
- Card text for each kind.
- Names cut off with a tooltip.
- Filters.
- Click centres the map.
- The Resume button works.

## Left to the builder

- Event class names and fields, and where "reported" bookkeeping lives (the `Player` sight
  data is the natural home).
- Exact colours and sizes beyond the mockup, and card height.
- Verb wording for each unit-type matchup (a small, plain table is fine: "destroyed", "sank",
  "shot down", "spotted").
