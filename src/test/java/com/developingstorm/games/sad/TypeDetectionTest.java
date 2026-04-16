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
