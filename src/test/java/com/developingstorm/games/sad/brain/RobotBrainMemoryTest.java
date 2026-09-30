package com.developingstorm.games.sad.brain;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import org.junit.jupiter.api.Test;

public class RobotBrainMemoryTest {

    @Test
    public void battleplanUsesTheBrainsPersistentMemory() {
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        Robot a = new Robot("A", 1);
        RobotBrain brain = new RobotBrain(a);
        a.setBrain(brain);
        Robot b = new Robot("B", 2);
        b.setBrain(new RobotBrain(b));
        new Game(new Player[] {a, b}, map, new MockSaDBoardContext());

        brain.startNewTurn();
        StrategyMemory first = brain.getBattleplan().getStrategyMemory();
        brain.startNewTurn();
        StrategyMemory second = brain.getBattleplan().getStrategyMemory();

        assertSame(first, second, "memory must survive across turns");
        assertSame(brain.getStrategyMemory(), first);
        assertSame(brain.getOperationsCoordinator().getMemory(), first);
    }
}
