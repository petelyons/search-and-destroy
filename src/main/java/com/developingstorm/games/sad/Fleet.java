package com.developingstorm.games.sad;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Fleet {

    private final long id;
    private final Player owner;
    private Unit flagship;
    private final Map<Unit, FleetRole> members = new LinkedHashMap<>();

    public Fleet(long id, Player owner, Unit flagship) {
        if (owner == null || flagship == null) {
            throw new SaDException("Fleet requires owner and flagship");
        }
        if (!flagship.getType().isCapitalShip()) {
            throw new SaDException(
                "Flagship must be a capital ship, got " + flagship.getType());
        }
        if (flagship.getOwner() != owner) {
            throw new SaDException("Flagship owner mismatch");
        }
        this.id = id;
        this.owner = owner;
        this.flagship = flagship;
        this.members.put(flagship, FleetRole.FLAGSHIP);
    }

    public long getId() { return id; }
    public Player getOwner() { return owner; }
    public Unit getFlagship() { return flagship; }
    public int size() { return members.size(); }
    public Map<Unit, FleetRole> membersView() {
        return Collections.unmodifiableMap(members);
    }
    public FleetRole roleOf(Unit u) { return members.get(u); }
    public boolean contains(Unit u) { return members.containsKey(u); }

    void addMember(Unit u, FleetRole role) {
        if (u == null || role == null) throw new SaDException("null member or role");
        if (members.containsKey(u)) throw new SaDException("unit already in fleet");
        members.put(u, role);
    }

    void removeMember(Unit u) {
        members.remove(u);
    }

    void setFlagship(Unit newFlagship) {
        if (!members.containsKey(newFlagship)) {
            throw new SaDException("new flagship must be a member");
        }
        if (flagship != null && members.containsKey(flagship)) {
            members.put(flagship, FleetRole.SCREEN);
        }
        this.flagship = newFlagship;
        members.put(newFlagship, FleetRole.FLAGSHIP);
    }
}
