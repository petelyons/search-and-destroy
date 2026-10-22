package com.developingstorm.games.sad.fx;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MapUnitPickerTest {

    private Player human;
    private Unit transport;
    private Unit cargo;
    private Location sea;

    @BeforeEach
    public void setUp() {
        LocationMap.init(30, 30);
        human = new Player("Human", 1);
        sea = Location.get(5, 5);
        transport = Unit.createForTest(1, Type.TRANSPORT, human, sea);
        cargo = Unit.createForTest(2, Type.INFANTRY, human, sea);
        cargo.onboard = transport;
    }

    @Test
    public void selectedCargoIsDrawnInsteadOfItsTransport() {
        Map<Location, List<Unit>> byLoc =
            MapUnitPicker.ownUnitsByLocation(List.of(transport, cargo), human, cargo);
        assertSame(cargo, MapUnitPicker.unitToDraw(byLoc.get(sea), cargo));
    }

    @Test
    public void unselectedCargoStaysHidden() {
        Map<Location, List<Unit>> byLoc =
            MapUnitPicker.ownUnitsByLocation(List.of(transport, cargo), human, transport);
        assertEquals(List.of(transport), byLoc.get(sea));
        assertSame(transport, MapUnitPicker.unitToDraw(byLoc.get(sea), transport));
    }
}
