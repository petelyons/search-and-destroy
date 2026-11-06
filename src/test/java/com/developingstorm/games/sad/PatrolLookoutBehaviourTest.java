package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.events.GameEvent;
import com.developingstorm.games.sad.events.GameEventListener;
import com.developingstorm.games.sad.events.GameEventType;
import com.developingstorm.games.sad.events.PatrolReportEvent;
import com.developingstorm.games.sad.orders.Patrol;
import com.developingstorm.util.RandomUtil;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PatrolLookoutBehaviourTest {

    private Game game;
    private Player human;
    private Robot robot;
    private Location sea;      // patrol start
    private Location far;      // second waypoint, 3 hexes from sea
    private final List<PatrolReportEvent> reports = new ArrayList<>();
    private final List<GameEvent> combats = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        RandomUtil.setSeed(20261106L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        human = new Player("Human", 1);
        robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        game = new Game(new Player[] {human, robot}, map, new MockSaDBoardContext());
        sea = PatrolLookoutTest.openWater(game, 5, null);
        far = hexAt(sea, 3);
        game.getEventBus().addListener(new GameEventListener() {
            @Override
            public void onGameEvent(GameEvent e) {
                if (e instanceof PatrolReportEvent pre) {
                    reports.add(pre);
                } else {
                    combats.add(e);
                }
            }

            @Override
            public GameEventType[] getInterestedEventTypes() {
                return new GameEventType[] { GameEventType.PATROL_REPORT, GameEventType.COMBAT_RESOLVED };
            }
        });
    }

    private static Location hexAt(Location from, int d) {
        for (Location l : from.getRing(d)) {
            if (from.distance(l) == d) {
                return l;
            }
        }
        throw new SaDException("no hex at distance " + d);
    }

    private Unit place(Type t, Player owner, Location loc) {
        Unit u = game.createUnit(t, owner, loc);
        owner.addUnit(u);
        u.turn().beginTurn();
        return u;
    }

    private Patrol patrol(Unit u, Location a, Location b) {
        Patrol p = u.newPatrolOrder(List.of(a, b), Patrol.PatrolMode.LINEAR);
        u.assignOrder(p);
        return p;
    }

    /** A water hex out of the destroyer's sight at {@code sea} but within its sight of {@code far}. */
    private Location justOutOfSight() {
        int range = Type.DESTROYER.getVisionDistance();
        for (int r = range + 1; r <= 5; r++) {
            for (Location l : sea.getRing(r)) {
                if (sea.distance(l) > range && l.distance(far) <= range && !l.equals(far)
                        && game.getBoard().isWater(l) && game.unitsAtLocation(l).isEmpty()) {
                    return l;
                }
            }
        }
        throw new SaDException("no suitable hex");
    }

    private void step(Unit u, Location to) {
        u.turn().beginTurn();
        u.move(to);
    }

    @Test
    public void wakesOnUnreportedEnemy() {
        Unit dd = place(Type.DESTROYER, human, sea);
        Patrol p = patrol(dd, sea, far);
        Unit tr = place(Type.TRANSPORT, robot, justOutOfSight());

        dd.turn().attemptTurn();

        assertNull(dd.getOrder());
        assertSame(p, dd.getSuspendedPatrol());
        assertEquals(1, reports.size());
        PatrolReportEvent e = reports.get(0);
        assertEquals(PatrolReportEvent.Kind.SIGHTING, e.getKind());
        assertSame(dd, e.getPatroller());
        assertSame(tr, e.getEnemy());
        assertEquals(tr.getLocation(), e.getLocation());
        assertTrue(human.isReported(tr));
    }

    @Test
    public void doesNotWakeTwiceForSameEnemy() {
        Unit dd = place(Type.DESTROYER, human, sea);
        Patrol p = patrol(dd, sea, far);
        place(Type.TRANSPORT, robot, justOutOfSight());
        dd.turn().attemptTurn();
        assertEquals(1, reports.size());

        assertTrue(dd.resumePatrol());
        dd.turn().beginTurn();
        dd.turn().attemptTurn();

        assertEquals(1, reports.size(), "same enemy still in sight");
        assertNull(dd.getSuspendedPatrol());
        assertSame(p, dd.getOrder());
    }

    @Test
    public void wakesAgainAfterEnemyReturns() {
        Unit dd = place(Type.DESTROYER, human, sea);
        patrol(dd, sea, far);
        Unit tr = place(Type.TRANSPORT, robot, hexAt(sea, 1));
        dd.turn().attemptTurn();
        assertEquals(1, reports.size());
        Location home = tr.getLocation();

        // The transport sails out of sight...
        List<Location> trail = new ArrayList<>();
        while (tr.getLocation().distance(dd.getLocation()) <= Type.DESTROYER.getVisionDistance() + 1) {
            Location next = null;
            for (Location n : tr.getLocation().getRing(1)) {
                if (game.getBoard().isWater(n) && game.unitsAtLocation(n).isEmpty()
                        && n.distance(dd.getLocation()) > tr.getLocation().distance(dd.getLocation())) {
                    next = n;
                    break;
                }
            }
            assertNotNull(next, "room to sail away");
            trail.add(tr.getLocation());
            step(tr, next);
        }
        human.startNewTurn();
        assertFalse(human.isReported(tr));

        // ...and comes back
        for (int i = trail.size() - 1; i >= 0; i--) {
            step(tr, trail.get(i));
        }
        assertEquals(home, tr.getLocation());
        dd.resumePatrol();
        dd.turn().beginTurn();
        dd.turn().attemptTurn();

        assertEquals(2, reports.size());
        assertEquals(PatrolReportEvent.Kind.SIGHTING, reports.get(1).getKind());
    }

    @Test
    public void reportedEnemyOnRouteBlocksWithoutCombat() {
        Unit dd = place(Type.DESTROYER, human, sea);
        Patrol p = patrol(dd, sea, far);
        Unit enemy = place(Type.DESTROYER, robot, far);
        human.markReported(enemy);

        dd.turn().attemptTurn();

        assertEquals(1, reports.size());
        assertEquals(PatrolReportEvent.Kind.BLOCKED, reports.get(0).getKind());
        assertSame(enemy, reports.get(0).getEnemy());
        assertSame(p, dd.getSuspendedPatrol());
        assertFalse(dd.isDead());
        assertFalse(enemy.isDead());
        assertEquals(Type.DESTROYER.getHits(), dd.life().hits);
        assertEquals(Type.DESTROYER.getHits(), enemy.life().hits);
        assertTrue(combats.isEmpty(), "a patrol must not start a fight");
    }

    @Test
    public void noPathBlocksInsteadOfCancelling() {
        Unit dd = place(Type.DESTROYER, human, sea);
        Patrol p = patrol(dd, sea, far);
        for (Location l : far.getRing(1)) {
            game.getBoard().makeLand(l);
        }

        dd.turn().attemptTurn();

        assertEquals(1, reports.size());
        assertEquals(PatrolReportEvent.Kind.BLOCKED, reports.get(0).getKind());
        assertEquals(far, reports.get(0).getLocation());
        assertSame(p, dd.getSuspendedPatrol());
    }

    @Test
    public void friendlyInTheWayStillWaits() {
        Unit dd = place(Type.DESTROYER, human, sea);
        Patrol p = patrol(dd, sea, far);
        place(Type.DESTROYER, human, far);

        dd.turn().attemptTurn();

        assertTrue(reports.isEmpty());
        assertSame(p, dd.getOrder());
        assertNull(dd.getSuspendedPatrol());
    }

    @Test
    public void secondPatrolDoesNotWakeForSameEnemy() {
        Unit dd1 = place(Type.DESTROYER, human, sea);
        patrol(dd1, sea, far);
        Unit dd2 = place(Type.DESTROYER, human, hexAt(sea, 1));
        Patrol p2 = patrol(dd2, hexAt(sea, 1), far);
        place(Type.TRANSPORT, robot, hexAt(sea, 2));

        dd1.turn().attemptTurn();
        dd2.turn().attemptTurn();

        assertEquals(1, reports.size());
        assertSame(dd1, reports.get(0).getPatroller());
        assertSame(p2, dd2.getOrder());
    }

    @Test
    public void robotPatrolsAreUnchanged() {
        Unit rd = place(Type.DESTROYER, robot, sea);
        patrol(rd, sea, far);
        place(Type.TRANSPORT, human, hexAt(sea, 1));

        rd.turn().attemptTurn();

        assertTrue(reports.isEmpty());
        assertNull(rd.getSuspendedPatrol());
    }

    @Test
    public void resumeWithNoMovesKeepsTheWaypoint() {
        Unit dd = place(Type.DESTROYER, human, sea);
        Patrol p = patrol(dd, sea, far);
        p.restoreState(1, false); // heading out to far
        dd.suspendPatrol(p);
        dd.life().burnMoves(); // woke on its last step: no moves left, turn not over

        assertTrue(dd.resumePatrol());
        dd.turn().attemptTurn();

        assertEquals(1, p.getCurrentWaypointIndex(), "resuming with no moves must not skip a waypoint");
        assertFalse(p.isReverseDirection());
        assertEquals(sea, dd.getLocation());
        assertSame(p, dd.getOrder());
    }

    @Test
    public void loadedTransportWakesOnce() {
        Unit dd = place(Type.DESTROYER, human, sea);
        Patrol p = patrol(dd, sea, far);
        Unit tr = place(Type.TRANSPORT, robot, justOutOfSight());
        for (int i = 0; i < 3; i++) {
            Unit inf = place(Type.INFANTRY, robot, tr.getLocation());
            tr.addCarried(inf);
        }

        dd.turn().attemptTurn();
        assertEquals(1, reports.size());
        assertSame(tr, reports.get(0).getEnemy(), "the card names the transport, not its cargo");

        dd.resumePatrol();
        dd.turn().attemptTurn();
        assertEquals(1, reports.size(), "cargo must not wake the patrol again");
        assertSame(p, dd.getOrder());
    }

    @Test
    public void enemySeenOnTheLastStepIsReported() {
        Unit dd = place(Type.DESTROYER, human, sea);
        patrol(dd, sea, far);
        // Only the final hex of this turn's path (far, 3 steps = all its moves) can see it
        Path path = dd.getPath(far);
        int range = Type.DESTROYER.getVisionDistance();
        Location hidden = null;
        for (int r = 1; r <= 5 && hidden == null; r++) {
            for (Location l : far.getRing(r)) {
                if (far.distance(l) != range || !game.getBoard().isWater(l) || !game.unitsAtLocation(l).isEmpty()) {
                    continue;
                }
                boolean seenEarlier = sea.distance(l) <= range;
                for (Location step : path) {
                    if (!step.equals(far) && step.distance(l) <= range) {
                        seenEarlier = true;
                    }
                }
                if (!seenEarlier) {
                    hidden = l;
                    break;
                }
            }
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(hidden != null, "a hex only the last step can see");
        place(Type.TRANSPORT, robot, hidden);

        dd.turn().attemptTurn();

        assertEquals(far, dd.getLocation());
        assertEquals(1, reports.size(), "sighting on the turn's last step");
        assertEquals(PatrolReportEvent.Kind.SIGHTING, reports.get(0).getKind());
    }

    @Test
    public void lowFuelAirLookoutHeadsHome() {
        City home = human.getCities().get(0);
        Location c = home.getLocation();
        Location out = hexAt(c, 4);
        Unit f = place(Type.FIGHTER, human, out);
        Patrol p = f.newPatrolOrder(List.of(c, hexAt(c, 9), c), Patrol.PatrolMode.LOOP);
        p.restoreState(1, false); // heading away from home
        // Only just enough fuel left to get home
        while (f.life().getFuel() > out.distance(c) + 1) {
            f.life().burnMoves();
            f.life().resetForTurn();
        }
        assertTrue(f.life().mustLand());
        f.assignOrder(p);

        f.turn().attemptTurn();

        assertFalse(f.isDead(), "a lookout that must land should head home, not fly on");
        assertTrue(f.getLocation().distance(c) < out.distance(c), "moved toward home: " + f.getLocation());
    }
}
