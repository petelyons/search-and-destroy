# Logging Configuration Summary

## What Was Done

The logging system has been updated to allow fine-grained control over what gets logged during AI testing.

### Key Changes

1. **Log Wrapper Enhancement** - The `Log` utility class now uses the context object's class to determine which logger to use:
   - `Log.info(transportUnit, "message")` → logs to `com.developingstorm.games.sad.types.Transport` logger
   - `Log.info(general, "message")` → logs to `com.developingstorm.games.sad.brain.General` logger
   - This allows filtering by actual class in logback.xml

2. **Game Class** - Now uses its own SLF4J logger for better control

3. **Logback Configuration** - Set up with sensible defaults for AI testing:
   - AI brain classes (TransportCaptain, General, etc.) → INFO level
   - Verbose classes (PathCalculator, Unit, etc.) → WARN level (suppressed)

## How the Logging Works Now

When you run `./run_ai_test.sh`:
- **Console/stdout**: Shows turn-by-turn status (Cities, Units, Exploration %)
- **game_test.log**: Contains detailed logs based on logback.xml configuration

## Checking the Logs

### Quick Check
```bash
# See which classes are logging most
grep "INFO" game_test.log | awk '{print $4}' | sort | uniq -c | sort -rn | head -20
```

### Finding Transport Logs
```bash
# Search for TransportCaptain activity
grep "Transport" game_test.log | head -50

# Or check the logger name specifically
grep "c.d.games.sad.types.Transport" game_test.log
grep "TransportCaptain" game_test.log
```

### Finding AI Brain Logs
```bash
# General AI planning
grep "General" game_test.log

# Battle planning  
grep "Battleplan" game_test.log

# Operations coordination
grep "OperationsCoordinator" game_test.log
```

## Adjusting Logging Levels

Edit `src/main/resources/logback.xml`:

### To see MORE transport detail:
```xml
<logger name="com.developingstorm.games.sad.brain.TransportCaptain" level="DEBUG" />
<logger name="com.developingstorm.games.sad.types.Transport" level="DEBUG" />
```

### To see movement/pathfinding for transports:
```xml
<logger name="com.developingstorm.games.sad.MovementResolver" level="INFO" />
<logger name="com.developingstorm.games.sad.PathCalculator" level="INFO" />
```

### To see ALL AI brain activity:
```xml
<logger name="com.developingstorm.games.sad.brain" level="DEBUG" />
```

### To reduce noise from Infantry:
```xml
<logger name="com.developingstorm.games.sad.types.Infantry" level="WARN" />
```

After changing logback.xml, recompile:
```bash
mvn compile
```

## Example Workflow: Debugging Transports

1. **Run a test** (200 turns to ensure transports are built):
   ```bash
   ./run_ai_test.sh single baseline/v1.0 profiles/medium --turn-limit 200
   ```

2. **Check if transports were built** (look at console output for "T" count):
   ```
   T200|P1|...|Units:50(I20,A5,F3,B2,T3,D2,S1,C1,BB0,CV0)/...
                                      ^^^
   ```

3. **Search logs for transport activity**:
   ```bash
   grep -i transport game_test.log | less
   ```

4. **If you need more detail**, edit logback.xml to enable DEBUG for TransportCaptain

5. **Re-run the test** to get detailed logs

## Understanding the Log Format

```
19:41:06.092 [thread] INFO c.d.games.sad.types.Infantry - <[Infantry 2: ...]>:[64]:Message
              ^time  ^thread ^level  ^logger-name          ^context          ^tid ^actual-message
```

- **Logger name**: The class that called Log.info/debug/etc
- **Context**: The object passed as first parameter (e.g., the unit)
- **Thread ID**: [64] - useful for tracking concurrent execution

## Current Configuration Status

✅ Logging is context-aware (uses actual class names)
✅ AI brain logs are enabled at INFO level  
✅ Verbose non-AI logs are suppressed (WARN only)
✅ All logs go to game_test.log
✅ Turn status goes to stdout

## Notes

- The old 107MB log file was from BEFORE these changes
- With the new configuration, logs should be much smaller and more focused
- If you don't see TransportCaptain logs, it likely means no transports were active in that test
- The map being used might not require naval operations (check if it has water/multiple continents)
