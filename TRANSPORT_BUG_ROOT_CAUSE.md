# Transport Unload Bug - Root Cause Found

## The Problem

After 200 turns with 314 transport unload operations:
- **0 cities captured on foreign continents**
- **0 new continents occupied**
- **All 70 infantry remain stuck on transports**

## Root Cause

**Infantry units awakened during transport unloading do not have logic to disembark onto adjacent land hexes.**

### What Happens Now

1. ✅ Transport reaches foreign continent shore
2. ✅ Transport enters `UnLoad` mode
3. ✅ `UnitTurnState.beginTurn()` wakes up infantry: "Waking up for unloading from transport"
4. ❌ **Infantry AI (`InfantryCaptain.plan()`) has no disembark logic**
5. ❌ Infantry calls `occupyLandStrategy()` which tries to move directly to target city
6. ❌ Pathfinding calculates: `calculated travel path:land from 3,14 to 0,38`
   - Source (3,14) is the **transport's water location**
   - Trying to find a LAND path from WATER
7. ❌ **Error**: `No open nodes in A* search:42`
8. ❌ Infantry can't move, stays on transport
9. ❌ Next turn: repeat from step 3

### The Evidence

From logs:
```
Transport 26: UnLoad mode at Loc=(3,14) [water]
Infantry 15: On=Transport 26, Loc=(3,14) [same water location]
Infantry 15: "Moving to prioritized unoccupied city: Sylvan Lake at 0,38"
Path calculation: "calculated travel path:land from 3,14 to 0,38"
ERROR: "No open nodes in A* search:42"
[Infantry never moves, stays on transport]
```

## What Should Happen

When an infantry unit is:
- `isCarried()` = true
- On a transport in `UnLoad` mode
- `isAlongCoast()` = true (transport adjacent to land)
- Been awakened for unloading

The unit should:
1. **Find an adjacent open land hex** (next to the transport)
2. **Move to that hex** (disembark from transport)
3. **On subsequent turns**, move toward strategic targets (cities, etc.)

## Where to Fix

### Option 1: Add disembark logic to InfantryCaptain

In `InfantryCaptain.plan(Infantry u)`:

```java
@Override
public Order plan(Infantry u) {
    // NEW: Check if we need to disembark from transport
    if (u.isCarried() && needsToDisembark(u)) {
        return disembarkOrder(u);
    }
    
    // Check if assigned to an amphibious operation
    if (coordinator != null && coordinator.isAssigned(u)) {
        ...
```

Add methods:
```java
private boolean needsToDisembark(Infantry u) {
    return u.onboard != null && 
           u.onboard.isUnloadingMode() && 
           u.onboard.isAlongCoast();
}

private Order disembarkOrder(Infantry u) {
    // Find adjacent land hex with space
    Location transportLoc = u.getLocation();
    for (Location adjacent : transportLoc.getNeighbors()) {
        if (isValidDisembarkHex(adjacent, u)) {
            Log.info(u, "Disembarking from transport to " + adjacent);
            return u.newMove(adjacent);
        }
    }
    Log.warn(u, "Cannot disembark - no valid adjacent land hex");
    return u.newSkipTurn();
}

private boolean isValidDisembarkHex(Location loc, Infantry u) {
    // Must be on board
    if (!plan.getBoard().onBoard(loc)) return false;
    
    // Must be land (or city on coast)
    if (plan.getBoard().isWater(loc) && !plan.getBoard().hasCity(loc)) {
        return false;
    }
    
    // Must have space (less than 3 units already there)
    if (gen.getGame().unitsAtLocation(loc).size() >= 3) {
        return false;
    }
    
    return true;
}
```

### Option 2: Add to UnitCaptain base class

Put disembark logic in `UnitCaptain.occupyLandStrategy()`:

```java
protected Order occupyLandStrategy(Unit u) {
    // NEW: If on transport being unloaded, disembark first
    if (u.isCarried() && needsToDisembark(u)) {
        return disembarkOrder(u);
    }
    
    // First try to reach a city this turn
    Order order = occupyUnownedCity(u);
    ...
```

This handles all unit types (Infantry, Armor) not just Infantry.

### Option 3: Make it part of the movement system

When a unit calls `u.newMove(destination)` and `u.isCarried()`, the movement resolver could automatically:
1. Check if unit needs to disembark first
2. Find adjacent land hex
3. Move there instead of trying to path from water

## Recommendation

**Option 2** (UnitCaptain base class) is best because:
- Handles all land unit types (Infantry, Armor)
- Centralized logic
- Falls back to normal strategy after disembarking
- Units will automatically pursue strategic targets after getting off transport

## Expected Behavior After Fix

Turn 1:
- Transport at (3,14) in UnLoad mode
- Infantry 15 awakened, location (3,14) [on transport]
- InfantryCaptain recognizes need to disembark
- Finds adjacent land hex at (3,15)
- Moves to (3,15) [now OFF transport, on land]

Turn 2:
- Infantry 15 at (3,15) [on land]
- No longer carried
- InfantryCaptain: "Moving to prioritized unoccupied city: Sylvan Lake at 0,38"
- Path calculated from (3,15) to (0,38) - LAND to LAND ✓
- Infantry moves toward city

Turn N:
- Infantry reaches and captures Sylvan Lake
- **First foreign city conquered! ✓**

## Testing After Fix

Run same 200-turn test and check:
- Infantry cargo count decreases after unload
- Infantry appear at locations OFF the transport (not water hexes)
- Infantry successfully capture cities on foreign continents
- Status shows `Conts:1/2` or higher (multiple continents occupied)
