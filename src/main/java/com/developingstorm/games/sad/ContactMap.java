package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import java.util.ArrayList;
import java.util.Collection;
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
     * identification level, the fresher sighting (later {@code turnDetected})
     * wins, and within the same turn, the higher {@code spotterQuality} wins.
     * In the common case (same turn) this is equivalent to "longer remaining
     * ghost life wins," since higher spotter quality extends ghost lifetime.
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
        // Snapshot: writes happen on the game thread while rendering reads on
        // the JavaFX thread, so callers must iterate a stable copy.
        return new ArrayList<>(contacts.values());
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
