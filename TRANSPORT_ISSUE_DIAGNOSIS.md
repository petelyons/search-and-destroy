# Transport AI Issue - Root Cause Analysis

## Executive Summary

**Original Report**: "Transports never move units across water to conquer other continents"

**Reality**: Transports ARE working correctly (314 successful unload operations in 200 turns)

**Actual Problem**: Infantry units unloaded on new continents cannot reliably navigate to target cities due to pathfinding issues.

## Evidence from Logs

### Transports ARE Working
- 3,789 transport log entries
- 314 successful "Unload complete" operations
- Transports correctly:
  - Load infantry (carrying 6 units)
  - Navigate to "expansion unloading points"
  - Unload units near target continents

### Infantry Navigation is Broken

Example: Infantry 3 targeting Sylvan Lake at (0,38)

```
Movement path:
(3,4) → (2,5) → (2,6) → (1,6) → (0,7) → (0,8)  [getting closer]
(0,8) → (0,7) → (1,6) → (2,6) → (2,5) → (3,4)  [walking BACKWARD!]
(3,4) → (4,4) → (4,5) → (5,5) → (4,5) → (4,4)  [oscillating again]
...eventually captures different city at (2,1)
```

**Pattern**: Units move toward target, then reverse direction and return to starting point.

### Only 3 City Captures in 200 Turns

Despite 314 transport unload operations, only 3 cities were captured:
- Infantry 9 at (1,3)
- Infantry 5 at (5,4)  
- Infantry 3 at (2,1)

This is a **99% failure rate** for post-unload conquest.

## Root Cause

**Pathfinding fails for long-distance navigation on unexplored/partially-explored continents.**

When infantry are unloaded:
1. They receive orders to move to "prioritized unoccupied city" (correct)
2. PathCalculator attempts to find a path (often succeeds initially)
3. During multi-turn movement, something causes the path to be recalculated
4. New path calculation fails or returns a path in the OPPOSITE direction
5. Unit oscillates back and forth, wasting moves
6. Unit never reaches target city

## Why This Wasn't Obvious

The logs show units saying "Moving to prioritized unoccupied city: Sylvan Lake at 0,38 unowned" every turn, which LOOKS like they're trying to reach it. But tracking the actual `Loc=` coordinates reveals they're wandering aimlessly.

## Potential Causes

1. **Vision/Fog of War**: Pathfinding may recalculate when terrain is revealed, finding "better" paths that actually lead away
2. **Blocked Paths**: Terrain or other units blocking paths, causing rerouting that fails
3. **Path Invalidation**: Paths becoming invalid mid-execution without proper replanning
4. **Distance Limits**: PathCalculator may give up on very long paths
5. **Heuristic Issues**: A* heuristic may be broken for cross-continent navigation

## What's NOT the Problem

✅ Transports loading units - WORKING
✅ Transports navigating to unload points - WORKING  
✅ Transports unloading cargo - WORKING
✅ Infantry receiving move orders - WORKING
✅ Infantry starting to move toward targets - WORKING

❌ Infantry completing long-distance navigation - **BROKEN**

## Recommended Investigation

1. **Check PathCalculator logs** for units that oscillate:
   - Enable DEBUG logging for PathCalculator
   - Track why paths are being recalculated
   - Look for "path not found" or "blocked" messages

2. **Check Infantry order persistence**:
   - Are move orders being cancelled/reassigned mid-journey?
   - Is something resetting orders each turn?

3. **Test with short-distance targets**:
   - Do infantry successfully capture cities 10 hexes away?
   - At what distance does pathfinding break?

4. **Check exploration state**:
   - Are units trying to path through unexplored terrain?
   - Does pathfinding work better on fully-explored continents?

## Next Steps

To debug pathfinding issues, update logback.xml:

```xml
<!-- Enable detailed pathfinding logs -->
<logger name="com.developingstorm.games.sad.PathCalculator" level="DEBUG" />
<logger name="com.developingstorm.games.astar.AStar" level="DEBUG" />
<logger name="com.developingstorm.games.sad.orders.Move" level="DEBUG" />

<!-- Keep infantry logs but reduce detail -->
<logger name="com.developingstorm.games.sad.types.Infantry" level="INFO" />
```

Then run a shorter test (50 turns) and analyze why specific infantry units oscillate rather than reaching their targets.

## Conclusion

The transport AI is **not** broken. The issue is in the **infantry navigation system** after they've been successfully unloaded on new continents. Units receive correct orders but cannot execute long-distance movement reliably.
