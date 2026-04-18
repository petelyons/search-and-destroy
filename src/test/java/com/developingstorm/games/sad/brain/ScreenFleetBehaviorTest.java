package com.developingstorm.games.sad.brain;

import static org.junit.jupiter.api.Assertions.*;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.*;
import org.junit.jupiter.api.Test;

public class ScreenFleetBehaviorTest {

    @Test
    public void interceptsVisibleEnemyNearFlagship() {
        FleetHarness h = FleetHarness.withFlagshipAndScreen(Type.DESTROYER, 2);
        Unit enemy = h.game.createUnit(Type.DESTROYER, h.enemy,
            findWaterNear(h.game, h.flagship.getLocation(), 2));
        h.enemy.addUnit(enemy);
        h.owner.startNewTurn();
        Location choice = ScreenFleetBehavior.chooseMove(h.screen, h.fleet, h.game);
        assertNotNull(choice, "screen should pick a target hex");
        int dBefore = h.screen.getLocation().distance(enemy.getLocation());
        int dAfter = choice.distance(enemy.getLocation());
        assertTrue(dAfter < dBefore, "intercept: should step closer to enemy");
    }

    @Test
    public void interposesOnDetectionRingWhenEnemyIsFar() {
        FleetHarness h = FleetHarness.withFlagshipAndScreen(Type.DESTROYER, 2);
        Unit enemy = h.game.createUnit(Type.DESTROYER, h.enemy,
            findWaterNear(h.game, h.flagship.getLocation(), 10));
        h.enemy.addUnit(enemy);
        h.owner.startNewTurn();
        Location choice = ScreenFleetBehavior.chooseMove(h.screen, h.fleet, h.game);
        assertNotNull(choice);
        int ring = h.screen.getType().getVisionDistance();
        int d = choice.distance(h.flagship.getLocation());
        assertTrue(Math.abs(d - ring) <= 1,
            "interpose: target should be approximately on flagship's vision ring, got d=" + d);
    }

    @Test
    public void trailsFlagshipWhenNoEnemyKnown() {
        FleetHarness h = FleetHarness.withFlagshipAndScreen(Type.DESTROYER, 5);
        h.owner.startNewTurn();
        Location choice = ScreenFleetBehavior.chooseMove(h.screen, h.fleet, h.game);
        assertNotNull(choice);
        assertTrue(choice.distance(h.flagship.getLocation()) <= 5,
            "trail: target should be along path toward flagship");
    }

    private static Location findWaterNear(Game g, Location center, int distance) {
        for (Location l : center.getRing(distance)) {
            if (!g.getBoard().isWater(l)) continue;
            if (g.unitAtLocation(l) != null) continue;
            return l;
        }
        throw new SaDException("no water at distance " + distance);
    }
}
