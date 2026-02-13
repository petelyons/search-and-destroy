# Logging Configuration Setup

The project now has two separate logging configurations:

## 1. Normal Gameplay Logging (`logback.xml`)

**Purpose**: Used during normal gameplay (JavaFX UI, Swing UI)

**Output**: Logs to **stdout** (console)

**Configuration**:
- Most loggers set to WARN level to reduce noise
- Game core events at INFO level
- AI brain loggers at WARN (since they're noisy during AI turns)
- Optional file appender to `SaD.log` (commented out by default)

**When it's used**: 
- Automatically loaded when running the JavaFX application
- Default configuration for normal gameplay

**To enable file logging during gameplay**:
Uncomment this line in `src/main/resources/logback.xml`:
```xml
<!-- <appender-ref ref="FILE" /> -->
```

## 2. AI Testing Logging (`logback-test.xml`)

**Purpose**: Used for AI vs AI testing and debugging

**Output**: Logs to **file** (`game_test.log`)

**Configuration**:
- AI brain loggers at INFO level for detailed AI behavior tracking
- Movement/pathing at WARN to reduce noise
- No console output (only file output)
- File is overwritten on each test run

**When it's used**:
- Explicitly loaded by `run_ai_test.sh` script
- Uses `-Dlogback.configurationFile=src/main/resources/logback-test.xml`

**Script**: `./run_ai_test.sh`

## How It Works

### Logback Configuration Precedence

Logback looks for configuration files in this order:
1. System property: `-Dlogback.configurationFile=<path>`
2. `logback-test.xml` in classpath (if in test scope)
3. `logback.xml` in classpath

Since `run_ai_test.sh` explicitly specifies `logback-test.xml`, AI testing uses that configuration. Normal gameplay uses the default `logback.xml`.

## Adjusting Log Levels

### For Normal Gameplay

Edit `src/main/resources/logback.xml`:

```xml
<!-- To see more AI decision logs during gameplay -->
<logger name="com.developingstorm.games.sad.brain" level="INFO" />

<!-- To see detailed movement logs -->
<logger name="com.developingstorm.games.sad.MovementResolver" level="DEBUG" />
```

### For AI Testing

Edit `src/main/resources/logback-test.xml` - levels are already set appropriately for AI debugging.

## Quick Reference

| Use Case | Configuration File | Output | Command |
|----------|-------------------|---------|---------|
| Normal Gameplay | `logback.xml` | stdout | Run JavaFX/Swing app normally |
| AI Testing | `logback-test.xml` | `game_test.log` | `./run_ai_test.sh` |

## Benefits

- **Clean console output** during normal gameplay
- **Detailed file logs** for AI testing without cluttering console
- **Independent configurations** - changes to AI test logging don't affect gameplay
- **Easy switching** - no code changes needed, just use appropriate script
