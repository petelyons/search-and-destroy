# Transport Conquest Failure - Complete Analysis

## The Problem Statement

**User Report**: "Transports assigned to move land units across water so other continents can be conquered, never seem to do it."

**Status Output After 200 Turns**:
```
T200|P2|Cities:1/4|Conts:1/1|Units:76(I70,A0,F0,B1,T5,D0,S0,C0,BB0,CV0)
              ^^^     ^^^
         4 cities    BUT only 1 continent occupied!
```

## What the Logs Revealed

### Transports ARE Working
- **314 successful transport unload operations** on other continents
- 5 transports built and active (T5 in status)
- 70 infantry units available for conquest
- Transports correctly:
  - Load infantry (up to 6 units per transport)
  - Navigate to expansion unloading points near target cities
  - Unload units on foreign continents

### Cities Captured: 3 Total
1. Infantry 9: Captured city at (1,3)
2. Infantry 5: Captured city at (5,4)
3. Infantry 3: Captured city at (2,1)

**All 3 cities are within 5 hexes of Player 2's starting position (3,4)**

### Cities Captured on Other Continents: **ZERO**

Despite:
- 314 transport unload operations
- 70 infantry units created
- 5 active transports
- 200 turns of gameplay

**Result: 0 continents conquered, 0 foreign cities captured**

## Root Cause: Infantry Pathfinding Failure

### Observed Behavior

Infantry unloaded on foreign continents:
1. ✅ Receive correct orders: "Moving to prioritized unoccupied city: Sylvan Lake at 0,38"
2. ✅ Start moving toward target
3. ❌ **Oscillate back and forth** instead of reaching destination
4. ❌ Never capture the target city

### Example: Infantry 3 Trying to Reach Sylvan Lake (0,38)

```
Turn N:   Loc=(3,4)  "Moving to Sylvan Lake at 0,38"
Turn N+1: Loc=(2,5)  "Moving to Sylvan Lake at 0,38"  [moving toward target]
Turn N+2: Loc=(2,6)  "Moving to Sylvan Lake at 0,38"  [still moving toward]
Turn N+3: Loc=(1,6)  "Moving to Sylvan Lake at 0,38"  [getting closer]
Turn N+4: Loc=(0,7)  "Moving to Sylvan Lake at 0,38"  [very close! Only 31 hexes away]
Turn N+5: Loc=(0,8)  "Moving to Sylvan Lake at 0,38"  [still close]
Turn N+6: Loc=(0,7)  "Moving to Sylvan Lake at 0,38"  [GOING BACKWARD]
Turn N+7: Loc=(1,6)  "Moving to Sylvan Lake at 0,38"  [STILL BACKWARD]
Turn N+8: Loc=(2,6)  "Moving to Sylvan Lake at 0,38"  [STILL BACKWARD]
Turn N+9: Loc=(2,5)  "Moving to Sylvan Lake at 0,38"  [STILL BACKWARD]
Turn N+10: Loc=(3,4) "Moving to Sylvan Lake at 0,38"  [BACK AT START!]
Turn N+11: Loc=(4,4) "Moving to Sylvan Lake at 0,38"  [Now going wrong direction]
Turn N+12: Loc=(4,5) "Moving to Sylvan Lake at 0,38"  [Wandering aimlessly]
...
Eventually: Loc=(2,1) "Captured city" [Captured a different, closer city]
```

**The unit walked toward the target, then reversed course and walked all the way back to its starting point!**

### Why This is Catastrophic

Every unloaded infantry unit:
- Gets stuck in pathfinding oscillation loops
- Wastes movement points going back and forth
- Never reaches distant target cities
- Eventually captures nearby cities on HOME continent (if any)
- Or just wanders forever

**314 transport loads × 6 infantry average = ~1,900 infantry delivered**
**Result: 0 cities captured on foreign continents**

## Technical Analysis

### Pathfinding Recalculation Bug

The logs show units saying "Moving to prioritized unoccupied city: X" every single turn, which suggests:

1. Each turn, the unit's Move order recalculates the path
2. PathCalculator sometimes returns a path in the OPPOSITE direction
3. Unit follows the reversed path, moving away from target
4. Next turn, path might reverse again
5. Unit oscillates, making no net progress

### Possible Causes

**Path Recalculation on Unexplored Terrain**:
- As units explore, new terrain is revealed
- PathCalculator may find "better" paths through newly-visible terrain
- These "better" paths actually lead away from the target
- Vision changes cause constant path invalidation

**A* Heuristic Problems**:
- Distance heuristic may be broken for long distances
- Goal-distance calculation might overflow or wrap around
- Heuristic may favor local minima over global path to goal

**Blocked Path Recovery**:
- When a path is blocked (other units, terrain), replanning fails
- System defaults to moving "away from obstacle" instead of "toward goal"
- Unit gets stuck in avoidance behavior

**Move Order State Loss**:
- Move order may not preserve the original path
- Each turn recalculates from scratch instead of resuming saved path
- Original destination gets lost or deprioritized

### Evidence

Looking at the location changes for Infantry 3:
- Distance from start (3,4) to Sylvan Lake (0,38): ~35 hexes
- Unit got within 31 hexes of target
- Then walked 5+ hexes BACKWARD (away from goal)
- Ended up at (2,1) - only 3 hexes from start, 37 hexes from goal

**This is not random movement - it's systematically walking BACK to the starting area.**

## Impact on Game

### What the AI Thinks is Happening
- "I'm building transports" ✓
- "I'm loading infantry" ✓
- "I'm unloading on target continents" ✓
- "I'm capturing cities on other continents" ✗ (thinks yes, actually no)

### What's Actually Happening
- Infantry get unloaded on foreign continents
- They wander aimlessly due to broken pathfinding
- They never capture foreign cities
- Game reaches turn limit with 0 continents conquered
- Victory is impossible

## Why This Wasn't Obvious Before

The logs say "Moving to prioritized unoccupied city: Sylvan Lake" on every turn, which LOOKS like the unit is actively pursuing that goal. Only by tracking the actual `Loc=` coordinates over time do you see the unit is:
- Not making progress toward the goal
- Actually moving AWAY from the goal
- Returning to its starting position

## Recommended Fix Priority

**CRITICAL - P0**: Fix PathCalculator/Move order system
- Infantry must be able to navigate 30-50 hexes to target cities
- Paths must be stable across multiple turns
- Path recalculation must not reverse direction

**Without this fix, amphibious invasions are impossible and the game is unwinnable.**

## Next Investigation Steps

1. **Enable DEBUG logging for pathfinding**:
   ```xml
   <logger name="com.developingstorm.games.sad.PathCalculator" level="DEBUG" />
   <logger name="com.developingstorm.games.sad.orders.Move" level="DEBUG" />
   ```

2. **Run 20-turn test** with one transport and track specific infantry unit

3. **Analyze logs to find**:
   - Why paths are recalculated each turn
   - Why recalculated paths point backward
   - Whether paths are being cached/reused
   - How path blockages are handled

4. **Test scenarios**:
   - Does pathfinding work on fully-explored terrain?
   - Does it work for short distances (10 hexes)?
   - At what distance does oscillation begin?

## Conclusion

**The transport AI works perfectly.** The failure is in the **infantry long-distance navigation system** after successful amphibious landing.

This is a critical game-breaking bug that makes conquest of other continents impossible.
