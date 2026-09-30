package com.developingstorm.games.hexboard;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HexBoardMapTest {

    @Test
    public void testNonSquareDimensions() {
        HexBoardMap map = new HexBoardMap(32, 28);
        assertEquals(32, map.getWidth());
        assertEquals(28, map.getHeight());
        LocationMap.init(50, 50);
    }
}
