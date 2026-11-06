# Generated Maps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** New Game generates a fresh land/water map at a chosen size. It has one connected
ocean, varied continents, and cities spaced at least 3 apart with coastal and inland cities on
the continents that can hold both.

**Architecture:** A new `sad.mapgen` package holds four pieces:
- `MapSize`: the four sizes.
- `MapRegions`: flood-fill helpers over a `HexBoardMap`.
- `MapGenerator`: produces a `HexBoardMap`.
- `CityPlacer`: picks city locations on any map. It replaces `Board.sprinkleCities`.

`CityManager.assignCities` starts players on different continents. The JavaFX New Game flow
passes a `MapSize` instead of a map resource name.

**Tech Stack:** Java 24, Maven, JUnit 5, JavaFX.

**Spec:** `docs/superpowers/specs/2026-11-06-generated-maps-design.md`

## Global Constraints

- **Sizes:** Small 25×25, Medium 50×50 (default), Large 75×75, X-Large 100×100.
- **Map grid:** `HexBoardMap` ints; 0 is water and 1 is land. Build maps with
  `new HexBoardMap(w, h)`, which calls `LocationMap.init`.
- **Hex adjacency:** use `Location.getRing(1)` only, never dx/dy loops.
- **Randomness:** all through `RandomUtil`. Generation must never loop forever: retry up to
  a fixed limit, then accept.
