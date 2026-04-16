# LLM State Serializer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a state serializer that converts the game state visible to an AI player into a compact text format suitable for LLM consumption, plus an LLM brain implementation that uses it to make decisions via API calls.

**Architecture:** A new `LLMStateSerializer` class produces a line-oriented text representation of everything a `Player` can see: own units, cities, visible enemies, terrain context, and strategic summary. A new `LLMBrain` implements `IBrain` and calls an LLM API each turn, sending the serialized state and parsing structured order responses. The existing `Robot.setBrain()` injection point means zero changes to the game engine.

**Tech Stack:** Java 17, Anthropic Java SDK (via Maven), existing game model classes.

---

## File Structure

| File | Responsibility |
|------|---------------|
| Create: `src/main/java/.../sad/llm/LLMStateSerializer.java` | Converts `Game`+`Player` state into compact text |
| Create: `src/main/java/.../sad/llm/LLMBrain.java` | Implements `IBrain`, calls LLM API, parses responses into Orders |
| Create: `src/main/java/.../sad/llm/LLMOrderParser.java` | Validates and converts LLM text output into legal `Order` objects |
| Create: `src/main/java/.../sad/llm/LLMSystemPrompt.java` | Holds the system prompt explaining game rules and action format |
| Create: `src/test/java/.../sad/llm/LLMStateSerializerTest.java` | Tests serializer output for correctness and completeness |
| Create: `src/test/java/.../sad/llm/LLMOrderParserTest.java` | Tests order parsing from LLM response text |
| Create: `src/test/java/.../sad/llm/LLMBrainTest.java` | Integration test: serializer + parser with mock LLM |

Note: all paths below use `com/developingstorm/games/sad/llm/` as the package. The full prefix `src/main/java/com/developingstorm/games/sad/llm/` is abbreviated to `...sad/llm/` in step descriptions.

---

## Task 1: LLMStateSerializer - Header and City Sections

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/llm/LLMStateSerializer.java`
- Create: `src/test/java/com/developingstorm/games/sad/llm/LLMStateSerializerTest.java`

This task builds the serializer skeleton with the global header and city sections. We test against a real `Game` instance using the existing `HeadlessGameRunner` pattern for setup.

- [ ] **Step 1: Write the failing test for header serialization**

Create the test file. We need a test helper that creates a minimal game with two AI players, since `Game` requires a fully initialized board. The test verifies the header line contains turn number, player name, city/unit counts, and map dimensions.

```java
package com.developingstorm.games.sad.llm;

import static org.junit.Assert.*;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.sad.*;
import com.developingstorm.games.sad.brain.RobotBrain;
import org.junit.Before;
import org.junit.Test;

public class LLMStateSerializerTest {

    private Game game;
    private Player player;
    private LLMStateSerializer serializer;

    @Before
    public void setUp() {
        // Load the standard map
        HexBoardMap map = HexBoardMap.load("war.map");
        HexBoardContext ctx = new HexBoardContext() {
            @Override public int getWidth() { return 20; }
            @Override public int getHeight() { return 20; }
        };

        Player[] players = new Player[2];
        Robot robot1 = new Robot("TestAI1", 1);
        robot1.setBrain(new RobotBrain(robot1));
        players[0] = robot1;

        Robot robot2 = new Robot("TestAI2", 2);
        robot2.setBrain(new RobotBrain(robot2));
        players[1] = robot2;

        UnitNames.autoAssignThemes(2);
        game = new Game(players, map, ctx);
        player = players[0];
        serializer = new LLMStateSerializer();
    }

    @Test
    public void testHeaderContainsTurnAndPlayer() {
        String output = serializer.serialize(game, player);
        String firstLine = output.split("\n")[0];

        assertTrue("Header should contain TURN", firstLine.contains("TURN"));
        assertTrue("Header should contain player name", firstLine.contains("TestAI1"));
        assertTrue("Header should contain MAP dimensions", firstLine.contains("MAP"));
    }

    @Test
    public void testOwnCitiesSection() {
        String output = serializer.serialize(game, player);

        assertTrue("Should have CITIES section", output.contains("CITIES:"));
        // Player starts with 1 city
        String citiesSection = extractSection(output, "CITIES:", "UNITS_AWAITING_ORDERS:");
        // Each city line should have name, location, and production info
        String[] cityLines = citiesSection.trim().split("\n");
        assertTrue("Should have at least 1 city", cityLines.length >= 1);
        // City line should contain coordinates in (x,y) format
        assertTrue("City should have coordinates", cityLines[0].matches(".*\\(\\d+,\\d+\\).*"));
    }

    /** Extract text between two section headers */
    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start == -1) return "";
        start += startMarker.length();
        int end = text.indexOf(endMarker, start);
        if (end == -1) end = text.length();
        return text.substring(start, end);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMStateSerializerTest -Dsurefire.useFile=false`
Expected: Compilation failure — `LLMStateSerializer` class does not exist.

- [ ] **Step 3: Implement the serializer with header and cities**

```java
package com.developingstorm.games.sad.llm;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.*;
import java.util.List;

public class LLMStateSerializer {

