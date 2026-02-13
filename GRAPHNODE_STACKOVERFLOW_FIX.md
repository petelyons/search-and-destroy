# GraphNode StackOverflowError Fix

## Problem

When issuing an Explore order, the game crashes with a StackOverflowError:

```
java.lang.StackOverflowError
	at com.developingstorm.util.GraphNode.hashCode(GraphNode.java:44)
	at java.base/java.util.AbstractSet.hashCode(AbstractSet.java:124)
	at com.developingstorm.util.GraphNode.hashCode(GraphNode.java:46)
	at java.base/java.util.AbstractSet.hashCode(AbstractSet.java:124)
	at com.developingstorm.util.GraphNode.hashCode(GraphNode.java:46)
	...infinite recursion...
```

## Root Cause

The `GraphNode` class had a flawed implementation of `hashCode()` and `equals()` that included the `relatives` field (a Set of other GraphNodes) in the calculation.

### The Problematic Code

```java
@Override
public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((obj == null) ? 0 : this.obj.hashCode());
    result = prime * result + ((relatives == null) ? 0 : this.relatives.hashCode()); // ❌ PROBLEM
    result = prime * result + ((state == null) ? 0 : this.state.hashCode());
    return result;
}
```

### Why This Causes Stack Overflow

1. When you call `hashCode()` on a GraphNode, it calls `this.relatives.hashCode()`
2. `Set.hashCode()` iterates over all elements and calls `hashCode()` on each
3. Each relative GraphNode then calls its own `relatives.hashCode()`
4. If the graph has cycles (A → B → C → A), this creates infinite recursion
5. Stack overflows after ~5000-10000 recursive calls

### Graph Structure Example

```
Exploration creates a graph of reachable locations:

Location(6,4) ←→ Location(7,4)
    ↓                  ↓
Location(6,5) ←→ Location(7,5)

Each location is a GraphNode, and arrows represent "relatives".
This creates cycles, so hashCode() recurses infinitely.
```

## The Solution

Remove `relatives` from both `hashCode()` and `equals()`:

```java
@Override
public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((obj == null) ? 0 : this.obj.hashCode());
    // DO NOT include relatives - causes stack overflow with cycles
    result = prime * result + ((state == null) ? 0 : this.state.hashCode());
    return result;
}

@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    
    GraphNode<?, ?> other = (GraphNode<?, ?>) obj;
    
    // Compare only the content object and state
    if (this.obj == null) {
        if (other.obj != null) return false;
    } else if (!this.obj.equals(other.obj)) {
        return false;
    }
    
    // DO NOT compare relatives - causes stack overflow with cycles
    
    if (state == null) {
        if (other.state != null) return false;
    } else if (!this.state.equals(other.state)) {
        return false;
    }
    
    return true;
}
```

## Rationale

### Why It's Correct to Exclude Relatives

1. **Identity**: A graph node should be identified by what it **contains** (`obj`), not who it's **connected to** (`relatives`)

2. **Graph Theory**: In graph theory, two nodes are equal if they represent the same vertex, regardless of their edges

3. **Practical Use**: When searching a graph, you want to know "have I visited this location?" not "have I visited this location with these exact connections?"

4. **Hash Collections**: GraphNodes are stored in HashSets/HashMaps. The identity should be based on content, allowing efficient "have I seen this node?" checks

### Example

```java
Location loc = new Location(10, 20);
GraphNode<Location, ?> node1 = new GraphNode<>(graph, loc);
GraphNode<Location, ?> node2 = new GraphNode<>(graph, loc);

// node1 and node2 wrap the SAME location
// They should be considered equal, even if they have different relatives
// (e.g., discovered via different paths in the graph)

node1.equals(node2); // true - same content
node1.hashCode() == node2.hashCode(); // true - same hash
```

## Impact

### Before Fix
- ❌ Explore orders crash with StackOverflowError
- ❌ Any pathfinding creating cyclic graphs fails
- ❌ Game becomes unplayable for exploration

### After Fix
- ✅ Explore orders work correctly
- ✅ Pathfinding handles cyclic graphs safely
- ✅ HashCode/equals are consistent and correct
- ✅ No stack overflow regardless of graph structure

## Related Issues

This is a classic problem with cyclic data structures:

### Other Places to Watch For
- Any recursive methods on GraphNode
- toString() implementations (if they print relatives)
- Deep copy operations
- Serialization (if using default Java serialization)

### Best Practices
1. **Don't include cyclic references in hashCode/equals**
2. **Use object identity, not structure**
3. **Document the semantic meaning of equality**
4. **Test with cyclic graphs**

## Testing

To verify the fix works:

1. Start a new game
2. Select an infantry unit  
3. Issue an "Explore" order
4. Verify:
   - No StackOverflowError
   - Unit begins exploring
   - Game continues normally

## Files Changed

- `src/main/java/com/developingstorm/util/GraphNode.java`
  - Modified `hashCode()` to exclude relatives
  - Modified `equals()` to exclude relatives
  - Added documentation explaining the rationale
  - Fixed variable shadowing bug in equals() (was using `obj` parameter instead of `this.obj`)