- **Ocean:** water is one connected region, and no water region is enclosed by land.
- **Edges:** edge hexes are water. Keep this rule in one obvious place so it can be dropped
  later (Pete's reservation).
- **City spacing:** `Location.distance(a, b) >= 3` for every pair of cities, on any
  continent.
- **City count target:** `(width + height) / 2`. It may come in under target, but never by
  breaking spacing.
- **Hand-made maps** (`MedMap.sdm`, `BorderLand.sdm`, `SampleMap.sdm`) stay, used by
  `AITestCLI` and tests.
- **Logging:** tests log to `target/test-run.log`. Never write to `game_test.log`.

## Review Focus

1. **Small 25×25 with an unlucky seed:** a game must still start, with two players on
   different continents. Test: `MapGeneratorTest.everySmallMapStartsAGame` (Task 3).
2. **Generated map in a save:** save and load keep the exact land/water grid for every size.
   Test: `MapGeneratorTest.mapSurvivesSaveAndLoad` (Task 1).
3. **X-Large performance:** generating and constructing a `Game` stays fast. Test:
   `MapGeneratorTest.xLargeIsFast` (Task 1, generation under 1 s).
4. **Map with a single continent of coastal cities** (hand-made or fallback): the game still
   starts. Test: `StartAssignmentTest.singleContinentFallsBack` (Task 3).
5. **Hand-made MedMap after the placer change:** cities obey spacing and the game starts.
   Test: `CityPlacerTest.handMadeMapObeysSpacing` (Task 2).

---

### Task 1: MapSize, MapRegions and MapGenerator

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/mapgen/MapSize.java`
- Create: `src/main/java/com/developingstorm/games/sad/mapgen/MapRegions.java`
- Create: `src/main/java/com/developingstorm/games/sad/mapgen/MapGenerator.java`
- Test: `src/test/java/com/developingstorm/games/sad/mapgen/MapGeneratorTest.java`

**Interfaces:**
- Produces:
  - `enum MapSize { SMALL(25, "Small 25×25"), MEDIUM(50, "Medium 50×50"), LARGE(75, "Large 75×75"), XLARGE(100, "X-Large 100×100") }`
    with `int dimension()` and `toString()` returning the label.
  - `MapRegions`, all static, taking a `HexBoardMap`:
    - `List<Set<Location>> landRegions(HexBoardMap)` and `List<Set<Location>> waterRegions(HexBoardMap)`.
      Both use flood fill over `getRing(1)`, filtered to on-board hexes.
    - `boolean isEdge(HexBoardMap, Location)`.
    - `boolean isCoastal(HexBoardMap, Location)`: land with a water neighbour.
    - `double landShare(HexBoardMap)`.
  - `MapGenerator`:
    - `static HexBoardMap generate(MapSize size)`
    - `static final int MAX_ATTEMPTS = 20`
    - `static final double MIN_LAND = 0.30`, `MAX_LAND = 0.40`
    - Edge water rule: one private method `forceWaterEdges(HexBoardMap)`.

- [ ] **Step 1: Write the failing tests in `MapGeneratorTest`**

Run each property over seeds `1..30` for every `MapSize`, calling
`RandomUtil.setSeed(seed)` before each `generate`:
- `oceanIsOneConnectedRegion`: `MapRegions.waterRegions(map).size() == 1`. This covers both
  "connected" and "no enclosed water", since any lake would be a second region.
- `edgesAreWater`: every `l` with `MapRegions.isEdge(map, l)` is water.
- `landShareInRange`: `0.28 <= landShare(map) <= 0.42`. This leaves slack for lake filling.
- `atLeastTwoContinents`: `landRegions(map).size() >= 2`.
- `continentCountVaries`: for MEDIUM, across seeds 1..20, the set of distinct
  `landRegions(map).size()` values has at least 3 members.
- `sameSeedSameMap`: generate twice with seed 42 and assert identical `getData()` grids.
- `xLargeIsFast`: generating XLARGE with seed 7 takes under 1000 ms.
- `mapSurvivesSaveAndLoad`: for each size, `saveMap(StringWriter)`, then
  `HexBoardMap.loadMap(StringReader)`; assert equal grids and dimensions.

- [ ] **Step 2: Run** `mvn -q test -Dtest=MapGeneratorTest`. Expected: compile failure (the
  classes don't exist yet).

- [ ] **Step 3: Implement `MapSize`, `MapRegions` and `MapGenerator.generate`.**

Suggested approach (tuning is the builder's call):
1. Pick a land target, uniform in `[MIN_LAND, MAX_LAND]`.
2. Pick a seed count `k`, uniform in a size-scaled range. For example: SMALL 3–5,
   MEDIUM 4–9, LARGE 5–12, XLARGE 6–15.
3. Grow land from `k` random non-edge seeds by randomly expanding the frontier until the
   target is reached. Never grow into edge hexes.
4. Fill every water region except the one touching the edge with land.
5. Accept the map if `landRegions.size() >= 2` and the land share is within
   `[0.28, 0.42]`. Otherwise retry, up to `MAX_ATTEMPTS`, then return the last map.

- [ ] **Step 4: Run** `mvn -q test -Dtest=MapGeneratorTest`. Expected: all pass.

- [ ] **Step 5: Commit** with `feat(mapgen): generate land/water maps with one connected ocean`.

### Task 2: CityPlacer replaces sprinkleCities

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/mapgen/CityPlacer.java`
- Modify: `src/main/java/com/developingstorm/games/sad/Board.java`. In `init()` and
  `sprinkleCities`, place cities at `CityPlacer.place(map, (w + h) / 2)`, keeping the
  existing `new City(loc, game)`, `cities.add` and `ctoks.put`.
- Test: `src/test/java/com/developingstorm/games/sad/mapgen/CityPlacerTest.java`

**Interfaces:**
- Consumes: `MapRegions.landRegions`, `MapRegions.isCoastal`, `MapGenerator.generate`,
  `MapSize`.
- Produces:
  - `static List<Location> CityPlacer.place(HexBoardMap map, int target)`
  - `static final int MIN_SPACING = 3`
  - `static final int MIN_CONTINENT_FOR_CITY = 4` (hexes)

**Placement rules** (the implementer writes the body):
- Each continent with area `>= MIN_CONTINENT_FOR_CITY` gets a quota proportional to its area,
  with at least 1.
- A continent with any inland hex (land, not coastal) and a quota of at least 2 places one
  coastal and one inland city first, then fills the rest randomly.
- A candidate is valid only if it is at distance `>= MIN_SPACING` from every placed city.
- Unfillable quota is dropped; spacing is never relaxed.

- [ ] **Step 1: Write the failing tests in `CityPlacerTest`.** Use seeds 1..30 and every size
  on generated maps:
  - `citiesAreSpaced`: every pair has distance `>= 3`.
  - `continentsGetCoastalAndInland`: every continent with an inland hex and at least 2 cities
    has at least one coastal and at least one inland city.
  - `countNearTarget`: `place(map, t).size() >= 0.8 * t` for MEDIUM, LARGE and XLARGE. For
    SMALL, require `>= 0.5 * t`.
  - `handMadeMapObeysSpacing`: on `MedMap.sdm` (via `loadMapAsResource`), all pairs have
    distance `>= 3`.
  - `boardUsesPlacer`: build a `Game` on a generated MEDIUM map and assert every pair of
    `game.getBoard().getCities()` has distance `>= 3`.

- [ ] **Step 2: Run** `mvn -q test -Dtest=CityPlacerTest`. Expected: compile failure, and
  `boardUsesPlacer` fails on the old sprinkler.

- [ ] **Step 3: Implement `CityPlacer.place`, and switch `Board.sprinkleCities` to it.** Keep
  the "Map must have some land" check.

- [ ] **Step 4: Run** `mvn -q test`. Expected: the full suite passes. Seeded tests that
  assumed old city layouts may need new seeds; fix their assumptions, not the placer.

- [ ] **Step 5: Commit** with `feat(mapgen): place cities with hard spacing and coastal/inland mix`.

### Task 3: Players start on different continents

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/CityManager.java`
  (`assignCities`, `findUnownedCoastalCity`)
- Test: `src/test/java/com/developingstorm/games/sad/StartAssignmentTest.java`
- Test: add `everySmallMapStartsAGame` to `MapGeneratorTest`

**Interfaces:**
- Consumes: `MapGenerator.generate`, `City.getContinent()`, `City.isCoastal()`.
- Produces: the behaviour only; the signatures are unchanged.

- [ ] **Step 1: Write the failing tests.**
  - `StartAssignmentTest.playersStartOnDifferentContinents`: for seeds 1..30 on generated
    MEDIUM maps, build a 2-player `Game`. Assert both players' first cities are coastal and
    `getContinent()` differs.
  - `StartAssignmentTest.singleContinentFallsBack`: build a 25×25 `HexBoardMap` with one
    island (water edges, a solid land blob in the middle) and a 2-player `Game`. Assert
    construction succeeds and each player owns one coastal city.
  - `MapGeneratorTest.everySmallMapStartsAGame`: for seeds 1..50, SMALL, a 2-player `Game`
    constructs without exception. When at least 2 continents have coastal cities, the
    players' continents differ.

- [ ] **Step 2: Run them.** Expected: `playersStartOnDifferentContinents` fails on some seed.

- [ ] **Step 3: Change `assignCities`.** For each player, prefer an unowned coastal city on a
  continent no earlier player started on. Otherwise use any unowned coastal city, as today.
  Use `RandomUtil` for the choice among candidates.

- [ ] **Step 4: Run** `mvn -q test`. Expected: all pass.

- [ ] **Step 5: Commit** with `feat: players start on different continents when the map allows`.

### Task 4: New Game picks a size and generates the map

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/fx/NewGameDialog.java`
  - Replace `MapOption`, the `mapChoice` combo and `mapResource` with a
    `ComboBox<MapSize>` and `public MapSize mapSize`.
  - The default is `MEDIUM`, and the label is "Map size:".
  - Header text: "Create a new game. Pick the players and the map size."
- Modify: `src/main/java/com/developingstorm/games/sad/fx/GameLifecycleHandler.java`:
  change `newGame(..., String mapResource)` to `newGame(..., MapSize mapSize)`.
- Modify: `src/main/java/com/developingstorm/games/sad/fx/GameMenuBar.java:289-295`: pass
  `settings.mapSize`.
- Modify: `src/main/java/com/developingstorm/games/sad/fx/SaDFxApplication.java`
  - In the launch game (around line 100) and in `newGame` (around line 398), replace
    `HexBoardMap.loadMapAsResource(...)` with `MapGenerator.generate(...)`.
  - The launch game uses `MapSize.MEDIUM`.

- [ ] **Step 1: Make the changes.**

- [ ] **Step 2: Run** `mvn -q clean compile` and then `mvn -q test`. Expected: both
  succeed.

- [ ] **Step 3: Check by hand.** Launch the app (`mvn javafx:run`) and confirm:
  - the launch map differs from MedMap;
  - New Game shows the size picker with the four sizes, and Medium is selected;
  - each size starts a game;
  - saving and loading a generated map restores it.

  If this can't be run here, say so and leave it for Pete.

- [ ] **Step 4: Commit** with `feat(ui): New Game picks a map size and generates the map`.

### Task 5: Re-record AI baselines and update docs

**Files:**
- Modify: `ai-configs/baselines/2026-09-baseline-MedMap.json` and
  `ai-configs/baselines/2026-09-baseline-BorderLand.json`
- Modify: `CLAUDE.md`, in the "Map" section.

- [ ] **Step 1: Re-record the baselines** with the same command as before. For each map:

  ```
  mvn -q exec:java -Dlogback.configurationFile=src/main/resources/logback-test.xml \
    -Dexec.mainClass=com.developingstorm.games.sad.testing.AITestCLI \
    -Dexec.args="metrics --games 20 --turn-limit 150 --seed 1000 --map <Map> --out ai-configs/baselines/2026-09-baseline-<Map>.json"
  ```

  This overwrites `game_test.log`, so copy Pete's log aside first and restore it afterwards.

- [ ] **Step 2: Update the CLAUDE.md "Map" section.** Say that:
  - New Game generates maps via `MapGenerator`, in the four sizes;
  - there is one connected ocean and water edges;
  - cities come from `CityPlacer`, at least 3 apart, with a coastal and inland mix;
  - starts are on different continents when the map allows;
  - the hand-made maps are kept for the AI tools.

- [ ] **Step 3: Commit** with `chore: re-record AI baselines for new city placement; document generated maps`.
  Put the new metric tables in the commit body.