    public String serialize(Game game, Player player) {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb, game, player);
        appendOwnCities(sb, player);
        appendUnitsAwaitingOrders(sb, game, player);
        appendUnitsOnOrders(sb, player);
        appendVisibleEnemies(sb, player);
        appendEnemyCities(sb, player);
        appendStrategicSummary(sb, game, player);
        return sb.toString();
    }

    private void appendHeader(StringBuilder sb, Game game, Player player) {
        int cityCount = 0;
        for (City c : game.getBoard().getCities()) {
            if (c.getOwner() == player) cityCount++;
        }
        sb.append("TURN ").append(game.getTurn());
        sb.append(" | PLAYER ").append(player.getName());
        sb.append(" | CITIES ").append(cityCount);
        sb.append(" | UNITS ").append(player.getUnits().size());
        sb.append(" | MAP ").append(game.getBoard().getWidth());
        sb.append("x").append(game.getBoard().getHeight());
        sb.append("\n");
    }

    private void appendOwnCities(StringBuilder sb, Player player) {
        sb.append("CITIES:\n");
        for (City c : player.getCities()) {
            sb.append("  ").append(c.getName());
            sb.append(" (").append(c.getLocation().x).append(",").append(c.getLocation().y).append(")");
            if (c.isCoastal()) sb.append(" coastal");
            else sb.append(" inland");
            Continent cont = c.getContinent();
            if (cont != null) sb.append(" cont=").append(cont.getName());
            sb.append(" | ");
            if (c.getProduction() != null) {
                sb.append("PRODUCING ").append(c.getProduction().getAbr());
            } else {
                sb.append("IDLE");
            }
            sb.append("\n");
        }
    }

    // Stub methods for remaining sections — implemented in later tasks
    private void appendUnitsAwaitingOrders(StringBuilder sb, Game game, Player player) {
        sb.append("UNITS_AWAITING_ORDERS:\n");
    }

    private void appendUnitsOnOrders(StringBuilder sb, Player player) {
        sb.append("UNITS_ON_ORDERS:\n");
    }

    private void appendVisibleEnemies(StringBuilder sb, Player player) {
        sb.append("ENEMIES:\n");
    }

    private void appendEnemyCities(StringBuilder sb, Player player) {
        sb.append("ENEMY_CITIES:\n");
    }

    private void appendStrategicSummary(StringBuilder sb, Game game, Player player) {
        sb.append("STRATEGY:\n");
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMStateSerializerTest -Dsurefire.useFile=false`
Expected: 2 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/llm/LLMStateSerializer.java src/test/java/com/developingstorm/games/sad/llm/LLMStateSerializerTest.java
git commit -m "feat(llm): add LLMStateSerializer with header and city sections"
```

---

## Task 2: Serialize Units Awaiting Orders (with local context)

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/llm/LLMStateSerializer.java`
- Modify: `src/test/java/com/developingstorm/games/sad/llm/LLMStateSerializerTest.java`

This is the most important section — it provides the decision context for each unit the LLM must assign orders to. Each unit gets its stats, 1-ring neighborhood (terrain + occupants), and pre-computed strategic distances.

- [ ] **Step 1: Write the failing test**

Add to `LLMStateSerializerTest.java`:

```java
@Test
public void testUnitsAwaitingOrdersSection() {
    String output = serializer.serialize(game, player);
    // Player starts with at least 1 infantry unit
    String section = extractSection(output, "UNITS_AWAITING_ORDERS:", "UNITS_ON_ORDERS:");

    // Should contain unit ID prefix
    assertTrue("Should list units with # prefix", section.contains("#"));
    // Should contain unit type
    assertTrue("Should contain unit type abbreviation",
        section.contains("I") || section.contains("Infantry"));
    // Should contain hp info
    assertTrue("Should contain hp", section.contains("hp="));
    // Should contain NEARBY terrain context
    assertTrue("Should contain NEARBY", section.contains("NEARBY:"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMStateSerializerTest#testUnitsAwaitingOrdersSection -Dsurefire.useFile=false`
Expected: FAIL — UNITS_AWAITING_ORDERS section is empty (stub).

- [ ] **Step 3: Implement appendUnitsAwaitingOrders**

Replace the stub in `LLMStateSerializer.java`:

```java
private void appendUnitsAwaitingOrders(StringBuilder sb, Game game, Player player) {
    sb.append("UNITS_AWAITING_ORDERS:\n");
    for (Unit u : player.getUnits()) {
        if (u.hasOrders() || u.isDead()) continue;
        appendUnitDetail(sb, game, player, u);
    }
}

private void appendUnitDetail(StringBuilder sb, Game game, Player player, Unit u) {
    Location loc = u.getLocation();
    sb.append("  #").append(u.getId());
    sb.append(" ").append(u.getType().getAbr());
    sb.append(" (").append(loc.x).append(",").append(loc.y).append(")");
    sb.append(" hp=").append(u.getHits()).append("/").append(u.getType().getHits());
    sb.append(" mov=").append(u.getMovesLeft()).append("/").append(u.getType().getDist());

    // Fuel for air units
    if (u.getType().getFuel() > 0) {
        sb.append(" fuel=").append(u.getFuel()).append("/").append(u.getType().getFuel());
    }

    // Carried units
    if (u.getCarrying() != null && !u.getCarrying().isEmpty()) {
        sb.append(" cargo=[");
        boolean first = true;
        int totalWeight = 0;
        for (Unit carried : u.getCarrying()) {
            if (!first) sb.append(",");
            sb.append(carried.getType().getAbr()).append("#").append(carried.getId());
            totalWeight += carried.getType().getWeight();
            first = false;
        }
        sb.append("] weight=").append(totalWeight).append("/").append(u.getType().getCarryCount());
    }

    // If this unit is carried
    if (u.isCarried()) {
        sb.append(" [aboard #").append(u.getTransport().getId()).append("]");
    }

    sb.append("\n");

    // NEARBY: 1-ring neighborhood
    sb.append("    NEARBY:");
    List<Location> ring = loc.getRing(1);
    for (Location neighbor : ring) {
        if (!game.getBoard().onBoard(neighbor)) continue;
        sb.append(" ");
        if (game.getBoard().isLand(neighbor)) {
            sb.append("land");
        } else {
            sb.append("water");
        }
        sb.append("@(").append(neighbor.x).append(",").append(neighbor.y).append(")");

        // Check for city
        City city = game.getBoard().getCity(neighbor);
        if (city != null) {
            sb.append("[city:").append(city.getName());
            if (city.getOwner() == player) sb.append(",own");
            else if (city.getOwner() != null) sb.append(",enemy");
            else sb.append(",neutral");
            sb.append("]");
        }

        // Check for visible unit
        Unit occupant = player.visibleUnit(neighbor);
        if (occupant != null && occupant != u) {
            sb.append("[").append(occupant.getOwner() == player ? "own:" : "enemy:");
            sb.append(occupant.getType().getAbr()).append("]");
        }
    }
    sb.append("\n");

    // CONTEXT: pre-computed strategic info
    sb.append("    CONTEXT:");
    appendContextLine(sb, game, player, u);
    sb.append("\n");
}

private void appendContextLine(StringBuilder sb, Game game, Player player, Unit u) {
    Location loc = u.getLocation();

    // Distance to nearest friendly city
    int nearestCityDist = Integer.MAX_VALUE;
    for (City c : player.getCities()) {
        int d = loc.distance(c.getLocation());
        if (d < nearestCityDist) nearestCityDist = d;
    }
    if (nearestCityDist < Integer.MAX_VALUE) {
        sb.append(" nearest_city=").append(nearestCityDist);
    }

    // Distance to nearest visible enemy
    int nearestEnemyDist = Integer.MAX_VALUE;
    for (Unit enemy : player.getKnownEnemies()) {
        int d = loc.distance(enemy.getLocation());
        if (d < nearestEnemyDist) nearestEnemyDist = d;
    }
    if (nearestEnemyDist < Integer.MAX_VALUE) {
        sb.append(" nearest_enemy=").append(nearestEnemyDist);
    }

    // Continent
    Continent cont = game.getBoard().getContinent(loc);
    if (cont != null) {
        sb.append(" cont=").append(cont.getName());
    }
}
```

Note: This uses `u.getHits()`, `u.getMovesLeft()`, `u.getFuel()`, `u.getCarrying()`, `u.getTransport()`, and `player.visibleUnit()`. Verify these method names exist on `Unit` and `Player` — check the actual signatures if the compiler errors, and adjust accordingly. The key accessors to verify:
- `Unit.getHits()` — may be `u.life.getHits()` or similar, check Unit.java
- `Unit.getMovesLeft()` — may be `u.life.movesLeft()`
- `Unit.getFuel()` — may be `u.life.getFuel()`
- `Unit.getCarrying()` — may be `u.getCarried()` or `u.carries` (the field is `ArrayList<Unit> carries`)
- `Unit.getTransport()` — may be `u.onboard` (the field name for the carrying unit)

Adjust method names to match the actual Unit API.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMStateSerializerTest -Dsurefire.useFile=false`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/llm/LLMStateSerializer.java src/test/java/com/developingstorm/games/sad/llm/LLMStateSerializerTest.java
git commit -m "feat(llm): serialize units awaiting orders with local context"
```

---

## Task 3: Serialize Units on Orders, Enemies, and Enemy Cities

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/llm/LLMStateSerializer.java`
- Modify: `src/test/java/com/developingstorm/games/sad/llm/LLMStateSerializerTest.java`

Fill in the remaining stubs: units already executing orders (compact format), visible enemy units, and known enemy/neutral cities.

- [ ] **Step 1: Write the failing tests**

Add to `LLMStateSerializerTest.java`:

```java
@Test
public void testUnitsOnOrdersSection() {
    // Give a unit an explore order so it appears in this section
    if (!player.getUnits().isEmpty()) {
        Unit u = player.getUnits().get(0);
        u.assignOrder(u.newExploreOrder());
    }
    String output = serializer.serialize(game, player);
    String section = extractSection(output, "UNITS_ON_ORDERS:", "ENEMIES:");
    // If unit was assigned explore, it should appear here
    if (!player.getUnits().isEmpty()) {
        assertTrue("Should show unit with order", section.contains("EXPLORE") || section.contains("Explore"));
    }
}

@Test
public void testEnemiesSection() {
    String output = serializer.serialize(game, player);
    // ENEMIES section should exist even if empty
    assertTrue("Should have ENEMIES section", output.contains("ENEMIES:"));
}

@Test
public void testEnemyCitiesSection() {
    String output = serializer.serialize(game, player);
    assertTrue("Should have ENEMY_CITIES section", output.contains("ENEMY_CITIES:"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMStateSerializerTest -Dsurefire.useFile=false`
Expected: `testUnitsOnOrdersSection` FAILS — section is empty.

- [ ] **Step 3: Implement the three remaining list sections**

Replace the stubs in `LLMStateSerializer.java`:

```java
private void appendUnitsOnOrders(StringBuilder sb, Player player) {
    sb.append("UNITS_ON_ORDERS:\n");
    for (Unit u : player.getUnits()) {
        if (!u.hasOrders() || u.isDead()) continue;
        Location loc = u.getLocation();
        sb.append("  #").append(u.getId());
        sb.append(" ").append(u.getType().getAbr());
        sb.append(" (").append(loc.x).append(",").append(loc.y).append(")");
        sb.append(" hp=").append(u.getHits()).append("/").append(u.getType().getHits());
        if (u.getType().getFuel() > 0) {
            sb.append(" fuel=").append(u.getFuel()).append("/").append(u.getType().getFuel());
        }
        sb.append(" ").append(u.getOrder().getType().getDisplayName());
        sb.append("\n");
    }
}

private void appendVisibleEnemies(StringBuilder sb, Player player) {
    sb.append("ENEMIES:\n");
    for (Unit enemy : player.getKnownEnemies()) {
        Location loc = enemy.getLocation();
        sb.append("  ").append(enemy.getType().getAbr());
        sb.append(" ").append(enemy.getOwner().getName());
        sb.append(" (").append(loc.x).append(",").append(loc.y).append(")");
        sb.append(" hp=?/").append(enemy.getType().getHits());
        sb.append("\n");
    }
    // Also include last-seen enemies (fog of war memory)
    for (Player.LastSeenInfo info : player.getLastSeenEnemies()) {
        sb.append("  [last seen] ").append(info.unitType.getAbr());
        sb.append(" ").append(info.owner.getName());
        sb.append(" (").append(info.location.x).append(",").append(info.location.y).append(")");
        sb.append(" turn=").append(info.turnSeen);
        sb.append("\n");
    }
}

private void appendEnemyCities(StringBuilder sb, Player player) {
    sb.append("ENEMY_CITIES:\n");
    for (City c : player.enemyCities()) {
        Location loc = c.getLocation();
        sb.append("  ").append(c.getName());
        sb.append(" (").append(loc.x).append(",").append(loc.y).append(")");
        sb.append(" owner=").append(c.getOwner().getName());
        if (c.isCoastal()) sb.append(" coastal");
        Continent cont = c.getContinent();
        if (cont != null) sb.append(" cont=").append(cont.getName());
        sb.append("\n");
    }
    sb.append("NEUTRAL_CITIES:\n");
    for (City c : player.getUnownedCities()) {
        Location loc = c.getLocation();
        sb.append("  ").append(c.getName());
        sb.append(" (").append(loc.x).append(",").append(loc.y).append(")");
        if (c.isCoastal()) sb.append(" coastal");
        Continent cont = c.getContinent();
        if (cont != null) sb.append(" cont=").append(cont.getName());
        sb.append("\n");
    }
}
```

Note: Verify `player.getUnownedCities()` exists — the field is `unownedCities` (HashSet). There may be a getter or it may be `protected`. If no public getter exists, add one to `Player.java`:
```java
public Set<City> getUnownedCities() { return unownedCities; }
```

Similarly verify `player.getKnownEnemies()` and `player.getLastSeenEnemies()` return types.

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMStateSerializerTest -Dsurefire.useFile=false`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/llm/LLMStateSerializer.java src/test/java/com/developingstorm/games/sad/llm/LLMStateSerializerTest.java
git commit -m "feat(llm): serialize units on orders, enemies, and enemy cities"
```

---

## Task 4: Strategic Summary Section

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/llm/LLMStateSerializer.java`
- Modify: `src/test/java/com/developingstorm/games/sad/llm/LLMStateSerializerTest.java`

Compute continent control status and force balance to give the LLM strategic context without requiring it to derive this from raw coordinates.

- [ ] **Step 1: Write the failing test**

Add to `LLMStateSerializerTest.java`:

```java
@Test
public void testStrategicSummaryHasContent() {
    String output = serializer.serialize(game, player);
    String section = extractSection(output, "STRATEGY:", "");

    // Should classify at least one continent
    assertTrue("Strategy should mention at least one continent status",
        section.contains("SECURE:") || section.contains("CONTESTED:")
        || section.contains("UNEXPLORED:") || section.contains("FORCE_BALANCE:"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMStateSerializerTest#testStrategicSummaryHasContent -Dsurefire.useFile=false`
Expected: FAIL — strategy section is empty (stub).

- [ ] **Step 3: Implement appendStrategicSummary**

Replace the stub:

```java
private void appendStrategicSummary(StringBuilder sb, Game game, Player player) {
    sb.append("STRATEGY:\n");

    // Classify continents
    List<String> secure = new ArrayList<>();
    List<String> contested = new ArrayList<>();
    List<String> unexplored = new ArrayList<>();

    for (Continent cont : game.getBoard().getContinents()) {
        boolean hasOwnCity = false;
        boolean hasEnemyCity = false;
        boolean hasNeutralCity = false;
        boolean anyExplored = false;

        for (City c : cont.getCities()) {
            if (c.getOwner() == player) hasOwnCity = true;
            else if (c.getOwner() != null) hasEnemyCity = true;
            else hasNeutralCity = true;
        }

        // Check if player has explored any hex on this continent
        for (Location loc : cont.getLocations()) {
            if (player.isExplored(loc)) {
                anyExplored = true;
                break;
            }
        }

        String label = cont.getName() + "(" + cont.getCityCount() + " cities)";

        if (!anyExplored) {
            unexplored.add(label);
        } else if (hasOwnCity && !hasEnemyCity && !hasNeutralCity) {
            secure.add(label);
        } else {
            String detail = cont.getName() + "(";
            List<String> parts = new ArrayList<>();
            if (hasOwnCity) parts.add("own");
            if (hasEnemyCity) parts.add("enemy");
            if (hasNeutralCity) parts.add("neutral");
            detail += String.join("+", parts) + ", " + cont.getCityCount() + " cities)";
            contested.add(detail);
        }
    }

    if (!secure.isEmpty()) sb.append("  SECURE: ").append(String.join(", ", secure)).append("\n");
    if (!contested.isEmpty()) sb.append("  CONTESTED: ").append(String.join(", ", contested)).append("\n");
    if (!unexplored.isEmpty()) sb.append("  UNEXPLORED: ").append(String.join(", ", unexplored)).append("\n");

    // Force balance
    int ownUnits = player.getUnits().size();
    int enemyUnitsVisible = player.getKnownEnemies().size();
    sb.append("  FORCE_BALANCE: own_units=").append(ownUnits);
    sb.append(" visible_enemy_units=").append(enemyUnitsVisible);
    sb.append("\n");
}
```

Note: Verify `game.getBoard().getContinents()` returns the `Set<Continent>`. The field is `continents` on `Board` — check if there's a public getter. If not, add one:
```java
public Set<Continent> getContinents() { return continents; }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMStateSerializerTest -Dsurefire.useFile=false`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/llm/LLMStateSerializer.java src/test/java/com/developingstorm/games/sad/llm/LLMStateSerializerTest.java
git commit -m "feat(llm): add strategic summary with continent classification"
```

---

## Task 5: LLMOrderParser — Parse LLM Responses into Orders

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/llm/LLMOrderParser.java`
- Create: `src/test/java/com/developingstorm/games/sad/llm/LLMOrderParserTest.java`

The LLM will respond with one order per unit in a structured text format. This parser converts that text into `Order` objects. We define the response format as:

```
ORDERS:
#1042 MOVE 30,40
#1099 EXPLORE
#1077 SENTRY
#1050 ATTACK 11,21
#1033 SKIP
#1088 HEAD_HOME
```

- [ ] **Step 1: Write the failing tests**

```java
package com.developingstorm.games.sad.llm;

import static org.junit.Assert.*;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.*;
import com.developingstorm.games.sad.brain.RobotBrain;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class LLMOrderParserTest {

    private Game game;
    private Player player;
    private LLMOrderParser parser;

    @Before
    public void setUp() {
        HexBoardMap map = HexBoardMap.load("war.map");
        HexBoardContext ctx = new HexBoardContext() {
            @Override public int getWidth() { return 20; }
            @Override public int getHeight() { return 20; }
        };

        Player[] players = new Player[2];
        Robot robot1 = new Robot("TestAI1", 1);
        robot1.setBrain(new RobotBrain(robot1));
        players[0] = robot1;
        Robot robot2 = new Robot("TestAI2", 2);
        robot2.setBrain(new RobotBrain(robot2));
        players[1] = robot2;

        UnitNames.autoAssignThemes(2);
        game = new Game(players, map, ctx);
        player = players[0];
        parser = new LLMOrderParser(game, player);
    }

    @Test
    public void testParseExploreOrder() {
        Unit unit = player.getUnits().get(0);
        String response = "ORDERS:\n#" + unit.getId() + " EXPLORE\n";

        Map<Long, Order> orders = parser.parse(response);

        assertTrue("Should have order for unit", orders.containsKey(unit.getId()));
        assertEquals(OrderType.EXPLORE, orders.get(unit.getId()).getType());
    }

    @Test
    public void testParseSkipOrder() {
        Unit unit = player.getUnits().get(0);
        String response = "ORDERS:\n#" + unit.getId() + " SKIP\n";

        Map<Long, Order> orders = parser.parse(response);

        assertTrue(orders.containsKey(unit.getId()));
        assertEquals(OrderType.SKIPTURN, orders.get(unit.getId()).getType());
    }

    @Test
    public void testParseMoveOrder() {
        Unit unit = player.getUnits().get(0);
        // Use a valid land location near the unit
        Location target = unit.getLocation().getRing(1).stream()
            .filter(l -> game.getBoard().onBoard(l) && game.getBoard().isLand(l))
            .findFirst().orElse(unit.getLocation());

        String response = "ORDERS:\n#" + unit.getId() + " MOVE " + target.x + "," + target.y + "\n";

        Map<Long, Order> orders = parser.parse(response);

        assertTrue(orders.containsKey(unit.getId()));
        assertEquals(OrderType.MOVE, orders.get(unit.getId()).getType());
    }

    @Test
    public void testParseIgnoresUnknownUnitIds() {
        String response = "ORDERS:\n#999999 EXPLORE\n";

        Map<Long, Order> orders = parser.parse(response);

        assertTrue("Should skip unknown unit IDs", orders.isEmpty());
    }

    @Test
    public void testParseIgnoresGarbage() {
        String response = "I think we should attack.\nORDERS:\nLet me think...\n";

        Map<Long, Order> orders = parser.parse(response);

        assertTrue("Should handle garbage gracefully", orders.isEmpty());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMOrderParserTest -Dsurefire.useFile=false`
Expected: Compilation failure — `LLMOrderParser` does not exist.

- [ ] **Step 3: Implement LLMOrderParser**

```java
package com.developingstorm.games.sad.llm;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.*;
import com.developingstorm.games.sad.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLMOrderParser {

    private static final Pattern ORDER_LINE = Pattern.compile(
        "^#(\\d+)\\s+(\\w+)(?:\\s+(\\d+),(\\d+))?(?:\\s+#(\\d+))?$"
    );

    private final Game game;
    private final Player player;

    public LLMOrderParser(Game game, Player player) {
        this.game = game;
        this.player = player;
    }

    public Map<Long, Order> parse(String response) {
        Map<Long, Order> orders = new HashMap<>();
        boolean inOrders = false;

        for (String line : response.split("\n")) {
            line = line.trim();
            if (line.equals("ORDERS:")) {
                inOrders = true;
                continue;
            }
            if (!inOrders) continue;

            Matcher m = ORDER_LINE.matcher(line);
            if (!m.matches()) continue;

            long unitId = Long.parseLong(m.group(1));
            String command = m.group(2).toUpperCase();

            // Find the unit — must belong to this player
            Unit unit = findOwnUnit(unitId);
            if (unit == null) {
                Log.warn("LLMOrderParser: unknown unit #" + unitId);
                continue;
            }

            Order order = createOrder(unit, command, m.group(3), m.group(4), m.group(5));
            if (order != null) {
                orders.put(unitId, order);
            }
        }

        return orders;
    }

    private Unit findOwnUnit(long id) {
        for (Unit u : player.getUnits()) {
            if (u.getId() == id) return u;
        }
        return null;
    }

    private Order createOrder(Unit unit, String command, String xStr, String yStr, String targetUnitStr) {
        switch (command) {
            case "EXPLORE":
                return unit.newExploreOrder();
            case "SKIP":
                return unit.newSkipTurn();
            case "SENTRY":
                return unit.newSentryOrder();
            case "HEAD_HOME":
                return unit.newHeadHomeOrder();
            case "UNLOAD":
                return unit.newUnloadOrder();
            case "MOVE":
                if (xStr != null && yStr != null) {
                    Location target = Location.get(Integer.parseInt(xStr), Integer.parseInt(yStr));
                    return unit.newMoveOrder(target);
                }
                Log.warn("LLMOrderParser: MOVE requires coordinates for unit #" + unit.getId());
                return unit.newSkipTurn();
            case "ATTACK":
                if (xStr != null && yStr != null) {
                    Location target = Location.get(Integer.parseInt(xStr), Integer.parseInt(yStr));
                    return unit.newAttackOrder(target);
                }
                Log.warn("LLMOrderParser: ATTACK requires coordinates for unit #" + unit.getId());
                return unit.newSkipTurn();
            case "ESCORT":
                if (targetUnitStr != null) {
                    long escortId = Long.parseLong(targetUnitStr);
                    Unit escortTarget = game.getUnitById(escortId);
                    if (escortTarget != null) {
                        return unit.newEscortOrder(escortTarget);
                    }
                }
                Log.warn("LLMOrderParser: ESCORT requires target unit for unit #" + unit.getId());
                return unit.newSkipTurn();
            default:
                Log.warn("LLMOrderParser: unknown command '" + command + "' for unit #" + unit.getId());
                return unit.newSkipTurn();
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMOrderParserTest -Dsurefire.useFile=false`
Expected: All 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/llm/LLMOrderParser.java src/test/java/com/developingstorm/games/sad/llm/LLMOrderParserTest.java
git commit -m "feat(llm): add LLMOrderParser to convert LLM responses into Orders"
```

---

## Task 6: LLM System Prompt

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/llm/LLMSystemPrompt.java`
- Create: `src/test/java/com/developingstorm/games/sad/llm/LLMSystemPromptTest.java`

The system prompt explains the game rules, the state format, and the expected response format to the LLM. It's a static string but we put it in its own class for maintainability and to make it testable (verify it contains all order types, etc.).

- [ ] **Step 1: Write the failing test**

```java
package com.developingstorm.games.sad.llm;

import static org.junit.Assert.*;
import org.junit.Test;

public class LLMSystemPromptTest {

    @Test
    public void testPromptContainsAllOrderTypes() {
        String prompt = LLMSystemPrompt.get();

        assertTrue(prompt.contains("MOVE"));
        assertTrue(prompt.contains("EXPLORE"));
        assertTrue(prompt.contains("SENTRY"));
        assertTrue(prompt.contains("SKIP"));
        assertTrue(prompt.contains("ATTACK"));
        assertTrue(prompt.contains("HEAD_HOME"));
        assertTrue(prompt.contains("UNLOAD"));
        assertTrue(prompt.contains("ESCORT"));
    }

    @Test
    public void testPromptExplainsResponseFormat() {
        String prompt = LLMSystemPrompt.get();

        assertTrue("Should explain ORDERS: format", prompt.contains("ORDERS:"));
        assertTrue("Should show unit ID syntax", prompt.contains("#"));
    }

    @Test
    public void testPromptExplainsUnitTypes() {
        String prompt = LLMSystemPrompt.get();

        assertTrue(prompt.contains("Infantry"));
        assertTrue(prompt.contains("Transport"));
        assertTrue(prompt.contains("Fighter"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMSystemPromptTest -Dsurefire.useFile=false`
Expected: Compilation failure.

- [ ] **Step 3: Implement LLMSystemPrompt**

```java
package com.developingstorm.games.sad.llm;

public class LLMSystemPrompt {

    public static String get() {
        return PROMPT;
    }

    private static final String PROMPT = """
You are an AI player in a hex-grid turn-based strategy game called Search and Destroy.

## Goal
Capture all enemy cities and destroy all enemy units to win.

## Unit Types
| Abbr | Type | Travel | Moves | HP | Attack | Notes |
|------|------|--------|-------|----|--------|-------|
| I | Infantry | LAND | 1 | 2 | 1 | Captures cities (consumed on capture attempt) |
| A | Armor | LAND | 2 | 4 | 2 | Captures cities, stronger but heavier |
| F | Fighter | AIR | 5 | 2 | 1 | Must land at city/carrier before fuel=0 |
| B | Bomber | AIR | 4 | 2 | 3 | Bombs cities (cannot capture), strong attack |
| C | Cargo Plane | AIR | 3 | 2 | 0 | Carries 1 infantry, loads only in cities |
| DE | Destroyer | SEA | 3 | 3 | 3 | Detects submarines |
| TR | Transport | SEA | 2 | 2 | 0 | Carries infantry/armor (capacity 6 weight) |
| SU | Submarine | SEA | 2 | 4 | 4 | Invisible to most units |
| CR | Cruiser | SEA | 2 | 8 | 3 | Detects submarines |
| AC | Carrier | SEA | 2 | 6 | 1 | Carries fighters (capacity 6) |
| BA | Battleship | SEA | 2 | 12 | 4 | Strongest ship |

## Key Rules
- 1 unit per hex (exceptions: cities, transports/carriers, air units can share with 1 ground/sea unit)
- Land units capture cities. The attacker is always consumed on capture.
- Unowned cities: 50% capture chance. Enemy cities: fight defenders first, then 50%.
- Combat: attacker and defender trade blows (50% hit chance each round) until one dies.
- Moving into your own city repairs (+1 hp) and refuels.
- Transports in SENTRY mode at cities auto-load compatible units.

## State Format
You receive game state in sections: TURN header, CITIES, UNITS_AWAITING_ORDERS, UNITS_ON_ORDERS, ENEMIES, ENEMY_CITIES, NEUTRAL_CITIES, STRATEGY.

Each unit awaiting orders shows:
- ID, type abbreviation, location (x,y), hp, moves, fuel (if air), cargo (if carrier/transport)
- NEARBY: 1-ring neighborhood with terrain and occupants
- CONTEXT: distance to nearest city, nearest enemy, continent name

## Response Format
Respond with ONLY an ORDERS block. One line per unit that needs orders. Format:

```
ORDERS:
#<unit_id> <COMMAND> [arguments]
```

Available commands:
- `MOVE x,y` — pathfind to target hex
- `EXPLORE` — move toward nearest unexplored area
- `SENTRY` — sleep until enemy approaches (also: load mode for transports in cities)
- `SKIP` — do nothing this turn
- `ATTACK x,y` — move to target hex and attack
- `HEAD_HOME` — pathfind to nearest friendly city
- `UNLOAD` — disembark from transport / unload cargo
- `ESCORT #unit_id` — follow and protect another unit

## Production Format
When asked about city production, respond with:

```
PRODUCTION:
<city_name> <unit_type>
```

Where unit_type is one of: INFANTRY, ARMOR, FIGHTER, BOMBER, CARGO, DESTROYER, TRANSPORT, SUBMARINE, CRUISER, CARRIER, BATTLESHIP

## Strategy Tips
- Explore early to find cities. Infantry are cheap scouts.
- Capture neutral cities quickly to grow your economy.
- Build transports to move armies across water.
- Protect transports with destroyers/cruisers.
- Fighters need fuel management — always have a city or carrier in range.
- Concentrate forces rather than spreading thin.
""";
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMSystemPromptTest -Dsurefire.useFile=false`
Expected: All 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/llm/LLMSystemPrompt.java src/test/java/com/developingstorm/games/sad/llm/LLMSystemPromptTest.java
git commit -m "feat(llm): add system prompt explaining game rules and response format"
```

---

## Task 7: LLMBrain — IBrain Implementation

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/llm/LLMBrain.java`
- Create: `src/test/java/com/developingstorm/games/sad/llm/LLMBrainTest.java`

This is the integration class. It implements `IBrain`, calls the LLM at the start of each turn with the full serialized state, and caches the parsed orders for `getOrders()` calls. Production decisions also go through the LLM.

For the initial implementation, we use a simple HTTP client to call the Anthropic Messages API directly (no SDK dependency needed). This keeps the Maven dependency minimal — just `java.net.http`.

- [ ] **Step 1: Write the failing test using a mock LLM**

We test the brain with a fake LLM that returns canned responses. This verifies the serializer->LLM->parser pipeline works end-to-end without needing an API key.

```java
package com.developingstorm.games.sad.llm;

import static org.junit.Assert.*;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.sad.*;
import com.developingstorm.games.sad.brain.RobotBrain;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;

public class LLMBrainTest {

    private Game game;
    private Robot robot;

    @Before
    public void setUp() {
        HexBoardMap map = HexBoardMap.load("war.map");
        HexBoardContext ctx = new HexBoardContext() {
            @Override public int getWidth() { return 20; }
            @Override public int getHeight() { return 20; }
        };

        Player[] players = new Player[2];
        robot = new Robot("LLM_AI", 1);
        // Don't set brain yet — we'll set it after game init
        robot.setBrain(new RobotBrain(robot)); // temporary
        players[0] = robot;

        Robot robot2 = new Robot("Enemy", 2);
        robot2.setBrain(new RobotBrain(robot2));
        players[1] = robot2;

        UnitNames.autoAssignThemes(2);
        game = new Game(players, map, ctx);
    }

    @Test
    public void testGetOrdersReturnsExploreForAllUnits() {
        // Mock LLM that responds with EXPLORE for every unit
        Function<String, String> mockLLM = prompt -> {
            StringBuilder response = new StringBuilder("ORDERS:\n");
            for (Unit u : robot.getUnits()) {
                response.append("#").append(u.getId()).append(" EXPLORE\n");
            }
            return response.toString();
        };

        LLMBrain brain = new LLMBrain(robot, mockLLM);
        robot.setBrain(brain);
        brain.startNewTurn();

        for (Unit u : robot.getUnits()) {
            Order order = brain.getOrders(u);
            assertNotNull("Every unit should get an order", order);
            assertEquals("Default mock returns EXPLORE", OrderType.EXPLORE, order.getType());
        }
    }

    @Test
    public void testGetOrdersFallsBackToSkipOnMissingUnit() {
        // Mock LLM that returns empty orders
        Function<String, String> mockLLM = prompt -> "ORDERS:\n";

        LLMBrain brain = new LLMBrain(robot, mockLLM);
        robot.setBrain(brain);
        brain.startNewTurn();

        for (Unit u : robot.getUnits()) {
            Order order = brain.getOrders(u);
            assertNotNull("Should fall back to skip", order);
            assertEquals(OrderType.SKIPTURN, order.getType());
        }
    }

    @Test
    public void testGetProductionReturnsInfantryByDefault() {
        Function<String, String> mockLLM = prompt -> "ORDERS:\n";

        LLMBrain brain = new LLMBrain(robot, mockLLM);
        robot.setBrain(brain);

        for (City c : robot.getCities()) {
            Type t = brain.getProduction(c);
            assertNotNull("Should always return a production type", t);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMBrainTest -Dsurefire.useFile=false`
Expected: Compilation failure — `LLMBrain` does not exist.

- [ ] **Step 3: Implement LLMBrain**

```java
package com.developingstorm.games.sad.llm;

import com.developingstorm.games.sad.*;
import com.developingstorm.games.sad.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class LLMBrain implements IBrain {

    private final Robot owner;
    private final Function<String, String> llmClient;
    private final LLMStateSerializer serializer;
    private final String systemPrompt;

    // Cached orders for current turn
    private Map<Long, Order> turnOrders;

    /**
     * Create with a real or mock LLM client.
     * The client function takes a user message (serialized state) and returns the LLM response text.
     */
    public LLMBrain(Robot owner, Function<String, String> llmClient) {
        this.owner = owner;
        this.llmClient = llmClient;
        this.serializer = new LLMStateSerializer();
        this.systemPrompt = LLMSystemPrompt.get();
        this.turnOrders = new HashMap<>();
    }

    @Override
    public void startNewTurn() {
        turnOrders.clear();

        Game game = owner.getGame();
        String state = serializer.serialize(game, owner);

        String prompt = systemPrompt + "\n\n---\n\nCurrent game state:\n\n" + state
            + "\nProvide orders for all units listed under UNITS_AWAITING_ORDERS.";

        try {
            String response = llmClient.apply(prompt);
            Log.debug(owner, "LLM response length: " + response.length());

            LLMOrderParser parser = new LLMOrderParser(game, owner);
            turnOrders = parser.parse(response);

            Log.debug(owner, "Parsed " + turnOrders.size() + " orders from LLM");
        } catch (Exception e) {
            Log.warn("LLM call failed: " + e.getMessage() + " — all units will skip");
        }
    }

    @Override
    public Order getOrders(Unit u) {
        Order order = turnOrders.get(u.getId());
        if (order != null) {
            return order;
        }
        // Fallback: skip turn if LLM didn't provide an order for this unit
        Log.warn("LLM provided no order for unit #" + u.getId() + " — skipping");
        return u.newSkipTurn();
    }

    @Override
    public Type getProduction(City c) {
        // For now, default production logic:
        // Coastal cities build transports, inland cities build infantry
        // TODO: Ask the LLM for production decisions too
        if (c.isCoastal()) {
            return Type.TRANSPORT;
        }
        return Type.INFANTRY;
    }
}
```

Note: `owner.getGame()` — verify this accessor exists on `Player`/`Robot`. The `game` field is set via `setGame()`. If there's no public `getGame()`, add one to `Player.java`:
```java
public Game getGame() { return this.game; }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMBrainTest -Dsurefire.useFile=false`
Expected: All 3 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/llm/LLMBrain.java src/test/java/com/developingstorm/games/sad/llm/LLMBrainTest.java
git commit -m "feat(llm): add LLMBrain implementing IBrain with mock-friendly design"
```

---

## Task 8: Anthropic API Client

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/llm/AnthropicClient.java`
- Create: `src/test/java/com/developingstorm/games/sad/llm/AnthropicClientTest.java`
- Modify: `pom.xml` (no new dependencies — uses `java.net.http`)

A lightweight HTTP client that calls the Anthropic Messages API. Returns the text content from the response. Uses `java.net.http.HttpClient` (built into Java 11+) so no Maven dependency changes are needed.

- [ ] **Step 1: Write the failing test**

This test verifies request formatting without actually calling the API. We test the request builder logic.

```java
package com.developingstorm.games.sad.llm;

import static org.junit.Assert.*;
import org.junit.Test;

public class AnthropicClientTest {

    @Test
    public void testBuildRequestBody() {
        AnthropicClient client = new AnthropicClient("test-key", "claude-sonnet-4-20250514");
        String body = client.buildRequestBody("system prompt", "user message");

        assertTrue("Should contain model", body.contains("claude-sonnet-4-20250514"));
        assertTrue("Should contain system", body.contains("system prompt"));
        assertTrue("Should contain user message", body.contains("user message"));
        assertTrue("Should set max_tokens", body.contains("max_tokens"));
    }

    @Test
    public void testParseResponseText() {
        AnthropicClient client = new AnthropicClient("test-key", "claude-sonnet-4-20250514");

        String responseJson = """
            {
              "content": [
                {
                  "type": "text",
                  "text": "ORDERS:\\n#1 EXPLORE"
                }
              ],
              "stop_reason": "end_turn"
            }
            """;

        String text = client.parseResponseText(responseJson);
        assertTrue(text.contains("ORDERS:"));
        assertTrue(text.contains("#1 EXPLORE"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.AnthropicClientTest -Dsurefire.useFile=false`
Expected: Compilation failure.

- [ ] **Step 3: Implement AnthropicClient**

```java
package com.developingstorm.games.sad.llm;

import com.developingstorm.games.sad.util.Log;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Function;

/**
 * Lightweight Anthropic Messages API client using java.net.http.
 * Implements Function<String, String> so it can be passed directly to LLMBrain.
 */
public class AnthropicClient implements Function<String, String> {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final String systemPrompt;

    public AnthropicClient(String apiKey, String model) {
        this(apiKey, model, null);
    }

    public AnthropicClient(String apiKey, String model, String systemPrompt) {
        this.apiKey = apiKey;
        this.model = model;
        this.systemPrompt = systemPrompt != null ? systemPrompt : LLMSystemPrompt.get();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }

    @Override
    public String apply(String userMessage) {
        String requestBody = buildRequestBody(systemPrompt, userMessage);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("anthropic-version", API_VERSION)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(120))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                Log.warn("Anthropic API error " + response.statusCode() + ": " + response.body());
                return "ORDERS:\n";
            }

            return parseResponseText(response.body());
        } catch (Exception e) {
            Log.warn("Anthropic API call failed: " + e.getMessage());
            return "ORDERS:\n";
        }
    }

    /** Build the JSON request body. Package-private for testing. */
    String buildRequestBody(String system, String userMessage) {
        // Manual JSON construction to avoid adding a JSON library dependency.
        // Escapes special characters in the message strings.
        return "{"
            + "\"model\":\"" + model + "\","
            + "\"max_tokens\":2048,"
            + "\"system\":\"" + escapeJson(system) + "\","
            + "\"messages\":[{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}]"
            + "}";
    }

    /** Extract text content from the API response JSON. Package-private for testing. */
    String parseResponseText(String responseJson) {
        // Simple extraction without a JSON library:
        // Find "text": "..." in the content array
        int textKeyIdx = responseJson.indexOf("\"text\"");
        if (textKeyIdx == -1) return "ORDERS:\n";

        int colonIdx = responseJson.indexOf(":", textKeyIdx);
        if (colonIdx == -1) return "ORDERS:\n";

        // Find the opening quote of the value
        int openQuote = responseJson.indexOf("\"", colonIdx + 1);
        if (openQuote == -1) return "ORDERS:\n";

        // Find the closing quote, handling escaped quotes
        StringBuilder result = new StringBuilder();
        for (int i = openQuote + 1; i < responseJson.length(); i++) {
            char c = responseJson.charAt(i);
            if (c == '\\' && i + 1 < responseJson.length()) {
                char next = responseJson.charAt(i + 1);
                if (next == '"') { result.append('"'); i++; }
                else if (next == 'n') { result.append('\n'); i++; }
                else if (next == '\\') { result.append('\\'); i++; }
                else if (next == 't') { result.append('\t'); i++; }
                else { result.append(c); }
            } else if (c == '"') {
                break;
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.AnthropicClientTest -Dsurefire.useFile=false`
Expected: Both tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/llm/AnthropicClient.java src/test/java/com/developingstorm/games/sad/llm/AnthropicClientTest.java
git commit -m "feat(llm): add AnthropicClient for Messages API via java.net.http"
```

---

## Task 9: Wire LLMBrain into HeadlessGameRunner

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/testing/HeadlessGameRunner.java`

Add support for running a game with one `LLMBrain` player vs one `RobotBrain` player. This lets you run `LLM vs AI` games from the command line for evaluation.

- [ ] **Step 1: Write the failing test**

Add to `LLMBrainTest.java`:

```java
@Test
public void testLLMBrainCanBeInjectedViaSetBrain() {
    Function<String, String> mockLLM = prompt -> {
        StringBuilder response = new StringBuilder("ORDERS:\n");
        for (Unit u : robot.getUnits()) {
            response.append("#").append(u.getId()).append(" EXPLORE\n");
        }
        return response.toString();
    };

    LLMBrain brain = new LLMBrain(robot, mockLLM);
    robot.setBrain(brain);

    // Verify the brain is wired in — startNewTurn should not throw
    brain.startNewTurn();

    // Verify orders work for each unit
    for (Unit u : robot.getUnits()) {
        assertNotNull(brain.getOrders(u));
    }
}
```

- [ ] **Step 2: Run test to verify it passes (confirms existing wiring)**

Run: `mvn test -pl . -Dtest=com.developingstorm.games.sad.llm.LLMBrainTest -Dsurefire.useFile=false`
Expected: All tests PASS (this test works with existing code).

- [ ] **Step 3: Add LLM player support to HeadlessGameRunner**

Add a new constructor and factory method to `HeadlessGameRunner.java`. Add this after the existing constructors:

```java
/**
 * Create a headless game runner with an LLM brain for player 1.
 *
 * @param map The map to play on
 * @param ctx The hex board context
 * @param turnLimit Maximum turns
 * @param llmApiKey Anthropic API key
 * @param llmModel Model name (e.g. "claude-sonnet-4-20250514")
 */
public static HeadlessGameRunner withLLMPlayer(
    HexBoardMap map, HexBoardContext ctx, int turnLimit,
    String llmApiKey, String llmModel
) {
    return new HeadlessGameRunner(map, ctx, turnLimit, null, null) {
        @Override
        protected void configurePlayers(Player[] players) {
            Robot llmRobot = (Robot) players[0];
            com.developingstorm.games.sad.llm.AnthropicClient client =
                new com.developingstorm.games.sad.llm.AnthropicClient(llmApiKey, llmModel);
            llmRobot.setBrain(
                new com.developingstorm.games.sad.llm.LLMBrain(llmRobot, client)
            );
        }
    };
}
```

To make this work, extract a `configurePlayers` hook from `initializeGame()`. In the existing `initializeGame()` method, after creating the `players` array and before `game = new Game(...)`, add:

```java
configurePlayers(players);
```

And add the default (no-op) method:

```java
protected void configurePlayers(Player[] players) {
    // Default: no additional configuration. Subclasses/overrides can inject custom brains.
}
```

- [ ] **Step 4: Run all tests to verify nothing broke**

Run: `mvn test -Dsurefire.useFile=false`
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/testing/HeadlessGameRunner.java src/test/java/com/developingstorm/games/sad/llm/LLMBrainTest.java
git commit -m "feat(llm): wire LLMBrain into HeadlessGameRunner for LLM vs AI games"
```

---

## Task 10: Add Missing Accessors and Run Full Test Suite

**Files:**
- Possibly modify: `src/main/java/com/developingstorm/games/sad/Player.java`
- Possibly modify: `src/main/java/com/developingstorm/games/sad/Unit.java`
- Possibly modify: `src/main/java/com/developingstorm/games/sad/Board.java`

During implementation of Tasks 1-9, several accessors may need to be added if they don't already exist. This task is a sweep to verify and add any missing public getters.

- [ ] **Step 1: Verify all required accessors exist**

Check for these methods. If missing, add them:

On `Player.java`:
- `public Game getGame()` — returns `this.game`
- `public Set<City> getUnownedCities()` — returns `this.unownedCities`
- `public Set<City> enemyCities()` — should already exist
- `public Set<Unit> getKnownEnemies()` — should already exist
- `public Collection<LastSeenInfo> getLastSeenEnemies()` — returns `this.lastSeenEnemies.values()`

On `Unit.java`:
- `public int getHits()` — may be on `life` inner class, expose on Unit if needed
- `public int getMovesLeft()` — may be `life.movesLeft()`
- `public int getFuel()` — may be `life.getFuel()`
- `public List<Unit> getCarrying()` — returns `this.carries`
- `public Unit getTransport()` — returns `this.onboard`

On `Board.java`:
- `public Set<Continent> getContinents()` — returns `this.continents`

- [ ] **Step 2: Add any missing accessors as simple one-line getters**

For each missing accessor, add it to the appropriate class. Example:

```java
// Player.java
public Game getGame() { return this.game; }
public Set<City> getUnownedCities() { return this.unownedCities; }

// Unit.java (delegate to life if needed)
public int getHits() { return this.life.getHits(); }
public int getMovesLeft() { return this.life.movesLeft(); }
public int getFuel() { return this.life.getFuel(); }
public List<Unit> getCarrying() { return this.carries; }
public Unit getTransport() { return this.onboard; }

// Board.java
public Set<Continent> getContinents() { return this.continents; }
```

- [ ] **Step 3: Run the full test suite**

Run: `mvn clean test -Dsurefire.useFile=false`
Expected: All tests PASS (existing + new).

- [ ] **Step 4: Commit**

```bash
git add -A src/main/java/com/developingstorm/games/sad/
git commit -m "feat(llm): add missing accessors for LLM state serialization"
```

---

## Summary

| Task | What it builds | Test count |
|------|---------------|------------|
| 1 | Serializer skeleton + header + cities | 2 |
| 2 | Units awaiting orders with local context | 1 |
| 3 | Units on orders + enemies + enemy cities | 3 |
| 4 | Strategic summary (continent classification) | 1 |
| 5 | Order parser (LLM text -> Order objects) | 5 |
| 6 | System prompt | 3 |
| 7 | LLMBrain (IBrain implementation) | 3 |
| 8 | Anthropic API client | 2 |
| 9 | HeadlessGameRunner integration | 1 |
| 10 | Missing accessors + full suite verification | 0 (existing) |

After completing all tasks, you can run an LLM vs AI game with:

```java
HeadlessGameRunner runner = HeadlessGameRunner.withLLMPlayer(
    map, ctx, 200, System.getenv("ANTHROPIC_API_KEY"), "claude-sonnet-4-20250514"
);
GameResult result = runner.run();
```
