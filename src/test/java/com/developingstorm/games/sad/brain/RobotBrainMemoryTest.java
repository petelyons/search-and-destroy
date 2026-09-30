package com.developingstorm.games.sad.brain;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.brain.commit.CommitmentKind;
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

    @Test
    public void newTurnPrunesCommitmentsOfDeadUnits() {
        LocationMap.init(30, 30);
        Player p = new Player("A", 1);
        Unit alive = Unit.createForTest(1, Type.INFANTRY, p, Location.get(1, 1));
        Unit dead = Unit.createForTest(2, Type.INFANTRY, p, Location.get(2, 2));
        StrategyMemory memory = new StrategyMemory();
        memory.getCommitments().commit(alive, CommitmentKind.EXPLORE, null, 1);
        memory.getCommitments().commit(dead, CommitmentKind.EXPLORE, null, 1);
        dead.life().kill();

        memory.startNewTurn();

        assertNotNull(memory.getCommitments().get(alive));
        assertNull(memory.getCommitments().get(dead));
    }
}
