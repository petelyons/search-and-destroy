package com.developingstorm.games.sad.fx;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.orders.Patrol;
import com.developingstorm.util.RandomUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SightingPulsesTest {

    private Game game;
    private Unit lookout;
    private Unit other;
    private Patrol patrol;
    private final SightingPulses pulses = new SightingPulses();

    @BeforeEach
    public void setUp() {
        RandomUtil.setSeed(20261106L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        Player human = new Player("Human", 1);
        Robot robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        game = new Game(new Player[] {human, robot}, map, new MockSaDBoardContext());
        Location home = human.getCities().get(0).getLocation();
        lookout = game.createUnit(Type.FIGHTER, human, home);
        human.addUnit(lookout);
        other = game.createUnit(Type.FIGHTER, human, home);
        human.addUnit(other);
        patrol = lookout.newPatrolOrder(List.of(home, Location.get(home.x + 1, home.y), home), Patrol.PatrolMode.LOOP);
        lookout.suspendPatrol(patrol);
    }

    @Test
    public void pulsesOnceWhenTheLookoutGetsFocus() {
        pulses.sighted(lookout);
        assertFalse(pulses.takeOnFocus(other));
        assertTrue(pulses.takeOnFocus(lookout));
        assertFalse(pulses.takeOnFocus(lookout), "only the first focus after the sighting");
    }

    @Test
    public void nothingWithoutASighting() {
        assertFalse(pulses.takeOnFocus(lookout));
        assertFalse(pulses.takeOnFocus(null));
    }

    @Test
    public void noPulseOnceThePatrolHasBeenResumed() {
        pulses.sighted(lookout);
        lookout.resumePatrol();
        assertFalse(pulses.takeOnFocus(lookout));
    }

    @Test
    public void noPulseForADeadLookout() {
        pulses.sighted(lookout);
        game.killUnit(lookout);
        assertFalse(pulses.takeOnFocus(lookout));
    }
}
