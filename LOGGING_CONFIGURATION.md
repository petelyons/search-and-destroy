# Logging Configuration for AI Testing

## Overview

The game now uses SLF4J loggers with Logback configuration for better control over logging output during AI testing.

## Changes Made

### 1. Game Class Logger
The `Game` class now uses its own SLF4J logger instance instead of the legacy `Log` wrapper:

```java
private static final Logger logger = LoggerFactory.getLogger(Game.class);
```

This allows fine-grained control over Game-specific logging through the logback.xml configuration.

### 2. Log Wrapper Enhancement
The legacy `Log` wrapper class has been updated to use the context object's class for logger selection:

```java
// Before: All logs went through Log.class logger
// After: Logs use the context object's class
Logger logger = context != null 
    ? LoggerFactory.getLogger(context.getClass())
    : defaultLogger;
```

**This is crucial**: When code calls `Log.info(this, "message")`, the logger name is now based on the object's class (e.g., `TransportCaptain`), not `Log.class`. This means the logback.xml configuration can now filter logs by the actual class doing the logging, even when using the legacy Log wrapper.

### 3. Logback Configuration (src/main/resources/logback.xml)

The logging configuration has been updated with specific logger levels for different components:

#### AI Loggers (INFO level - verbose for debugging AI behavior)
- `com.developingstorm.games.sad.brain` - All AI brain components
- `com.developingstorm.games.sad.brain.TransportCaptain` - Transport AI (for debugging transport issues)
- `com.developingstorm.games.sad.brain.General` - Strategic planning
- `com.developingstorm.games.sad.brain.RobotBrain` - Core AI logic
- `com.developingstorm.games.sad.brain.Battleplan` - Tactical planning
- `com.developingstorm.games.sad.brain.OperationsCoordinator` - Multi-unit coordination

#### Game Core (INFO level - important game events)
- `com.developingstorm.games.sad.Game` - Main game loop and state

#### Verbose Components (WARN level - suppressed during normal AI testing)
- `com.developingstorm.games.sad.PathCalculator` - Path finding
- `com.developingstorm.games.sad.MovementResolver` - Movement resolution
- `com.developingstorm.games.astar` - A* algorithm
- `com.developingstorm.games.sad.CombatResolver` - Combat resolution
- `com.developingstorm.games.sad.Unit` - Individual unit operations
- `com.developingstorm.games.sad.UnitManager` - Unit management
- `com.developingstorm.games.sad.City` - City operations
- `com.developingstorm.games.sad.CityManager` - City management
- `com.developingstorm.games.sad.Board` - Board state
- `com.developingstorm.games.sad.VisionManager` - Fog of war
- `com.developingstorm.games.sad.orders` - Order execution

## Usage

### For AI Testing (Current Configuration)
The current configuration is optimized for AI testing:
- AI brain logs are visible at INFO level
- Game core events are visible at INFO level  
- Verbose non-AI components are suppressed (WARN level only)
- All logs go to `game_test.log`
- Turn-by-turn status goes to stdout (visible in terminal)

### Debugging Specific Issues

To debug transport AI issues specifically, you can temporarily increase logging:

1. Edit `src/main/resources/logback.xml`
2. Change relevant logger levels to DEBUG:

```xml
<!-- Example: Debug transport AI and movement -->
<logger name="com.developingstorm.games.sad.brain.TransportCaptain" level="DEBUG" />
<logger name="com.developingstorm.games.sad.MovementResolver" level="DEBUG" />
<logger name="com.developingstorm.games.sad.PathCalculator" level="INFO" />
```

3. Recompile: `mvn compile`
4. Run your test: `./run_ai_test.sh single <config1> <config2>`

### Debugging Pathfinding Issues

```xml
<logger name="com.developingstorm.games.sad.PathCalculator" level="DEBUG" />
<logger name="com.developingstorm.games.astar" level="DEBUG" />
```

### Debugging Combat Issues

```xml
<logger name="com.developingstorm.games.sad.CombatResolver" level="DEBUG" />
```

### Full Verbose Logging (not recommended for AI tests)

Set all loggers to DEBUG and the root logger to DEBUG:

```xml
<root level="DEBUG">
    <appender-ref ref="FILE" />
</root>
```

## Log Output

- **File**: All detailed logs → `game_test.log`
- **Console/stdout**: Turn-by-turn status (from `HeadlessGameRunner`)
- **Format**: `HH:mm:ss.SSS [thread] LEVEL logger - message`

## Benefits

1. **Focused debugging**: See only AI-related logs during testing
2. **Reduced noise**: Suppress verbose pathfinding/movement logs
3. **Easy customization**: Adjust individual logger levels as needed
4. **Better performance**: Less I/O overhead from reduced logging
5. **Easier analysis**: Cleaner logs make it easier to spot AI issues

## Example: Debugging Transport AI

When investigating why transports aren't moving units across water:

1. Set TransportCaptain to DEBUG level
2. Keep PathCalculator at WARN to reduce noise
3. Run test and check `game_test.log` for TransportCaptain logs
4. Look for patterns like:
   - Transport assignment decisions
   - Path calculations for transports
   - Cargo loading/unloading logic
   - Transport destination selection

The logs will show AI decision-making without overwhelming detail from path calculations.
