package com.developingstorm.games.sad.brain;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Type;
import org.junit.jupiter.api.Test;

public class OperationProductionTest {

    @Test
    public void operationsDoNotStarveEarlyExpansion() {
        com.developingstorm.util.RandomUtil.setSeed(20260930L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        Robot a = new Robot("A", 1);
        RobotBrain brain = new RobotBrain(a);
        a.setBrain(brain);
        Robot b = new Robot("B", 2);
        b.setBrain(new RobotBrain(b));
        Game game = new Game(new Player[] {a, b}, map, new MockSaDBoardContext());
        City home = a.getCities().get(0);
        brain.startNewTurn();
        assumeTrue(brain.getBattleplan().getProductionPair(home) == null, "home is not a paired factory");

        // Operations wanting transports and cargo exist, but nothing is staffed yet
        brain.getStrategyMemory().getOrCreateInvasionPlan(Location.get(40, 40));
        assumeTrue(a.getUnits().isEmpty());

        assertNotEquals(Type.TRANSPORT, brain.getBattleplan().productionChoice(home),
            "unstaffed operations must not turn the first city into a transport yard");
    }
}
