package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;

public final class TestUnits {
    static { LocationMap.init(30, 30); }
    private static long nextId = 1000L;

    public static Unit stubUnit(Type type, Player owner) {
        return stubUnit(type, owner, Location.get(5, 5));
    }

    public static Unit stubUnit(Type type, Player owner, Location loc) {
        Unit u = Unit.createForTest(nextId++, type, owner, loc);
        owner.addUnit(u);
        return u;
    }

    private TestUnits() {}
}
