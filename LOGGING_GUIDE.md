# Logging Configuration Guide

This project uses SLF4J with Logback for logging. There are three logging configurations available:

## 1. Normal Gameplay Logging (Default)

**File:** `src/main/resources/logback.xml`

**Usage:** Default configuration when running the game normally

**Characteristics:**
- Log level: INFO
- Output: Console (stdout) + File (SaD.log)
- File mode: Append (keeps logs across sessions)
- Best for: Normal gameplay, minimal logging overhead

**What gets logged:**
- Important game state changes (turn start, combat, etc.)
- AI decisions at high level
- Errors and warnings
- Player actions

**Not logged:**
- Detailed pathfinding
- Individual unit movement steps
- Low-level game mechanics
- Debug messages

## 2. Verbose Gameplay Logging

**File:** `src/main/resources/logback-gameplay-verbose.xml`

**Usage:** Enable when debugging gameplay issues (fog of war, save/load, UI bugs, etc.)

**Characteristics:**
- Log level: DEBUG
- Output: Console (stdout) + File (SaD.log)
- File mode: Append
- Best for: Debugging gameplay issues, understanding game behavior

**What gets logged:**
- Everything from normal logging PLUS:
- Detailed pathfinding calculations
- Individual unit movements
- Vision/fog of war calculations
- Save/load details
- UI events and rendering
- All AI decision making details

### How to Enable:

**Option 1: Command line**
```bash
mvn exec:java -Dlogback.configurationFile=logback-gameplay-verbose.xml
```

**Option 2: In your IDE**
Add VM argument: `-Dlogback.configurationFile=logback-gameplay-verbose.xml`

**Option 3: Programmatically** (in main method before any logging)
```java
System.setProperty("logback.configurationFile", "logback-gameplay-verbose.xml");
```

## 3. AI Testing Logging

**File:** `src/main/resources/logback-test.xml`

**Usage:** Automatically used when running tests (Maven test phase)

**Characteristics:**
- Log level: Mixed (INFO for AI, WARN for game mechanics)
- Output: File only (game_test.log)
- File mode: Overwrite (fresh log each test run)
- Best for: Automated AI testing, performance testing

**What gets logged:**
- AI decisions and strategies
- High-level game state
- Errors and warnings
- System.out turn monitoring still visible

**Suppressed:**
- Movement details (reduces noise)
- Pathfinding spam
- Unit lifecycle details
- Most debug messages

## Log Files

### SaD.log
- Normal and verbose gameplay logs
- Located in project root directory
- Append mode (grows over time)
- Delete periodically to manage size

### game_test.log
- AI testing logs
- Located in project root directory
- Overwrite mode (fresh each test)

## Customizing Logging

To adjust logging for specific components, edit the appropriate logback XML file:

```xml
<!-- Set a specific logger to DEBUG -->
<logger name="com.developingstorm.games.sad.Player" level="DEBUG" />

<!-- Suppress noisy logger -->
<logger name="com.developingstorm.games.sad.PathCalculator" level="ERROR" />
```

### Common Loggers:

| Logger | Purpose |
|--------|---------|
| `com.developingstorm.games.sad.Player` | Player actions, visibility, explored areas |
| `com.developingstorm.games.sad.Game` | Game state, turn management |
| `com.developingstorm.games.sad.Unit` | Unit lifecycle, orders, movement |
| `com.developingstorm.games.sad.brain.*` | AI decision making |
| `com.developingstorm.games.sad.PathCalculator` | Pathfinding (very verbose) |
| `com.developingstorm.games.sad.CombatResolver` | Combat resolution |
| `com.developingstorm.games.sad.fx.*` | JavaFX UI events |
| `com.developingstorm.games.sad.persistence.*` | Save/load operations |

## Performance Considerations

- **Normal logging (INFO):** Minimal performance impact
- **Verbose logging (DEBUG):** 10-30% performance overhead
  - Pathfinding becomes slower due to logging
  - UI may feel slightly less responsive
  - Acceptable for debugging, not for performance testing

## Troubleshooting

### "No logs appearing"
- Check that SaD.log exists in project root
- Verify logback configuration is being loaded (should see logback startup messages)
- Check console output (STDOUT appender)

### "Too much logging / log file huge"
- Switch to normal logging (remove -D parameter)
- Delete SaD.log to start fresh
- Set specific noisy loggers to WARN or ERROR

### "Need logs for specific component only"
- Copy logback.xml to logback-custom.xml
- Set most loggers to WARN
- Set your target logger to DEBUG
- Use `-Dlogback.configurationFile=logback-custom.xml`

## Examples

### Debug fog of war issues:
```bash
# Enable verbose logging
mvn exec:java -Dlogback.configurationFile=logback-gameplay-verbose.xml

# Play game, reproduce issue
# Check SaD.log for messages from:
# - com.developingstorm.games.sad.Player (visibility calculations)
# - com.developingstorm.games.sad.persistence (save/load)
```

### Debug AI pathfinding:
Edit `logback-gameplay-verbose.xml` to set PathCalculator to TRACE (if available) or DEBUG:
```xml
<logger name="com.developingstorm.games.sad.PathCalculator" level="DEBUG" />
<logger name="com.developingstorm.games.astar.AStar" level="DEBUG" />
```

### Run AI tests with more detail:
Edit `logback-test.xml` and change AI loggers from INFO to DEBUG:
```xml
<logger name="com.developingstorm.games.sad.brain" level="DEBUG" />
```
