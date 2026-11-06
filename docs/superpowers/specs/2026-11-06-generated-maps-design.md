# Generated Maps

## Intent

**Why (Pete):** the game always uses the same hand-made map, so exploring is pointless. Once
you work out where you started, you already know the whole world. Every new game should be a
world you have to discover.

**What success looks like:** you press New Game, pick a size, and get a map you've never seen.
It plays well: varied continents, one connected ocean, and cities spread sensibly with both
coastal and inland ones.

## Goals

- **New Game generates a fresh map every time.** The map dropdown in the New Game dialog is
  replaced by a size picker:
  - Small 25×25
  - Medium 50×50 (the default)
  - Large 75×75
  - X-Large 100×100
- **Continents vary.** The number and size of continents differ from game to game, scaled to
  the map size.
- **One connected ocean** (Pete's rule):
  - Every water hex can be reached by sea from every other water hex.
  - No continent encloses a lake or inland sea.
- **Coastal and inland cities** (Pete's rule): most continents have both.
  - Interpretation (my call): any continent that has hexes with no water neighbour, and has
    at least two cities, gets at least one coastal city and at least one inland city.
  - Small islands with no inland hexes can have coastal cities only.
- **City spacing** (Pete's rule): no two cities are adjacent. The hard minimum is a distance
  of 3 hexes between any two cities, whether on the same continent or not.
- **Starts on different continents.** Each player starts in a coastal city on a different
  continent. CLAUDE.md already says this, but the current code doesn't enforce it.

## Non-goals (for now, per Pete)

- **No map settings beyond size.** No land percentage, continent count or other knobs.
- **No fairness balancing.** Players' home continents and distances apart are not evened
  out. Pete expects to want this eventually; nothing here should make it hard to add later.
- **No map preview or editing.**

## Anti-goals

- **No adjacent cities.** Pete named this as the thing that would make him wince.
- **No lakes or landlocked water** on a continent, and no ocean split into separate
  bodies of water.

## Constraints and existing system

**Map format:**
- A map is a `HexBoardMap`: a width × height grid of ints, where 0 is water and anything
  above 0 is land.
- Hex adjacency comes from `Location.getRing(1)` (odd-r offset layout). Never use dx/dy
  loops.
- `HexBoardMap` constructors call `LocationMap.init(width, height)`, which resets a global.
  The generator must build the map through `HexBoardMap` so that happens.

**Continents and cities:**
- Continents are computed by flood fill in `Board.calcContinents()`.
- Cities are placed at game creation in `Board.sprinkleCities()` (called from
  `Board.init()`). Today it places `(width + height) / 2` cities, and its spacing check gives
  up after 100 attempts.
- Starting cities are assigned in `CityManager.assignCities()`, which picks any unowned
  coastal city.

**Saves:** saved games already store their map inside the save file. Generated maps
therefore save and load with no format change. Old saves keep working.

**Randomness:**
- All randomness must go through `RandomUtil`. The headless metrics runs seed it
  (`RandomUtil.setSeed`), and tests rely on that.
- Generation must never loop forever. It retries on failure up to a fixed limit, then
  falls back so a game always starts.

**Where it runs:**
- The JavaFX app starts a game at launch using `MedMap.sdm` (`SaDFxApplication` around line
  100), and the New Game flow loads `mapResource` (around line 398).
- Both should use a generated map. The launch game uses Medium (my call).

**The hand-made maps** (`MedMap.sdm`, `BorderLand.sdm`, `SampleMap.sdm`) stay in the project.
The headless AI tools (`AITestCLI`, `HeadlessGameRunner`) and many tests load them by name and
need a stable map.

## Decisions made in conversation

- The New Game dropdown is replaced by the size picker (Pete).
- The four sizes and their dimensions above (Pete).
- City spacing minimum of 3 (proposed, Pete agreed).

## Builder's calls (mine, marked so Pete can check)

- **Map edges.** All edge hexes are water, so continents never touch the map edge and the
  ocean wraps around everything. This makes the one-ocean rule easy to guarantee.
  - Pete isn't sure he likes this but accepted it for now.
  - Keep the edge rule in one obvious place, so it's easy to drop later. Without it, the
    one-ocean and no-lake checks must still hold, with land allowed to touch the edge.
- **Land share.** Roughly 30–40% of the map is land, close to MedMap's 37%. Exact numbers
  and how they vary are up to the builder.
- **City count** stays at `(width + height) / 2`: 25, 50, 75 and 100 cities by size.
  - It may come out a little lower when the spacing rule can't fit them all.
  - It must never place cities closer together than allowed to hit the count.
- **City distribution.** Continents get cities roughly in proportion to their land area.
  Every continent large enough to be worth landing on gets at least one, as today's "spread
  to most continents first" intent does.
- **Continent count.** There are always at least as many continents with coastal cities as
  there are players, so starts can be on different continents.
- **One placement rule for every map.** The new placement (hard spacing, coastal/inland mix)
  replaces `sprinkleCities` for hand-made maps too, rather than keeping two placers.
  - Consequence: city layouts on MedMap and BorderLand change, so the AI metric baselines in
    `ai-configs/baselines/` no longer match.
  - Re-record them with the same command after this lands, and note it in the commit.
- **Different-continent starts on every map.** `assignCities()` enforces different
  continents on all maps, hand-made included, rather than branching on map type.
  - If a map can't satisfy it (not enough continents with a free coastal city), fall back to
    any free coastal city as today, so a game always starts.
  - This only affects hand-made maps in the headless AI tools, since New Game no longer
    offers them.
  - On BorderLand, where one big continent holds about 80% of the land, one player will
    usually start on an offshore island. That's another reason to re-record the baselines.
- **Generation approach** is the builder's choice: seeded growth of continents, noise with
  thresholding, or similar. Afterwards it must check the rules:
  - water is fully connected;
  - no water is enclosed by land (if lakes appear, fill them with land);
  - the continent count and land share are in range.
  - If a map fails, regenerate it.
- **Small 25×25** is cramped: about 25 cities with spacing 3 on roughly 200 land hexes. If
  the rules can't all be met there, keep spacing and the one ocean, and let the city count or
  the coastal/inland mix give way.

## Testing

Use property tests over many seeds for each size:
- **Ocean:** water is one connected region, and no water region is enclosed.
- **Edges:** every edge hex is water.
- **Shape:** land share is within range, and there are at least as many continents with
  coastal cities as players.
- **Cities:**
  - no two cities are within distance 2;
  - continents that qualify have both coastal and inland cities;
  - the city count is close to the target.
- **Starts:** the players' starting cities are on different continents.
- **Variety:** across seeds, the continent count varies; it isn't the same every time.
- **Reproducibility:** the same seed gives the same map.
- **Performance:** generating X-Large takes well under a second.

UI: the New Game dialog shows the size picker, and starting a game with each size works.
Check this by hand.

## Left to the builder

- The generation algorithm and its tuning.
- Where the generator lives (a new class next to `HexBoardMap`/`Board` is the natural fit).
- How continent sizes and counts vary within the rules above.
- How the dialog's size picker is labelled, as long as it shows the four sizes.
