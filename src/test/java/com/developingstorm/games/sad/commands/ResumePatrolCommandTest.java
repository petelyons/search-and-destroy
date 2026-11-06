package com.developingstorm.games.sad.commands;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.orders.Patrol;
import com.developingstorm.util.RandomUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ResumePatrolCommandTest {

    private Game game;
    private Unit fighter;
    private Patrol patrol;

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
        fighter = game.createUnit(Type.FIGHTER, human, home);
        human.addUnit(fighter);
        patrol = fighter.newPatrolOrder(List.of(home, Location.get(home.x + 1, home.y), home), Patrol.PatrolMode.LOOP);
    }

    @Test
    public void resumesSuspendedPatrol() {
        fighter.suspendPatrol(patrol);
        new ResumePatrolCommand(fighter).execute(game);
        assertSame(patrol, fighter.getOrder());
        assertNull(fighter.getSuspendedPatrol());
    }

    @Test
    public void noSuspendedPatrolIsNoOp() {
        Order sentry = fighter.newSentryOrder();
        fighter.assignOrder(sentry);
        new ResumePatrolCommand(fighter).execute(game);
        assertSame(sentry, fighter.getOrder());
    }

    @Test
    public void deadUnitIsIgnored() {
        fighter.suspendPatrol(patrol);
        game.killUnit(fighter);
        assertDoesNotThrow(() -> new ResumePatrolCommand(fighter).execute(game));
        assertNull(fighter.getOrder());
    }
}
