package com.developingstorm.games.sad.fx.sprites;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class FxPulseSpriteTest {

    private static final double HEX_WIDTH = 24 * 1.7320508;

    private final AtomicLong now = new AtomicLong(1_000);

    private FxPulseSprite pulse(int vision) {
        return new FxPulseSprite(100, 100, vision, now::get);
    }

    @Test
    public void outlineReachesTheEdgeOfTheFarthestVisibleHex() {
        assertEquals(2.5 * HEX_WIDTH, FxPulseSprite.outlineRadius(2), 1e-6);
        assertEquals(3.5 * HEX_WIDTH, FxPulseSprite.outlineRadius(3), 1e-6);
    }

    @Test
    public void firstRingStartsOnTheTileAndOthersWait() {
        FxPulseSprite p = pulse(3);
        assertTrue(p.ringRadius(0, 0) > 0);
        assertTrue(p.ringRadius(0, 0) < HEX_WIDTH / 2, "starts inside the unit's tile");
        assertTrue(p.ringRadius(1, 0) < 0, "second ring hasn't started");
    }

    @Test
    public void ringsGrowOutToTheVisionLimitAndStop() {
        FxPulseSprite p = pulse(3);
        double max = FxPulseSprite.outlineRadius(3);
        double last = 0;
        for (long t = 0; t <= FxPulseSprite.RING_MS; t += 50) {
            double r = p.ringRadius(0, t);
            assertTrue(r >= last, "never shrinks");
            assertTrue(r <= max + 1e-6, "never passes the vision limit");
            last = r;
        }
        assertEquals(max, last, 1e-6);
    }

    @Test
    public void ringsFadeOut() {
        FxPulseSprite p = pulse(2);
        assertTrue(p.ringAlpha(0, 0) > 0.5);
        assertEquals(0, p.ringAlpha(0, FxPulseSprite.RING_MS), 1e-6);
    }

    @Test
    public void doneAfterTheLastRing() {
        FxPulseSprite p = pulse(2);
        assertFalse(p.done());
        now.addAndGet(FxPulseSprite.TOTAL_MS - 1);
        assertFalse(p.done());
        now.addAndGet(1);
        assertTrue(p.done());
    }
}
