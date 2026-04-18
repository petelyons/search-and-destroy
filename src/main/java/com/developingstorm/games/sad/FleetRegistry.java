package com.developingstorm.games.sad;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FleetRegistry {

    private final Map<Long, Fleet> byId = new LinkedHashMap<>();
    private long nextId = 1L;

    public Collection<Fleet> allFleets() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public Fleet byId(long id) { return byId.get(id); }

    public Fleet form(Unit flagship, List<Unit> initialMembers) {
        if (flagship == null) throw new SaDException("null flagship");
        if (flagship.getFleet() != null) {
            throw new SaDException("flagship already in a fleet");
        }
        Fleet f = new Fleet(nextId++, flagship.getOwner(), flagship);
        flagship.setFleet(f);
        byId.put(f.getId(), f);
        for (Unit m : initialMembers) {
            add(f, m);
        }
        return f;
    }

    public void add(Fleet f, Unit u) {
        if (u == null) throw new SaDException("null member");
        if (u.getFleet() != null) {
            throw new SaDException("unit already in a fleet");
        }
        if (u.getOwner() != f.getOwner()) {
            throw new SaDException("fleet member must have same owner");
        }
        if (u.getTravel() != Travel.SEA) {
            throw new SaDException("fleet members must be SEA units");
        }
        if (u.isCarried()) {
            throw new SaDException("carried unit cannot join fleet");
        }
        f.addMember(u, roleFor(u));
        u.setFleet(f);
    }

    public void leave(Unit u) {
        Fleet f = u.getFleet();
        if (f == null) return;
        boolean wasFlagship = u == f.getFlagship();
        f.removeMember(u);
        u.setFleet(null);
        if (wasFlagship) {
            Unit next = pickNewFlagship(f);
            if (next != null) {
                f.setFlagship(next);
            } else {
                disband(f);
            }
        }
    }

    public void onUnitDied(Unit u) {
        leave(u);
    }

    public void disband(Fleet f) {
        List<Unit> snapshot = new ArrayList<>(f.membersView().keySet());
        for (Unit m : snapshot) {
            f.removeMember(m);
            m.setFleet(null);
        }
        byId.remove(f.getId());
    }

    private Unit pickNewFlagship(Fleet f) {
        for (Unit m : f.membersView().keySet()) {
            if (m.getType().isCapitalShip()) return m;
        }
        return null;
    }

    private FleetRole roleFor(Unit u) {
        if (u.getType() == Type.TRANSPORT) return FleetRole.TRANSPORT;
        return FleetRole.SCREEN;
    }
}
