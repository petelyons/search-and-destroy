package com.developingstorm.games.sad.brain;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import org.junit.jupiter.api.Test;

public class TargetPrioritizerFairnessTest {

    @Test
    public void onlyExploredCitiesAreTargets() {
        com.developingstorm.util.RandomUtil.setSeed(20260930L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        Robot a = new Robot("A", 1);
        RobotBrain brain = new RobotBrain(a);
        a.setBrain(brain);
        Robot b = new Robot("B", 2);
        b.setBrain(new RobotBrain(b));
        Game game = new Game(new Player[] {a, b}, map, new MockSaDBoardContext());
        brain.startNewTurn();

        boolean anyUnexplored = game.getBoard().getCities().stream()
            .anyMatch(c -> !a.isExplored(c.getLocation()));
        assertTrue(anyUnexplored, "test needs some unexplored cities");

        for (TargetPrioritizer.CityTarget t : brain.getBattleplan().getPrioritizedCities()) {
            City c = t.city;
            assertTrue(a.isExplored(c.getLocation()), "targeted unexplored city " + c);
        }
    }

    @Test
    public void defendChoiceIsDeterministic() {
        com.developingstorm.util.RandomUtil.setSeed(20260930L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        Robot a = new Robot("A", 1);
        RobotBrain brain = new RobotBrain(a);
        a.setBrain(brain);
        Robot b = new Robot("B", 2);
        b.setBrain(new RobotBrain(b));
        new Game(new Player[] {a, b}, map, new MockSaDBoardContext());
        brain.startNewTurn();
        General general = new General(brain.getBattleplan());
        boolean threatened = !brain.getBattleplan().getThreatMap().getThreatenedCities().isEmpty();

        for (int i = 0; i < 50; i++) {
            assertEquals(threatened, general.shouldDefend(), "call " + i);
        }
    }
}
