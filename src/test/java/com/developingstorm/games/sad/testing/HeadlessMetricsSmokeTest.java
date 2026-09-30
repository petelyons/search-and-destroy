package com.developingstorm.games.sad.testing;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.sad.MockSaDBoardContext;
import org.junit.jupiter.api.Test;

public class HeadlessMetricsSmokeTest {

    @Test
    public void shortGameProducesBehaviorReport() throws Exception {
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        HeadlessGameRunner runner = new HeadlessGameRunner(map, new MockSaDBoardContext(), 8, null, null);
        runner.setMonitorEnabled(true);

        HeadlessGameRunner.GameResult result = runner.run();

        BehaviorReport report = result.getBehaviorReport();
        assertNotNull(report, "report should be attached: " + result);
        assertFalse(report.keys().isEmpty());
        for (String k : report.keys()) {
            assertTrue(report.get(k) >= 0, k + " should be non-negative");
        }
        assertTrue(result.getMeanTurnMs() >= 0);
    }
}
