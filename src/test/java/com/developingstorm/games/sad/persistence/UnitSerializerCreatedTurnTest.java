package com.developingstorm.games.sad.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.json.JsonObj;
import org.junit.jupiter.api.Test;

public class UnitSerializerCreatedTurnTest {

    private static Unit unit() {
        LocationMap.init(30, 30);
        return Unit.createForTest(77, Type.INFANTRY, new Player("A", 1), Location.get(3, 3));
    }

    @Test
    public void roundTripsCreatedTurn() {
        Unit u = unit();
        u.setCreatedTurn(42);
        JsonObj json = UnitSerializer.serializeUnit(u);
        assertEquals(42, UnitSerializer.deserializeUnit(json).createdTurn);
    }

    @Test
    public void missingFieldDefaultsToZero() {
        JsonObj json = UnitSerializer.serializeUnit(unit());
        json.entrySet().removeIf(e -> e.getKey().equals("createdTurn"));
        assertEquals(0, UnitSerializer.deserializeUnit(json).createdTurn);
    }
}
