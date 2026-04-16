# Visibility and Detection Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the binary enemy visibility system with a two-ring detection system (detection range / identification range) plus a persistent `ContactMap` that tracks current contacts and ghost sightings. Update rendering, AI threat assessment, and captain pursuit to use the new system.

**Architecture:** Additive — the existing `Vision` enum and `visible[][]` terrain fog-of-war stay unchanged. A new `Contact` + `ContactMap` layer sits alongside them, populated from a new `Player.scanForContacts()` pass in `startNewTurn()`. Rendering in `MapCanvas` shifts from iterating live units-in-visible-hexes to iterating contacts. `ThreatMap` gains weighted ghost contributions. Four AI captains gain ghost-pursuit fallback.

**Tech Stack:** Java 17, existing game model classes, JUnit 5 (Jupiter), JavaFX for rendering. No new dependencies.

Full design: `docs/superpowers/specs/2026-04-16-visibility-and-detection-overhaul.md`.

---

## File Structure

| File | Responsibility |
|------|---------------|
| Modify: `src/main/java/com/developingstorm/games/sad/Type.java` | Add `detectionRange`, `identificationRange`, `spotterQuality` stats to the enum |
| Create: `src/main/java/com/developingstorm/games/sad/Contact.java` | Immutable-ish data class representing one enemy contact (current or ghost) |
| Create: `src/main/java/com/developingstorm/games/sad/ContactMap.java` | Per-player map of contacts keyed by enemy unit id; merge/decay logic |
| Modify: `src/main/java/com/developingstorm/games/sad/Player.java` | Replace `LastSeenInfo`/`lastSeenEnemies` with `ContactMap`; add `scanForContacts()` |
| Modify: `src/main/java/com/developingstorm/games/sad/fx/MapCanvas.java` | Render contacts (current + ghost, identified + unidentified) |
| Modify: `src/main/java/com/developingstorm/games/sad/fx/TerrainImages.java` | Add silhouette drawing helper (programmatic shapes, no new assets) |
| Modify: `src/main/java/com/developingstorm/games/sad/brain/ThreatMap.java` | Include ghost contacts at reduced threat weight |
| Modify: `src/main/java/com/developingstorm/games/sad/brain/DestroyerCaptain.java` | Pursue ghost contacts when no current target |
| Modify: `src/main/java/com/developingstorm/games/sad/brain/CruiserCaptain.java` | Pursue ghost contacts when no current target |
| Modify: `src/main/java/com/developingstorm/games/sad/brain/FighterCaptain.java` | Pursue ghost contacts when no current target |
| Modify: `src/main/java/com/developingstorm/games/sad/brain/BomberCaptain.java` | Pursue ghost contacts when no current target |
| Create: `src/test/java/com/developingstorm/games/sad/ContactTest.java` | Unit tests for `Contact` age and lifetime logic |
| Create: `src/test/java/com/developingstorm/games/sad/ContactMapTest.java` | Unit tests for merge/decay/priority |
| Create: `src/test/java/com/developingstorm/games/sad/TypeDetectionTest.java` | Sanity check for new Type stats |

Silhouette icons are drawn programmatically (shapes on the JavaFX canvas) rather than shipped as new GIF assets. This avoids asset generation and keeps the change self-contained.

All paths below abbreviate `src/main/java/com/developingstorm/games/sad/` to `...sad/` and `src/test/java/com/developingstorm/games/sad/` to `.../test/sad/`.

---

## Task 1: Add detection stats to `Type`

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/Type.java`
- Create: `src/test/java/com/developingstorm/games/sad/TypeDetectionTest.java`

Adds three new stats to the `Type` enum per section 1.1 of the spec, using the existing Builder pattern.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/developingstorm/games/sad/TypeDetectionTest.java`:

```java
package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TypeDetectionTest {

    @Test
    public void testInfantryDetectionStats() {
        assertEquals(1, Type.INFANTRY.getDetectionRange());
        assertEquals(1, Type.INFANTRY.getIdentificationRange());
        assertEquals(0, Type.INFANTRY.getSpotterQuality());
    }

    @Test
    public void testCruiserIsBestDetector() {
        assertEquals(4, Type.CRUISER.getDetectionRange());
        assertEquals(4, Type.CRUISER.getIdentificationRange());
        assertEquals(2, Type.CRUISER.getSpotterQuality());
    }

    @Test
    public void testDestroyerIdentifiesAtDetectionRange() {
        // Destroyers and cruisers always ID what they detect
        assertEquals(
            Type.DESTROYER.getDetectionRange(),
            Type.DESTROYER.getIdentificationRange()
        );
        assertEquals(1, Type.DESTROYER.getSpotterQuality());
    }

    @Test
    public void testTransportIsPoorDetector() {
        assertEquals(2, Type.TRANSPORT.getDetectionRange());
        assertEquals(1, Type.TRANSPORT.getIdentificationRange());
        assertEquals(0, Type.TRANSPORT.getSpotterQuality());
    }

    @Test
    public void testCargoDetectionStats() {
        assertEquals(2, Type.CARGO.getDetectionRange());
        assertEquals(2, Type.CARGO.getIdentificationRange());
        assertEquals(0, Type.CARGO.getSpotterQuality());
    }

    @Test
    public void testAirUnitsDetectFarIdentifyClose() {
        assertEquals(4, Type.FIGHTER.getDetectionRange());
        assertEquals(2, Type.FIGHTER.getIdentificationRange());
        assertEquals(4, Type.BOMBER.getDetectionRange());
        assertEquals(2, Type.BOMBER.getIdentificationRange());
    }

    @Test
    public void testSubmarineSpotterQuality() {
        assertEquals(2, Type.SUBMARINE.getDetectionRange());
        assertEquals(2, Type.SUBMARINE.getIdentificationRange());
        assertEquals(1, Type.SUBMARINE.getSpotterQuality());
    }

    @Test
    public void testCarrierAndBattleshipStats() {
        assertEquals(3, Type.CARRIER.getDetectionRange());
        assertEquals(1, Type.CARRIER.getIdentificationRange());
        assertEquals(0, Type.CARRIER.getSpotterQuality());
        assertEquals(3, Type.BATTLESHIP.getDetectionRange());
        assertEquals(2, Type.BATTLESHIP.getIdentificationRange());
        assertEquals(1, Type.BATTLESHIP.getSpotterQuality());
    }

    @Test
    public void testArmorDetectionStats() {
        assertEquals(1, Type.ARMOR.getDetectionRange());
        assertEquals(1, Type.ARMOR.getIdentificationRange());
        assertEquals(0, Type.ARMOR.getSpotterQuality());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=TypeDetectionTest`
Expected: compile failure — `getDetectionRange`, `getIdentificationRange`, `getSpotterQuality` do not exist on `Type`.

- [ ] **Step 3: Add fields, builder methods, and getters to `Type`**

In `src/main/java/com/developingstorm/games/sad/Type.java`:

After line 201 (`private final int weight;`), add three fields:

```java
    private final int detectionRange;
    private final int identificationRange;
    private final int spotterQuality;
```

In the constructor (around line 203), add after `this.weight = builder.weight;`:

```java
        this.detectionRange = builder.detectionRange;
        this.identificationRange = builder.identificationRange;
        this.spotterQuality = builder.spotterQuality;
```

In the `Builder` inner class, after `private int weight;` add:

```java
        private int detectionRange;
        private int identificationRange;
        private int spotterQuality;
```

And add three builder methods at the end of the Builder class (before the closing brace):

```java
        Builder detectionRange(int v) {
            this.detectionRange = v;
            return this;
        }

        Builder identificationRange(int v) {
            this.identificationRange = v;
            return this;
        }

        Builder spotterQuality(int v) {
            this.spotterQuality = v;
            return this;
        }
```

After the existing `getWeight()` getter (around line 363), add:

```java
    public int getDetectionRange() {
        return detectionRange;
    }

    public int getIdentificationRange() {
        return identificationRange;
    }

    public int getSpotterQuality() {
        return spotterQuality;
    }
```

- [ ] **Step 4: Populate the values on each enum constant**

