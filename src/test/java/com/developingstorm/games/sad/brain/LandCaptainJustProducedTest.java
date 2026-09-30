package com.developingstorm.games.sad.brain;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import org.junit.jupiter.api.Test;

public class LandCaptainJustProducedTest {

    private static Unit unitCreatedOn(int turn) {
        LocationMap.init(30, 30);
        Unit u = Unit.createForTest(5, Type.INFANTRY, new Player("A", 1), Location.get(3, 3));
        u.setCreatedTurn(turn);
        return u;
    }

    @Test
    public void unitBuiltThisTurnInCityIsJustProduced() {
        assertTrue(LandCaptain.isJustProduced(unitCreatedOn(7), 7, true));
    }

    @Test
    public void olderUnitInCityIsNotJustProduced() {
        assertFalse(LandCaptain.isJustProduced(unitCreatedOn(3), 7, true));
    }

    @Test
    public void unitOutsideCityIsNotJustProduced() {
        assertFalse(LandCaptain.isJustProduced(unitCreatedOn(7), 7, false));
    }
}
