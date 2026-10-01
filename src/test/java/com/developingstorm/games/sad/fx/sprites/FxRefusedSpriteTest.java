package com.developingstorm.games.sad.fx.sprites;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class FxRefusedSpriteTest {

    @Test
    public void fadesOutThenIsDone() {
        AtomicLong now = new AtomicLong(1_000);
        FxRefusedSprite x = new FxRefusedSprite(10, 20, now::get);

        assertFalse(x.done());
        assertEquals(1.0, x.opacity(), 1e-9);

        now.addAndGet(FxRefusedSprite.DURATION_MS / 2);
        assertFalse(x.done());
        assertTrue(x.opacity() > 0 && x.opacity() < 1, "fading");

        now.addAndGet(FxRefusedSprite.DURATION_MS / 2);
        assertTrue(x.done());
    }
}