For each enum constant in `Type.java`, append three chained builder calls before the closing paren. Use exactly these values from the spec table:

| Enum | detectionRange | identificationRange | spotterQuality |
|------|---|---|---|
| INFANTRY | 1 | 1 | 0 |
| ARMOR | 1 | 1 | 0 |
| FIGHTER | 4 | 2 | 1 |
| BOMBER | 4 | 2 | 1 |
| CARGO | 2 | 2 | 0 |
| DESTROYER | 3 | 3 | 1 |
| TRANSPORT | 2 | 1 | 0 |
| SUBMARINE | 2 | 2 | 1 |
| CRUISER | 4 | 4 | 2 |
| CARRIER | 3 | 1 | 0 |
| BATTLESHIP | 3 | 2 | 1 |

Example for INFANTRY (apply the same pattern — three calls at the end of the builder chain — to every constant):

```java
    INFANTRY(
        builder()
            .description("Infantry")
            .abr("I")
            .travel(Travel.LAND)
            .dist(1)
            .hits(2)
            .cost(5)
            .vision(Vision.SURFACE)
            .visionDistance(1)
            .maxFuelMultiplier(-1)
            .carryCount(0)
            .weight(1)
            .attack(1)
            .iconID(7)
            .detectionRange(1)
            .identificationRange(1)
            .spotterQuality(0)
    ),
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `mvn -q test -Dtest=TypeDetectionTest`
Expected: PASS (9 tests).

- [ ] **Step 6: Run the full suite to confirm no regressions**

Run: `mvn -q test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/Type.java \
        src/test/java/com/developingstorm/games/sad/TypeDetectionTest.java
git commit -m "feat(type): add detection, identification, and spotter quality stats"
```

---

## Task 2: Create `Contact` class

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/Contact.java`
- Create: `src/test/java/com/developingstorm/games/sad/ContactTest.java`

