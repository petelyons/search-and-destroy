package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.edicts.CityAirPatrol;
import com.developingstorm.games.sad.orders.Patrol;
import com.developingstorm.games.sad.persistence.GameStateSerializer;
import com.developingstorm.util.RandomUtil;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SuspendedPatrolTest {

    private Game game;
    private Player human;
    private City home;
    private Unit fighter;

    @BeforeEach
    public void setUp() {
        RandomUtil.setSeed(20261106L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        human = new Player("Human", 1);
        Robot robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        game = new Game(new Player[] {human, robot}, map, new MockSaDBoardContext());
        home = human.getCities().get(0);
        fighter = game.createUnit(Type.FIGHTER, human, home.getLocation());
        human.addUnit(fighter);
    }

    private List<Location> route() {
        Location c = home.getLocation();
        // Air patrols must start and end at a city
        return List.of(c, Location.get(c.x + 1, c.y), c);
    }

    private Patrol patrol(Patrol.PatrolMode mode) {
        return fighter.newPatrolOrder(route(), mode);
    }

    @Test
    public void resumeRestoresRouteAndProgress() {
        Patrol p = patrol(Patrol.PatrolMode.LINEAR);
        p.restoreState(2, true);
        fighter.assignOrder(p);

        fighter.suspendPatrol(p);
        assertNull(fighter.getOrder());

        assertTrue(fighter.resumePatrol());
        assertSame(p, fighter.getOrder());
        assertEquals(2, p.getCurrentWaypointIndex());
        assertTrue(p.isReverseDirection());
        assertNull(fighter.getSuspendedPatrol());
    }

    @Test
    public void skipTurnKeepsSuspended() {
        fighter.suspendPatrol(patrol(Patrol.PatrolMode.LOOP));
        fighter.assignOrder(fighter.newSkipTurn());
        assertNotNull(fighter.getSuspendedPatrol());
    }

    @Test
    public void otherOrdersDiscard() {
        fighter.suspendPatrol(patrol(Patrol.PatrolMode.LOOP));
        fighter.assignOrder(fighter.newSentryOrder());
        assertNull(fighter.getSuspendedPatrol());

        fighter.suspendPatrol(patrol(Patrol.PatrolMode.LOOP));
        fighter.assignOrder(fighter.newMoveOrder(Location.get(home.getLocation().x + 1, home.getLocation().y)));
        assertNull(fighter.getSuspendedPatrol());

        fighter.suspendPatrol(patrol(Patrol.PatrolMode.LOOP));
        fighter.assignOrder(patrol(Patrol.PatrolMode.LINEAR));
        assertNull(fighter.getSuspendedPatrol());
    }

    @Test
    public void resumeWithNothingIsFalse() {
        Order sentry = fighter.newSentryOrder();
        fighter.assignOrder(sentry);
        assertFalse(fighter.resumePatrol());
        assertSame(sentry, fighter.getOrder());
    }

    @Test
    public void survivesSaveAndLoad() throws Exception {
        Patrol p = patrol(Patrol.PatrolMode.LINEAR);
        p.restoreState(1, true);
        fighter.suspendPatrol(p);
        File f = Files.createTempFile("suspended", ".sav").toFile();
        f.deleteOnExit();
        new GameStateSerializer().saveGameToFile(game, f);

        Game loaded = new GameStateSerializer().loadGame(f, new MockSaDBoardContext());

        // Unit ids are renumbered on load; find the human's only fighter
        Unit back = loaded.units().stream()
            .filter(u -> u.getType() == Type.FIGHTER && !u.getOwner().isRobot())
            .findFirst().orElse(null);
        assertNotNull(back, "fighter should load");
        Patrol s = back.getSuspendedPatrol();
        assertNotNull(s, "suspended patrol should load");
        assertEquals(route(), s.getWaypoints());
        assertEquals(Patrol.PatrolMode.LINEAR, s.getMode());
        assertEquals(1, s.getCurrentWaypointIndex());
        assertTrue(s.isReverseDirection());
    }

    @Test
    public void cityAirPatrolSkipsSuspended() {
        Patrol p = patrol(Patrol.PatrolMode.LOOP);
        fighter.suspendPatrol(p);
        CityAirPatrol edict = new CityAirPatrol(human, home, route(), Patrol.PatrolMode.LOOP);

        edict.execute(game);

        assertNull(fighter.getOrder(), "edict must not hand a new patrol to a waiting lookout");
        assertSame(p, fighter.getSuspendedPatrol());
    }
}