A `Contact` is one entry in a player's `ContactMap`. Section 1.2 of the spec. Fields are mutable (ContactMap merges multiple spottings into the same Contact per turn).

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/developingstorm/games/sad/ContactTest.java`:

```java
package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContactTest {

    @BeforeEach
    public void setup() {
        LocationMap.init(20, 20);
    }

    @Test
    public void testCurrentContactNotExpired() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, Type.DESTROYER, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ true, /* spotterQuality= */ 1
        );
        assertTrue(c.isCurrent(10));
        assertFalse(c.isExpired(10));
        assertEquals(0, c.age(10));
    }

    @Test
    public void testGhostOneTurnOld() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, null, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ false, /* spotterQuality= */ 0
        );
        // Base duration 2 + spotter quality 0 = maxAge 2
        assertEquals(2, c.maxAge());
        assertFalse(c.isCurrent(11));
        assertFalse(c.isExpired(11));
        assertEquals(1, c.age(11));
    }

    @Test
    public void testGhostExpiresAfterMaxAge() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, null, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ false, /* spotterQuality= */ 0
        );
        // maxAge=2. age 2 is still alive, age 3 is expired.
        assertFalse(c.isExpired(12));
        assertTrue(c.isExpired(13));
    }

    @Test
    public void testHighQualitySpotterExtendsGhostLife() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, Type.CRUISER, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ true, /* spotterQuality= */ 2
        );
        // Base 2 + quality 2 = maxAge 4
        assertEquals(4, c.maxAge());
        assertFalse(c.isExpired(14));
        assertTrue(c.isExpired(15));
    }

    @Test
    public void testRemainingLife() {
        Location loc = Location.get(5, 5);
        Contact c = new Contact(
            42L, loc, null, Travel.SEA, null,
            /* turnDetected= */ 10, /* identified= */ false, /* spotterQuality= */ 1
        );
        // maxAge 3. At turn 10 remaining=3, at turn 12 remaining=1.
        assertEquals(3, c.remainingLife(10));
        assertEquals(1, c.remainingLife(12));
        assertEquals(0, c.remainingLife(13));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=ContactTest`
Expected: compile failure — `Contact` class does not exist.

- [ ] **Step 3: Create `Contact.java`**

Create `src/main/java/com/developingstorm/games/sad/Contact.java`:

```java
package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;

/**
 * Represents what a player knows about a single enemy unit — either a live
 * sighting this turn (current) or a stale sighting from a prior turn (ghost).
 *
 * Contacts are owned and updated by {@link ContactMap}. Fields are mutable so
 * that multiple friendly spotters can merge their observations into one entry
 * per enemy unit per turn (see {@link ContactMap#updateContact}).
 */
public class Contact {

    /** Universal base ghost lifetime in turns; spotter quality adds to this. */
    public static final int BASE_GHOST_DURATION = 2;

    public final long unitId;

    public Location location;
    /** Null when {@code identified == false}. */
    public Type type;
    /** Travel class (LAND/SEA/AIR). Always known, even for unidentified contacts. */
    public final Travel travelType;
    public final Player owner;

    public int turnDetected;
    public boolean identified;
    /** Highest spotter quality that has touched this contact (0..2). */
    public int spotterQuality;

    public Contact(
        long unitId,
        Location location,
        Type type,
        Travel travelType,
        Player owner,
        int turnDetected,
        boolean identified,
        int spotterQuality
    ) {
        this.unitId = unitId;
        this.location = location;
        this.type = type;
        this.travelType = travelType;
        this.owner = owner;
        this.turnDetected = turnDetected;
        this.identified = identified;
        this.spotterQuality = spotterQuality;
    }

    public int age(int currentTurn) {
        return currentTurn - turnDetected;
    }

    public boolean isCurrent(int currentTurn) {
        return age(currentTurn) == 0;
    }

    /** Maximum age before the ghost is removed. */
    public int maxAge() {
        return BASE_GHOST_DURATION + spotterQuality;
    }

    public boolean isExpired(int currentTurn) {
        return age(currentTurn) > maxAge();
    }

    /** Turns remaining before expiry. Zero or negative means expired. */
    public int remainingLife(int currentTurn) {
        return maxAge() - age(currentTurn);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=ContactTest`
Expected: PASS (5 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/Contact.java \
        src/test/java/com/developingstorm/games/sad/ContactTest.java
git commit -m "feat(contact): add Contact data class for enemy sighting tracking"
```

---

## Task 3: Create `ContactMap` class

**Files:**
- Create: `src/main/java/com/developingstorm/games/sad/ContactMap.java`
- Create: `src/test/java/com/developingstorm/games/sad/ContactMapTest.java`

Core logic: merge spottings by unit id, decay/remove expired ghosts, and pick the best contact at a hex for rendering (section 1.3 and 1.6 of the spec).

To keep `ContactMap` testable in isolation, its `updateContact` accepts primitives (unit id, location, type, travel, owner) rather than a live `Unit` reference. The caller (`Player.scanForContacts`) extracts these from the `Unit`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/developingstorm/games/sad/ContactMapTest.java`:

```java
package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContactMapTest {

    private ContactMap map;

    @BeforeEach
    public void setup() {
        LocationMap.init(20, 20);
        map = new ContactMap();
    }

    @Test
    public void testUpdateInsertsNewContact() {
        Location loc = Location.get(5, 5);
        map.updateContact(
            /* unitId= */ 1L, loc, Type.DESTROYER, Travel.SEA, /* owner= */ null,
            /* identified= */ true, /* spotterQuality= */ 1, /* currentTurn= */ 5
        );
        Contact c = map.getContactAt(loc);
        assertNotNull(c);
        assertEquals(Type.DESTROYER, c.type);
        assertTrue(c.identified);
        assertEquals(1, c.spotterQuality);
    }

    @Test
    public void testUpdateUpgradesToIdentified() {
        Location loc = Location.get(5, 5);
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 0, 5);
        map.updateContact(1L, loc, Type.CRUISER, Travel.SEA, null, true, 2, 5);
        Contact c = map.getContactAt(loc);
        assertTrue(c.identified);
        assertEquals(Type.CRUISER, c.type);
        assertEquals(2, c.spotterQuality);
    }

    @Test
    public void testUpdateDoesNotDowngradeIdentified() {
        Location loc = Location.get(5, 5);
        // First: identified by a cruiser
        map.updateContact(1L, loc, Type.SUBMARINE, Travel.SEA, null, true, 2, 5);
        // Second: a distant fighter sees it unidentified
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 1, 5);
        Contact c = map.getContactAt(loc);
        assertTrue(c.identified);
        assertEquals(Type.SUBMARINE, c.type);
        // spotterQuality keeps the higher value seen (2, not overwritten by 1)
        assertEquals(2, c.spotterQuality);
    }

    @Test
    public void testUpdateKeepsHighestSpotterQuality() {
        Location loc = Location.get(5, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 0, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 2, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 1, 5);
        Contact c = map.getContactAt(loc);
        assertEquals(2, c.spotterQuality);
    }

    @Test
    public void testRefreshRemovesExpiredGhosts() {
        Location loc = Location.get(5, 5);
        // Low-quality ghost: maxAge 2
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 0, 5);

        // Age 2: still alive
        map.refreshContacts(7, id -> false);
        assertNotNull(map.getContactAt(loc));

        // Age 3: expired
        map.refreshContacts(8, id -> false);
        assertNull(map.getContactAt(loc));
    }

    @Test
    public void testRefreshRemovesContactsForDeadUnits() {
        Location loc = Location.get(5, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 1, 5);
        // Caller signals unit 1 is dead
        map.refreshContacts(5, id -> id == 1L);
        assertNull(map.getContactAt(loc));
    }

    @Test
    public void testGetContactAtPrefersIdentified() {
        Location loc = Location.get(5, 5);
        // Unit 1 unidentified sighting + Unit 2 identified sighting at same hex.
        // (Multiple enemy units at same hex is rare but possible — e.g. carrier
        // with fighter on board via different detection events.)
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 0, 5);
        map.updateContact(2L, loc, Type.DESTROYER, Travel.SEA, null, true, 1, 5);
        Contact c = map.getContactAt(loc);
        assertEquals(2L, c.unitId, "identified contact wins");
    }

    @Test
    public void testGetContactAtTiebreakByRemainingLife() {
        Location loc = Location.get(5, 5);
        // Two unidentified ghosts, different spotterQuality => different maxAge
        map.updateContact(1L, loc, null, Travel.SEA, null, false, 0, 5); // maxAge 2
        map.updateContact(2L, loc, null, Travel.SEA, null, false, 2, 5); // maxAge 4
        Contact c = map.getContactAt(loc);
        assertEquals(2L, c.unitId, "longer remaining life wins");
    }

    @Test
    public void testGetAllAndGetGhostsAndGetCurrent() {
        Location l1 = Location.get(1, 1);
        Location l2 = Location.get(2, 2);
        map.updateContact(1L, l1, Type.INFANTRY, Travel.LAND, null, true, 0, 10);
        map.updateContact(2L, l2, Type.DESTROYER, Travel.SEA, null, true, 1, 9);

        Collection<Contact> all = map.getAllContacts();
        assertEquals(2, all.size());

        Collection<Contact> current = map.getCurrentContacts(10);
        assertEquals(1, current.size());
        assertEquals(1L, current.iterator().next().unitId);

        Collection<Contact> ghosts = map.getGhostContacts(10);
        assertEquals(1, ghosts.size());
        assertEquals(2L, ghosts.iterator().next().unitId);
    }

    @Test
    public void testRemoveContact() {
        Location loc = Location.get(5, 5);
        map.updateContact(1L, loc, Type.DESTROYER, Travel.SEA, null, true, 1, 5);
        map.removeContact(1L);
        assertNull(map.getContactAt(loc));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=ContactMapTest`
Expected: compile failure — `ContactMap` class does not exist.

- [ ] **Step 3: Create `ContactMap.java`**

Create `src/main/java/com/developingstorm/games/sad/ContactMap.java`:

```java
package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;

/**
 * Per-player registry of enemy contacts (current sightings + stale ghosts).
 *
 * Keyed by enemy unit id so multiple spottings of the same enemy in one turn
 * merge into a single entry. {@link #updateContact} never downgrades an
 * identified contact to unidentified, and keeps the highest spotter quality
 * seen so far (controls ghost lifetime).
 *
 * {@link #refreshContacts} is expected to be called once per owning player's
 * turn (after {@code scanForContacts}) to drop expired ghosts and contacts
 * whose underlying enemy unit is dead.
 */
public class ContactMap {

    private final Map<Long, Contact> contacts = new HashMap<>();

    /**
     * Merges a sighting of enemy unit {@code unitId} at {@code location} into
     * the map. Upgrades identification (never downgrades). Keeps the highest
     * {@code spotterQuality} seen. Always snaps location and turnDetected to
     * the latest values.
     */
    public void updateContact(
        long unitId,
        Location location,
        Type type,
        Travel travelType,
        Player owner,
        boolean identified,
        int spotterQuality,
        int currentTurn
    ) {
        Contact existing = contacts.get(unitId);
        if (existing == null) {
            contacts.put(
                unitId,
                new Contact(
                    unitId,
                    location,
                    identified ? type : null,
                    travelType,
                    owner,
                    currentTurn,
                    identified,
                    spotterQuality
                )
            );
            return;
        }

        existing.location = location;
        existing.turnDetected = currentTurn;
        if (identified && !existing.identified) {
            existing.identified = true;
            existing.type = type;
        } else if (identified && existing.identified && existing.type == null) {
            // Defensive: previous identified entry missing type — fill in
            existing.type = type;
        }
        if (spotterQuality > existing.spotterQuality) {
            existing.spotterQuality = spotterQuality;
        }
    }

    /**
     * Removes expired ghosts and contacts whose unit is reported dead.
     *
     * @param currentTurn current game turn
     * @param isUnitDead predicate returning {@code true} if a given unit id
     *                   refers to a dead or removed unit
     */
    public void refreshContacts(int currentTurn, LongPredicate isUnitDead) {
        List<Long> toRemove = new ArrayList<>();
        for (Contact c : contacts.values()) {
            if (isUnitDead.test(c.unitId) || c.isExpired(currentTurn)) {
                toRemove.add(c.unitId);
            }
        }
        for (Long id : toRemove) {
            contacts.remove(id);
        }
    }

    /**
     * Returns the best contact at {@code location} for rendering purposes, or
     * null if none. Priority: identified beats unidentified; within the same
     * identification level, the contact with the longer remaining ghost life
     * wins.
     */
    public Contact getContactAt(Location location) {
        Contact best = null;
        for (Contact c : contacts.values()) {
            if (!location.equals(c.location)) continue;
            if (best == null) {
                best = c;
                continue;
            }
            if (c.identified && !best.identified) {
                best = c;
            } else if (c.identified == best.identified) {
                // Tie break: later turnDetected (= fresher) wins; then higher
                // spotterQuality (= longer max lifetime)
                if (c.turnDetected > best.turnDetected) {
                    best = c;
                } else if (
                    c.turnDetected == best.turnDetected
                    && c.spotterQuality > best.spotterQuality
                ) {
                    best = c;
                }
            }
        }
        return best;
    }

    public Collection<Contact> getAllContacts() {
        return Collections.unmodifiableCollection(contacts.values());
    }

    public Collection<Contact> getCurrentContacts(int currentTurn) {
        List<Contact> out = new ArrayList<>();
        for (Contact c : contacts.values()) {
            if (c.isCurrent(currentTurn)) out.add(c);
        }
        return out;
    }

    public Collection<Contact> getGhostContacts(int currentTurn) {
        List<Contact> out = new ArrayList<>();
        for (Contact c : contacts.values()) {
            if (!c.isCurrent(currentTurn)) out.add(c);
        }
        return out;
    }

    public void removeContact(long unitId) {
        contacts.remove(unitId);
    }

    public int size() {
        return contacts.size();
    }

    public void clear() {
        contacts.clear();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q test -Dtest=ContactMapTest`
Expected: PASS (10 tests).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/ContactMap.java \
        src/test/java/com/developingstorm/games/sad/ContactMapTest.java
git commit -m "feat(contact): add ContactMap with merge/decay/priority logic"
```

---

## Task 4: Replace `LastSeenInfo`/`lastSeenEnemies` with `ContactMap` in `Player`

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/Player.java`

This task swaps the old ghost-tracking scaffolding for the new `ContactMap`. It does NOT yet add the richer two-ring detection logic — `buildEnemyUnitList` continues to populate `enemyUnits` from currently visible hexes, and also pushes an identified contact into the new map for each. Task 5 replaces that loop with a proper radial scan.

- [ ] **Step 1: Remove the `LastSeenInfo` inner class and the `lastSeenEnemies` field**

In `src/main/java/com/developingstorm/games/sad/Player.java`, delete:

- Lines 24–48 (the Javadoc comment and `public static class LastSeenInfo { ... }` block).
- Line 73 (`private Map<Long, LastSeenInfo> lastSeenEnemies;`).
- Line 87 (`this.lastSeenEnemies = new HashMap<>();`) inside the constructor.
- The entire `getLastSeenEnemies()` method (around lines 810–816, including its Javadoc).
- The entire `removeLastSeenEnemy(long unitId)` method (around lines 818–824).
- The entire `cleanupLastSeenEnemies()` private method (around lines 826–870).

- [ ] **Step 2: Add the new `ContactMap` field and getters**

Under the other protected fields (after the `enemyActivity` declaration around line 60), add:

```java
    protected ContactMap contactMap;
```

In the constructor (around line 87 — where `lastSeenEnemies` was initialized), add:

```java
        this.contactMap = new ContactMap();
```

Add these public query methods near the other "query" accessors (e.g. just below `getKnownEnemies()` at line 418). Keep imports tidy — `java.util.Collection` is already imported.

```java
    public ContactMap getContactMap() {
        return contactMap;
    }

    public Collection<Contact> getAllContacts() {
        return contactMap.getAllContacts();
    }

    public Collection<Contact> getGhostContacts() {
        return contactMap.getGhostContacts(this.game.getTurn());
    }

    public Collection<Contact> getIdentifiedContacts() {
        java.util.List<Contact> out = new java.util.ArrayList<>();
        for (Contact c : contactMap.getAllContacts()) {
            if (c.identified) out.add(c);
        }
        return out;
    }
```

- [ ] **Step 3: Make `buildEnemyUnitList` populate `contactMap` instead of `lastSeenEnemies`**

Replace the body of `buildEnemyUnitList` (around lines 361–395) with:

```java
    private void buildEnemyUnitList() {
        this.enemyUnits.clear();

        int width = this.board.getWidth();
        int height = this.board.getHeight();
        int currentTurn = this.game.getTurn();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Location loc = Location.get(x, y);
                if (isExplored(loc)) {
                    Unit u = visibleUnit(loc);
                    if (u != null) {
                        Player p = u.getOwner();
                        if (p != this) {
                            this.enemyUnits.add(u);
                            // Placeholder: mark an identified contact at current
                            // visibility range. Task 5 replaces this with a proper
                            // radial scan using detectionRange/identificationRange.
                            this.contactMap.updateContact(
                                u.id,
                                loc,
                                u.getType(),
                                u.getTravel(),
                                p,
                                /* identified= */ true,
                                /* spotterQuality= */ 0,
                                currentTurn
                            );
                        }
                    }
                }
            }
        }

        // Decay old ghosts; drop contacts for units that have died.
        this.contactMap.refreshContacts(
            currentTurn,
            unitId -> {
                Unit u = this.game.getUnitById(unitId);
                return u == null || u.isDead();
            }
        );
    }
```

- [ ] **Step 4: Verify compilation and no broken call sites**

Run: `mvn -q compile`
Expected: compile error only if other files reference `LastSeenInfo`, `getLastSeenEnemies`, or `removeLastSeenEnemy`. Check:

Run: `grep -rn "LastSeenInfo\|getLastSeenEnemies\|removeLastSeenEnemy\|lastSeenEnemies" src/main src/test`
Expected: no hits. (The only remaining references live in `docs/` and `archive/`, which are not compiled.)

If there are unexpected main-source references, delete them — they are unreachable after this refactor.

- [ ] **Step 5: Run full test suite**

Run: `mvn -q test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/Player.java
git commit -m "refactor(player): replace LastSeenInfo with ContactMap"
```

---

## Task 5: Add `scanForContacts` — proper two-ring radial detection

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/Player.java`

Replaces the placeholder contact population from Task 4 with a full radial scan across all owned units and cities. Implements sections 2.2, 2.3, and 1.5 of the spec.

- [ ] **Step 1: Write the integration test**

Create `src/test/java/com/developingstorm/games/sad/DetectionIntegrationTest.java`:

```java
package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.brain.RobotBrain;
import java.util.Collection;
import org.junit.jupiter.api.Test;

/**
 * Integration test for two-ring detection. Uses a real Game with two AI
 * players, then manipulates unit positions directly to test detection at
 * specific ranges.
 *
 * Pattern mirrors other whole-game tests (see GameStateSerializerTest).
 */
public class DetectionIntegrationTest {

    private Game buildGame() {
        HexBoardMap map = HexBoardMap.load("war.map");
        HexBoardContext ctx = new HexBoardContext() {
            @Override public int getWidth() { return 20; }
            @Override public int getHeight() { return 20; }
        };
        Player[] players = new Player[2];
        Robot r1 = new Robot("A", 1); r1.setBrain(new RobotBrain(r1));
        Robot r2 = new Robot("B", 2); r2.setBrain(new RobotBrain(r2));
        players[0] = r1;
        players[1] = r2;
        UnitNames.autoAssignThemes(2);
        return new Game(players, map, ctx);
    }

    /**
     * Find a water-land coastal pair on the board so we can drop a destroyer
     * and an enemy submarine within range of each other.
     */
    private Location findWaterLocation(Game g) {
        for (int x = 0; x < g.getBoard().getWidth(); x++) {
            for (int y = 0; y < g.getBoard().getHeight(); y++) {
                Location loc = Location.get(x, y);
                BoardHex hex = g.getBoard().get(loc);
                if (hex != null && hex.getTerrainType() == 0) return loc;
            }
        }
        throw new IllegalStateException("No water hex found");
    }

    @Test
    public void testDestroyerDetectsSubmarineAtRange3() {
        Game g = buildGame();
        Player a = g.getPlayers()[0];
        Player b = g.getPlayers()[1];

        Location waterA = findWaterLocation(g);
        // Find a water hex 3 away on the same board
        Location waterB = null;
        for (int x = 0; x < g.getBoard().getWidth() && waterB == null; x++) {
            for (int y = 0; y < g.getBoard().getHeight(); y++) {
                Location loc = Location.get(x, y);
                BoardHex hex = g.getBoard().get(loc);
                if (hex != null && hex.getTerrainType() == 0
                    && waterA.distance(loc) == 3) {
                    waterB = loc;
                    break;
                }
            }
        }
        assertNotNull(waterB, "need a water hex exactly 3 away");

        Unit destroyer = g.createUnit(Type.DESTROYER, a, waterA);
        Unit sub = g.createUnit(Type.SUBMARINE, b, waterB);

        a.startNewTurn();

        // Destroyer has COMPLETE vision and detectionRange=3
        // Submarine should be detected AND identified (submarine auto-ID via COMPLETE)
        Contact c = a.getContactMap().getContactAt(waterB);
        assertNotNull(c, "destroyer should detect sub at distance 3");
        assertTrue(c.identified, "sub should be auto-identified via COMPLETE vision");
        assertEquals(Type.SUBMARINE, c.type);
    }

    @Test
    public void testFighterDetectsFarIdentifiesClose() {
        Game g = buildGame();
        Player a = g.getPlayers()[0];
        Player b = g.getPlayers()[1];

        // Pick two locations exactly 3 apart
        Location fighterLoc = Location.get(5, 5);
        // Find a location exactly 3 away
        Location targetLoc = null;
        for (int x = 0; x < g.getBoard().getWidth() && targetLoc == null; x++) {
            for (int y = 0; y < g.getBoard().getHeight(); y++) {
                Location loc = Location.get(x, y);
                if (loc != null && fighterLoc.distance(loc) == 3) {
                    targetLoc = loc;
                    break;
                }
            }
        }
        assertNotNull(targetLoc);

        Unit fighter = g.createUnit(Type.FIGHTER, a, fighterLoc);
        Unit enemy = g.createUnit(Type.INFANTRY, b, targetLoc);

        a.startNewTurn();

        // Fighter: detectionRange=4, identificationRange=2. At distance 3 => detected but NOT identified.
        Contact c = a.getContactMap().getContactAt(targetLoc);
        assertNotNull(c, "fighter should detect at distance 3 (within 4)");
        assertFalse(c.identified, "at distance 3 (> 2) fighter should NOT identify");
        assertEquals(Travel.LAND, c.travelType);
    }

    @Test
    public void testGhostSurvivesAfterEnemyLeavesVisibility() {
        Game g = buildGame();
        Player a = g.getPlayers()[0];
        Player b = g.getPlayers()[1];

        Location waterA = findWaterLocation(g);
        Location waterB = null;
        for (int x = 0; x < g.getBoard().getWidth() && waterB == null; x++) {
            for (int y = 0; y < g.getBoard().getHeight(); y++) {
                Location loc = Location.get(x, y);
                BoardHex hex = g.getBoard().get(loc);
                if (hex != null && hex.getTerrainType() == 0
                    && waterA.distance(loc) == 2) {
                    waterB = loc;
                }
            }
        }
        assertNotNull(waterB);

        Unit destroyer = g.createUnit(Type.DESTROYER, a, waterA);
        Unit enemySub = g.createUnit(Type.SUBMARINE, b, waterB);

        a.startNewTurn();
        assertNotNull(a.getContactMap().getContactAt(waterB));

        // Remove enemy sub (simulates it moving far away) and advance turn
        g.killUnit(enemySub);
        // Run refresh a turn later — dead unit contact should be dropped
        a.startNewTurn();
        Contact gone = a.getContactMap().getContactAt(waterB);
        assertNull(gone, "dead unit contact should be removed");
    }
}
```

Note: `Game.createUnit(Type, Player, Location)` and `Game.killUnit(Unit)` exist on the real `Game` class — if a signature is slightly different in the current codebase, adapt the test helper to match. Grep `Game.java` for `createUnit` and adjust.

- [ ] **Step 2: Run to verify it fails**

Run: `mvn -q test -Dtest=DetectionIntegrationTest`
Expected: FAIL. With the Task 4 placeholder, the destroyer at distance 3 will only detect the sub if the sub is in a directly-visible hex — which for submarines requires WATER/COMPLETE vision. This test may fail by returning unidentified instead of identified, or by not detecting at range 3 at all, depending on the fallback. We want this to fail so the new scan logic can drive the fix.

- [ ] **Step 3: Implement `scanForContacts` in `Player.java`**

Replace `buildEnemyUnitList` entirely (the version from Task 4) with a new `scanForContacts` method. Also update the call site inside `startNewTurn` (around line 1087). Put this below `getKnownEnemies` (near line 418):

```java
    /**
     * Two-ring detection pass. Populates {@code contactMap} with every enemy
     * unit that any owned unit or city can detect, marking it identified if
     * within that spotter's identification range (or auto-identifying
     * submarines under WATER/COMPLETE vision). Also refreshes/decays ghosts
     * and re-populates the legacy {@code enemyUnits} set from current
     * contacts only (for backwards compatibility with existing callers).
     */
    private void scanForContacts() {
        this.enemyUnits.clear();
        int currentTurn = this.game.getTurn();

        for (Player other : this.game.getPlayers()) {
            if (other == this) continue;
            for (Unit enemy : other.getUnits()) {
                if (enemy.isDead()) continue;
                Location eLoc = enemy.getLocation();

                boolean detectedThisTurn = false;
                boolean bestIdentified = false;
                int bestSpotterQuality = 0;

                // Owned units as spotters
                for (Unit u : this.units) {
                    Type ut = u.getType();
                    int dist = u.getLocation().distance(eLoc);
                    if (dist > ut.getDetectionRange()) continue;
                    if (!enemy.isVisible(ut.getVision())) continue;

                    detectedThisTurn = true;
                    boolean identified = dist <= ut.getIdentificationRange();
                    // Submarines under WATER or COMPLETE vision are auto-ID'd
                    if (enemy.getType() == Type.SUBMARINE
                        && (ut.getVision() == Vision.WATER
                            || ut.getVision() == Vision.COMPLETE)) {
                        identified = true;
                    }
                    if (identified) bestIdentified = true;
                    if (ut.getSpotterQuality() > bestSpotterQuality) {
                        bestSpotterQuality = ut.getSpotterQuality();
                    }
                }

                // Owned cities as spotters (detectionRange=3, idRange=3, quality=2, COMPLETE vision)
                if (!bestIdentified || bestSpotterQuality < 2) {
                    for (City c : this.cities) {
                        int dist = c.getLocation().distance(eLoc);
                        if (dist > 3) continue;
                        // Cities have COMPLETE vision — everything is visible
                        detectedThisTurn = true;
                        bestIdentified = true;
                        if (bestSpotterQuality < 2) bestSpotterQuality = 2;
                    }
                }

                if (detectedThisTurn) {
                    this.enemyUnits.add(enemy);
                    this.contactMap.updateContact(
                        enemy.id,
                        eLoc,
                        enemy.getType(),
                        enemy.getTravel(),
                        other,
                        bestIdentified,
                        bestSpotterQuality,
                        currentTurn
                    );
                }
            }
        }

        // Decay stale contacts; drop dead units
        this.contactMap.refreshContacts(
            currentTurn,
            unitId -> {
                Unit u = this.game.getUnitById(unitId);
                return u == null || u.isDead();
            }
        );
    }
```

Now delete the old `buildEnemyUnitList` body (the one added in Task 4) — its responsibilities are fully replaced by `scanForContacts`.

In `startNewTurn` (around line 1087), change `buildEnemyUnitList();` to `scanForContacts();`.

- [ ] **Step 4: Run the integration test**

Run: `mvn -q test -Dtest=DetectionIntegrationTest`
Expected: PASS (3 tests).

- [ ] **Step 5: Run the full test suite**

Run: `mvn -q test`
Expected: PASS. (If an existing test depended on `buildEnemyUnitList` populating `enemyUnits` from vision rather than detection, flag it — the behavior is intentionally different now; adjust the test to use the real radial rules.)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/Player.java \
        src/test/java/com/developingstorm/games/sad/DetectionIntegrationTest.java
git commit -m "feat(detection): add two-ring radial scan with ghost decay"
```

---

## Task 6: Silhouette drawing helper in `TerrainImages`

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/fx/TerrainImages.java`

Silhouettes are drawn programmatically (no new GIF assets) per section 4.2 of the spec, parameterized by travel class. This keeps the change self-contained.

- [ ] **Step 1: Add `drawSilhouette` helper**

In `src/main/java/com/developingstorm/games/sad/fx/TerrainImages.java`, add these imports at the top if not already present:

```java
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import com.developingstorm.games.sad.Travel;
```

Then add this method to the class (right above the final closing brace):

```java
    /**
     * Draws a travel-class silhouette (unknown enemy contact) centered on
     * {@code (cx, cy)} with size {@code size}. Uses {@code color} for the fill
     * (typically the enemy player's color).
     */
    public void drawSilhouette(
        GraphicsContext gc,
        Travel travel,
        double cx,
        double cy,
        double size,
        Color color
    ) {
        double half = size / 2.0;
        gc.setFill(color);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.0);

        switch (travel) {
            case LAND: {
                // Upward-pointing triangle
                double[] xs = { cx, cx - half, cx + half };
                double[] ys = { cy - half, cy + half, cy + half };
                gc.fillPolygon(xs, ys, 3);
                gc.strokePolygon(xs, ys, 3);
                break;
            }
            case SEA: {
                // Ship hull: trapezoid (wide bottom, narrow top)
                double[] xs = {
                    cx - half, cx - half * 0.6, cx + half * 0.6, cx + half
                };
                double[] ys = {
                    cy + half * 0.3, cy - half * 0.4, cy - half * 0.4, cy + half * 0.3
                };
                gc.fillPolygon(xs, ys, 4);
                gc.strokePolygon(xs, ys, 4);
                break;
            }
            case AIR: {
                // Diamond / plan-view aircraft
                double[] xs = { cx, cx + half, cx, cx - half };
                double[] ys = { cy - half, cy, cy + half, cy };
                gc.fillPolygon(xs, ys, 4);
                gc.strokePolygon(xs, ys, 4);
                break;
            }
            default:
                return;
        }

        // Overlay "?" mark
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        javafx.scene.text.Font font = javafx.scene.text.Font.font(
            "Arial",
            javafx.scene.text.FontWeight.BOLD,
            Math.max(10, size * 0.55)
        );
        gc.setFont(font);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.strokeText("?", cx, cy);
        gc.fillText("?", cx, cy);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.setTextBaseline(javafx.geometry.VPos.BASELINE);
    }
```

- [ ] **Step 2: Verify compilation**

Run: `mvn -q compile`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/fx/TerrainImages.java
git commit -m "feat(fx): add programmatic silhouette drawing helper"
```

---

## Task 7: Ghost rendering in `MapCanvas`

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/fx/MapCanvas.java`

Rewrites the enemy unit drawing pass to read from `ContactMap`. Own units are drawn exactly as before. Four visual states per section 4.1: (current, identified) → normal icon; (current, unidentified) → silhouette at full opacity; (ghost, identified) → faded icon; (ghost, unidentified) → faded silhouette.

- [ ] **Step 1: Split the draw loop — own units vs enemy contacts**

In `src/main/java/com/developingstorm/games/sad/fx/MapCanvas.java`, find the unit draw section in `drawMap()` (starts around line 331 with the comment `// Draw units (only in explored areas)`).

Replace the block from line 331 through line 445 (end of the outer `for` over `unitsByLocation.entrySet()`) with this new logic:

```java
        // 1) Draw own units (no fog of war for own units).
        Player humanPlayer = query.getHumanPlayer();
        java.util.Map<Location, java.util.List<Unit>> ownUnitsByLocation =
            new java.util.HashMap<>();
        for (Unit unit : query.getAllUnits()) {
            if (unit.isCarried() && unit.inSentryMode()) continue;
            if (humanPlayer != null && unit.getOwner() != humanPlayer) continue;
            ownUnitsByLocation
                .computeIfAbsent(unit.getLocation(), k -> new java.util.ArrayList<>())
                .add(unit);
        }

        Unit selectedUnit = query.getSelectedUnit();
        for (java.util.Map.Entry<Location, java.util.List<Unit>> entry :
                ownUnitsByLocation.entrySet()) {
            Location loc = entry.getKey();
            java.util.List<Unit> unitsAtLoc = entry.getValue();
            if (unitsAtLoc.isEmpty()) continue;

            Unit unitToDraw = null;
            if (selectedUnit != null && unitsAtLoc.contains(selectedUnit)) {
                unitToDraw = selectedUnit;
            } else {
                for (Unit u : unitsAtLoc) {
                    if (u.life().hasMoves() && !u.inSentryMode()) {
                        unitToDraw = u;
                        break;
                    }
                }
                if (unitToDraw == null) unitToDraw = unitsAtLoc.get(0);
            }

            drawUnit(unitToDraw);
            if (unitsAtLoc.size() > 1) drawStackBadge(loc, unitsAtLoc.size());
        }

        // 2) Draw enemy contacts from the human player's ContactMap.
        if (humanPlayer != null) {
            int currentTurn = query.getGame().getTurn();
            for (com.developingstorm.games.sad.Contact contact
                    : humanPlayer.getContactMap().getAllContacts()) {
                drawContact(contact, currentTurn);
            }
        } else {
            // Spectator / no-fog mode: draw all enemy units live
            for (Unit unit : query.getAllUnits()) {
                if (unit.isCarried() && unit.inSentryMode()) continue;
                drawUnit(unit);
            }
        }
```

- [ ] **Step 2: Extract `drawStackBadge` helper**

Add this private method alongside `drawUnit` (around line 720). The body is lifted verbatim from the stack-count code that was embedded in the old draw loop:

```java
    private void drawStackBadge(Location loc, int count) {
        double[] center = getHexCenter(loc);
        String text = String.valueOf(count);
        double badgeSize = text.length() == 1 ? 14 : 18;
        double badgeX = center[0] + 12;
        double badgeY = center[1] - 12;

        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        gc.fillOval(badgeX - badgeSize / 2, badgeY - badgeSize / 2, badgeSize, badgeSize);
        gc.strokeOval(badgeX - badgeSize / 2, badgeY - badgeSize / 2, badgeSize, badgeSize);

        gc.setFill(Color.BLACK);
        javafx.scene.text.Font font = javafx.scene.text.Font.font(
            "Arial",
            javafx.scene.text.FontWeight.BOLD,
            text.length() == 1 ? 10 : 9
        );
        gc.setFont(font);
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(text, badgeX, badgeY);
        gc.setTextAlign(javafx.scene.text.TextAlignment.LEFT);
        gc.setTextBaseline(javafx.geometry.VPos.BASELINE);
    }
```

- [ ] **Step 3: Add `drawContact` helper**

Add this private method below `drawUnit` (around line 800). It handles all four rendering states:

```java
    private static final double GHOST_OPACITY = 0.45;

    private Color colorForPlayerId(int playerId) {
        if (playerId == 1) return Color.rgb(250, 100, 100);
        if (playerId == 2) return Color.rgb(150, 150, 250);
        return Color.rgb(100, 250, 100);
    }

    private void drawContact(
        com.developingstorm.games.sad.Contact contact,
        int currentTurn
    ) {
        double[] center = getHexCenter(contact.location);
        double bgSize = 28;

        boolean isCurrent = contact.isCurrent(currentTurn);
        double alpha = isCurrent ? 1.0 : GHOST_OPACITY;

        Color playerColor = colorForPlayerId(
            contact.owner != null ? contact.owner.getId() : 0
        );
        Color fadedBg = new Color(
            playerColor.getRed(),
            playerColor.getGreen(),
            playerColor.getBlue(),
            alpha
        );

        // Background tile
        gc.setFill(fadedBg);
        gc.fillRect(
            center[0] - bgSize / 2, center[1] - bgSize / 2, bgSize, bgSize
        );
        gc.setStroke(new Color(0, 0, 0, alpha));
        gc.setLineWidth(1.0);
        gc.strokeRect(
            center[0] - bgSize / 2, center[1] - bgSize / 2, bgSize, bgSize
        );

        double iconSize = 24;
        if (contact.identified && contact.type != null) {
            javafx.scene.image.Image img = terrainImages.getUnitImage(contact.type);
            if (img != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(
                    img,
                    center[0] - iconSize / 2,
                    center[1] - iconSize / 2,
                    iconSize,
                    iconSize
                );
                gc.setGlobalAlpha(1.0);
            }
        } else {
            Color silColor = new Color(0.75, 0.75, 0.75, alpha);
            terrainImages.drawSilhouette(
                gc,
                contact.travelType,
                center[0],
                center[1],
                iconSize,
                silColor
            );
        }
    }
```

Note: importing `Contact` and `Travel` inside method signatures uses the fully qualified name so the existing import block in `MapCanvas.java` doesn't need reshuffling. If you want to clean up, add `import com.developingstorm.games.sad.Contact;` at the top.

- [ ] **Step 4: Ensure `GameQueryService` exposes `getGame()`**

The new enemy-contact loop calls `query.getGame()`. Verify that method exists:

Run: `grep -n "getGame" src/main/java/com/developingstorm/games/sad/controller/GameQueryService.java`

If `getGame()` is not present there but `getCurrentTurn()` or similar is, replace `query.getGame().getTurn()` with the existing equivalent. If neither exists, add `int getCurrentTurn();` to the interface and implement it as `return game.getTurn();` in `GameQueryServiceImpl`.

- [ ] **Step 5: Launch the app and eyeball the rendering**

Run: `mvn -q compile exec:java -Dexec.mainClass=com.developingstorm.games.sad.fx.SaDFxApplication`

Start a game. Move units around so enemies drift in and out of detection range. Verify:
- Identified enemies in current vision render as normal icons.
- Unidentified enemies in current vision render as silhouettes (triangle/trapezoid/diamond by travel class) with a `?`.
- After a detected enemy leaves detection range, its contact persists at ~45% opacity for the ghost window (2–4 turns).
- After the ghost expires, it disappears.
- Own units render exactly as before.

Stop the app when satisfied.

- [ ] **Step 6: Run the full test suite**

Run: `mvn -q test`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/fx/MapCanvas.java \
        src/main/java/com/developingstorm/games/sad/controller/GameQueryService.java \
        src/main/java/com/developingstorm/games/sad/controller/GameQueryServiceImpl.java
git commit -m "feat(fx): render enemy contacts with ghost and silhouette states"
```

(Only include `GameQueryService*.java` in the add list if they were edited.)

---

## Task 8: Ghost threat layer in `ThreatMap`

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/brain/ThreatMap.java`

Adds ghost contributions at reduced weight per section 3.2 of the spec. Ghosts add a proportion of the threat of a unit at that location (as if they were still there).

- [ ] **Step 1: Rewrite `analyze` to also process ghost contacts**

In `ThreatMap.java`, replace the `analyze()` method (lines 69–122) with:

```java
    private void analyze() {
        int currentTurn = this.game.getTurn();

        // 1) Live visible enemies — full threat
        for (Player enemy : this.game.getPlayers()) {
            if (enemy.equals(this.player)) continue;
            for (Unit enemyUnit : enemy.getUnits()) {
                if (enemyUnit.isDead()) continue;
                Location loc = enemyUnit.getLocation();
                Vision visibility = this.player.getVisibility(loc);
                if (visibility == Vision.NONE) continue;

                this.visibleEnemies.add(enemyUnit);
                ThreatInfo threat = this.threatsByLocation.computeIfAbsent(
                    loc, ThreatInfo::new
                );
                threat.addEnemy(enemyUnit);
                addContinentThreat(loc, threat.threatLevel);
                checkCityThreats(enemyUnit);
            }
        }

        // 2) Ghost contacts — reduced threat (only where we don't already have
        //    a live visible enemy tracked at that hex).
        for (com.developingstorm.games.sad.Contact contact
                : this.player.getContactMap().getAllContacts()) {
            if (contact.isCurrent(currentTurn)) continue;
            if (!contact.identified || contact.type == null) continue;
            if (this.threatsByLocation.containsKey(contact.location)) continue;

            int age = contact.age(currentTurn);
            double weight;
            if (age == 1) weight = 0.5;
            else if (age >= 2) weight = 0.25;
            else weight = 1.0;

            double ghostThreat = contact.type.getAttack() * weight;
            if (ghostThreat <= 0.0) continue;

            ThreatInfo threat = this.threatsByLocation.computeIfAbsent(
                contact.location, ThreatInfo::new
            );
            threat.threatLevel += ghostThreat;
            addContinentThreat(contact.location, ghostThreat);
        }

        Log.info(
            "ThreatMap: Found " + this.visibleEnemies.size()
                + " visible enemies, " + this.threatenedCities.size()
                + " threatened cities"
        );
    }

    private void addContinentThreat(Location loc, double delta) {
        Continent continent = this.game.getBoard().getContinent(loc);
        if (continent != null) {
            double existing = this.threatsByContinent.getOrDefault(continent, 0.0);
            this.threatsByContinent.put(continent, existing + delta);
        }
    }
```

- [ ] **Step 2: Verify compilation**

Run: `mvn -q compile`
Expected: PASS.

- [ ] **Step 3: Run the full suite**

Run: `mvn -q test`
Expected: PASS. `ThreatMap`'s existing behavior for live enemies is unchanged; ghosts add only to hexes where no live enemy is already tracked.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/brain/ThreatMap.java
git commit -m "feat(ai): weight ghost contacts into ThreatMap"
```

---

## Task 9: Ghost pursuit — `DestroyerCaptain`

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/brain/DestroyerCaptain.java`

When a destroyer has no current naval target, it can move toward a known sea ghost position to investigate per section 3.3 of the spec.

- [ ] **Step 1: Locate existing target-selection logic**

Read `src/main/java/com/developingstorm/games/sad/brain/DestroyerCaptain.java` end-to-end. Identify:
- The method where it picks its move target (often called something like `chooseMove`, `plan`, or `nextOrder`).
- Whether it already queries `player.getKnownEnemies()` or iterates enemy units elsewhere.

If the captain structure does not expose an obvious "no target found" branch, add one.

- [ ] **Step 2: Add a `findGhostTarget` helper at the top of the class body**

Add this private method (adjusting to whatever member names the captain uses for `player` and `unit`):

```java
    /**
     * Returns the location of the best sea-ghost contact this captain should
     * investigate, or null if no useful ghost exists. Prefers identified
     * ghosts of higher-value targets; falls back to unidentified SEA ghosts.
     */
    private com.developingstorm.games.hexboard.Location findGhostTarget(
        com.developingstorm.games.sad.Unit unit
    ) {
        com.developingstorm.games.sad.Player owner = unit.getOwner();
        int currentTurn = owner.getGame().getTurn();

        com.developingstorm.games.hexboard.Location bestLoc = null;
        double bestScore = -1.0;

        for (com.developingstorm.games.sad.Contact c
                : owner.getContactMap().getAllContacts()) {
            if (c.isCurrent(currentTurn)) continue;
            if (c.travelType != com.developingstorm.games.sad.Travel.SEA) continue;

            double score;
            if (c.identified && c.type != null) {
                // Favor destroyer-vs-sub etc. — use attack power as proxy
                score = c.type.getAttack() + 0.5;
            } else {
                score = 0.5; // unknown — worth a look
            }

            int dist = unit.getLocation().distance(c.location);
            if (dist == 0) continue;
            score = score / (double) dist;

            if (score > bestScore) {
                bestScore = score;
                bestLoc = c.location;
            }
        }
        return bestLoc;
    }
```

- [ ] **Step 3: Plumb `findGhostTarget` into the captain's no-target fallback**

At the point where the captain decides there is no current enemy to engage and would otherwise default to patrol/explore/sentry, insert:

```java
        com.developingstorm.games.hexboard.Location ghost = findGhostTarget(unit);
        if (ghost != null) {
            // Issue a MOVE order toward the ghost. The exact call pattern
            // mirrors how the captain currently issues MOVE orders for live
            // targets — use the same helper (e.g. issueMove(unit, ghost), or
            // new Move(unit, ghost) as appropriate for this captain's style).
            issueMove(unit, ghost);  // adjust to match existing call pattern
            return;
        }
```

If the captain does not have an `issueMove` helper, inspect adjacent captains (`CruiserCaptain.java`, `FighterCaptain.java`) for the exact idiom used to create and assign a `Move` order, and mirror it. Do not invent a new ordering API.

- [ ] **Step 4: Compile**

Run: `mvn -q compile`
Expected: PASS.

- [ ] **Step 5: Run full suite**

Run: `mvn -q test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/brain/DestroyerCaptain.java
git commit -m "feat(ai): destroyer pursues sea ghost contacts"
```

---

## Task 10: Ghost pursuit — `CruiserCaptain`, `FighterCaptain`, `BomberCaptain`

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/brain/CruiserCaptain.java`
- Modify: `src/main/java/com/developingstorm/games/sad/brain/FighterCaptain.java`
- Modify: `src/main/java/com/developingstorm/games/sad/brain/BomberCaptain.java`

Each of these captains gets the same treatment as DestroyerCaptain, but filtered to the travel classes each can meaningfully investigate.

- [ ] **Step 1: CruiserCaptain**

Add `findGhostTarget(Unit)` (same structure as Task 9, but the filter is `c.travelType == Travel.SEA`). In the captain's no-current-target branch, call it and issue a MOVE if a ghost is returned.

- [ ] **Step 2: FighterCaptain**

Add `findGhostTarget(Unit)` filtered to `c.travelType == Travel.AIR || c.travelType == Travel.SEA`. Fighters can chase both air and sea ghosts (they attack air; they scout over sea). Land ghosts are ignored (fighters don't attack land units effectively).

Prefer air ghosts:

```java
        // Identified AIR ghost beats SEA ghost
        if (c.travelType == com.developingstorm.games.sad.Travel.AIR && c.identified) {
            score = c.type.getAttack() + 1.5;
        } else if (c.travelType == com.developingstorm.games.sad.Travel.AIR) {
            score = 1.0;
        } else if (c.travelType == com.developingstorm.games.sad.Travel.SEA && c.identified) {
            score = c.type.getAttack() + 0.5;
        } else if (c.travelType == com.developingstorm.games.sad.Travel.SEA) {
            score = 0.5;
        } else {
            continue; // skip LAND
        }
```

Respect fighter fuel — if the ghost distance exceeds the fighter's remaining fuel range, skip it. Add:

```java
        if (dist > unit.life().getFuel()) continue;
```

just after computing `dist`.

- [ ] **Step 3: BomberCaptain**

Add `findGhostTarget(Unit)` filtered to `c.travelType == Travel.LAND || c.travelType == Travel.SEA`. Bombers ignore air ghosts. Respect fuel identically to FighterCaptain.

- [ ] **Step 4: Compile and test**

Run: `mvn -q test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/developingstorm/games/sad/brain/CruiserCaptain.java \
        src/main/java/com/developingstorm/games/sad/brain/FighterCaptain.java \
        src/main/java/com/developingstorm/games/sad/brain/BomberCaptain.java
git commit -m "feat(ai): cruiser/fighter/bomber pursue ghost contacts"
```

---

## Task 11: Save/load — clear `ContactMap` on load

**Files:**
- Modify: `src/main/java/com/developingstorm/games/sad/persistence/GameStateSerializer.java` (only if a current `lastSeenEnemies` block is serialized)

Per section 5.1 of the spec, ContactMap is NOT persisted. On load, it should start empty and repopulate from the next turn's scan. If the old code serialized `lastSeenEnemies`, remove those lines; otherwise this task is a no-op verification.

- [ ] **Step 1: Grep for any serialization of the old tracking**

Run: `grep -n "lastSeenEnemies\|LastSeenInfo\|contactMap" src/main/java/com/developingstorm/games/sad/persistence/`

Expected: no hits. The exploration pass already confirmed `GameStateSerializer` does not persist `lastSeenEnemies`, so this task is mostly a sanity check.

- [ ] **Step 2: Load a saved game and verify**

If there is an existing save-game test (`GameStateSerializerTest`), run it:

Run: `mvn -q test -Dtest=GameStateSerializerTest`
Expected: PASS. If it fails with a reference to removed symbols, update the test to remove those references.

- [ ] **Step 3: Add a regression test that loaded games start with an empty `ContactMap`**

Append to `src/test/java/com/developingstorm/games/sad/persistence/GameStateSerializerTest.java`:

```java
    @Test
    public void testLoadedGameStartsWithEmptyContactMap() throws Exception {
        // Build a game, advance a turn so ContactMap populates, save, reload.
        Game g = buildGameForTest();  // use whatever helper the existing tests use
        g.getPlayers()[0].startNewTurn();
        int before = g.getPlayers()[0].getContactMap().size();
        assertTrue(before >= 0); // just exercise the call

        java.io.File tmp = java.io.File.createTempFile("sad-save", ".zip");
        tmp.deleteOnExit();
        new GameStateSerializer().save(g, tmp);

        Game loaded = new GameStateSerializer().load(tmp);
        for (Player p : loaded.getPlayers()) {
            assertEquals(0, p.getContactMap().size(),
                "ContactMap should be empty on load; it repopulates next turn");
        }
    }
```

Adapt `buildGameForTest()` and `new GameStateSerializer().save/load(...)` to match the actual test helper names and serializer API in that file. If the existing test file has its own game-construction helper, reuse it; do not duplicate.

- [ ] **Step 4: Run the test**

Run: `mvn -q test -Dtest=GameStateSerializerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/developingstorm/games/sad/persistence/GameStateSerializerTest.java
git commit -m "test(persistence): verify ContactMap starts empty after load"
```

---

## Task 12: End-to-end smoke test + manual verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full suite**

Run: `mvn -q test`
Expected: PASS across all tests.

- [ ] **Step 2: Launch the app and play a real game**

Run: `mvn -q compile exec:java -Dexec.mainClass=com.developingstorm.games.sad.fx.SaDFxApplication`

Scenarios to exercise:
1. Build a destroyer. Park it near unexplored ocean. Confirm it detects enemy naval units at range 3 and identifies them (full icon).
2. Build a fighter. Fly near an enemy. Confirm distant enemies render as silhouettes; close enemies render as full icons.
3. Let a detected enemy move out of detection range. Confirm the contact persists as a faded ghost for 2–4 turns, then disappears.
4. Build a cruiser. Confirm its range-4 detection and higher ghost lifetime (quality 2 → 4 turns).
5. Submarine under WATER/COMPLETE vision should always identify correctly.

Stop the app when satisfied.

- [ ] **Step 3: Inspect the plan's scope against the spec**

Skim `docs/superpowers/specs/2026-04-16-visibility-and-detection-overhaul.md` once more. Confirm each section is covered:

- Section 1.1 Type stats — Task 1.
- Section 1.2 Contact class — Task 2.
- Section 1.3 ContactMap — Task 3.
- Section 1.4 Ghost decay formula — Task 2 (maxAge) + Task 3 (refresh).
- Section 1.5 Submarine auto-ID — Task 5.
- Section 1.6 Multi-contact priority — Task 3 (getContactAt).
- Section 2.1 Per-turn flow — Task 5 (scanForContacts in startNewTurn).
- Section 2.2 Scan algorithm — Task 5.
- Section 2.3 Contact update rules — Task 3.
- Section 2.4 Hex distance — Task 5 uses `Location.distance()`.
- Section 3.1 Player query methods — Task 4.
- Section 3.2 ThreatMap ghost layer — Task 8.
- Section 3.3 Captain pursuit — Tasks 9, 10.
- Section 3.4 No combat changes — confirmed (no task needed).
- Section 4 UI rendering — Tasks 6, 7.
- Section 5 Backwards compatibility — Tasks 4, 11.
- Section 6 Square vision range — intentionally out of scope (spec says so).

- [ ] **Step 4: Final commit (if anything unstaged)**

```bash
git status
```
If clean, no action. Otherwise commit residual fixes with a descriptive message.

---

## Notes for the Executor

1. **LLM plan cross-reference:** `docs/superpowers/plans/2026-04-15-llm-state-serializer.md` (not yet implemented) references `Player.getLastSeenEnemies()` and `Player.LastSeenInfo`. Both are removed by Task 4. When/if that plan is executed later, it should use `Player.getAllContacts()` / `Contact` instead. This plan doesn't modify the other plan — flag it in your PR description so the reader knows.
2. **Archive code:** `archive/swing-ui/ui/BoardCanvas.java` also references `getLastSeenEnemies`. This directory is not compiled and should be ignored.
3. **Test patterns:** The codebase uses JUnit 5 (Jupiter). Any test that touches `Location.get` must first call `LocationMap.init(w, h)` (see `BoardTest.java`).
4. **Captain idioms vary:** Each captain issues orders differently (direct `new Move(...)` construction vs a helper). When plumbing ghost pursuit, mirror the exact pattern already in use in that file — do not introduce a new helper name.
5. **Colors are hard-coded to player id today.** `MapCanvas.drawUnit` branches on `playerId` for red/blue/green. `drawContact` reuses the same palette via `colorForPlayerId`. If the game ever grows past 3 players, this is a preexisting limitation, not a bug introduced by this plan.
