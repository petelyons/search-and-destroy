package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.util.RandomUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PatrolLookoutTest {

    private Game game;
    private Player human;
    private Robot robot;
    private Location sea;

    @BeforeEach
    public void setUp() {
        RandomUtil.setSeed(20261106L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        human = new Player("Human", 1);
        robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        game = new Game(new Player[] {human, robot}, map, new MockSaDBoardContext());
        sea = openWater(game, 4, null);
    }

    /** A water hex whose rings up to {@code radius} are empty water, far from every city and from {@code awayFrom}. */
    static Location openWater(Game game, int radius, Location awayFrom) {
        for (int x = radius; x < 50 - radius; x++) {
            for (int y = radius; y < 50 - radius; y++) {
                Location l = Location.get(x, y);
                if (awayFrom != null && l.distance(awayFrom) < 15) {
                    continue;
                }
                boolean ok = game.getBoard().isWater(l) && game.unitsAtLocation(l).isEmpty();
                for (int r = 1; ok && r <= radius; r++) {
                    for (Location n : l.getRing(r)) {
                        ok &= game.getBoard().isWater(n) && game.unitsAtLocation(n).isEmpty();
                    }
                }
                for (City c : game.getBoard().getCities()) {
                    ok &= c.getLocation().distance(l) > 6;
                }
                if (ok) {
                    return l;
                }
            }
        }
        throw new SaDException("no open water");
    }

    private Unit place(Type t, Player owner, Location loc) {
        Unit u = game.createUnit(t, owner, loc);
        owner.addUnit(u);
        return u;
    }

    /** A hex exactly {@code d} away from {@code from}. */
    private static Location at(Location from, int d) {
        for (Location l : from.getRing(d)) {
            if (from.distance(l) == d) {
                return l;
            }
        }
        throw new SaDException("no hex at distance " + d);
    }

    @Test
    public void seesEnemyInRange() {
        Unit dd = place(Type.DESTROYER, human, sea);
        int range = Type.DESTROYER.getVisionDistance();
        Unit tr = place(Type.TRANSPORT, robot, at(sea, range));
        assertSame(tr, PatrolLookout.unreportedEnemyInSight(game, dd));
    }

    @Test
    public void ignoresOutOfRange() {
        Unit dd = place(Type.DESTROYER, human, sea);
        place(Type.TRANSPORT, robot, at(sea, Type.DESTROYER.getVisionDistance() + 1));
        assertNull(PatrolLookout.unreportedEnemyInSight(game, dd));
    }

    @Test
    public void submarineHiddenFromSurfaceVision() {
        Unit sub = place(Type.SUBMARINE, robot, at(sea, 1));
        Unit tr = place(Type.TRANSPORT, human, sea);
        assertNull(PatrolLookout.unreportedEnemyInSight(game, tr), "transport has SURFACE vision");
        game.killUnit(tr);
        Unit dd = place(Type.DESTROYER, human, sea);
        assertSame(sub, PatrolLookout.unreportedEnemyInSight(game, dd), "destroyer has COMPLETE vision");
    }

    @Test
    public void reportedIsIgnored() {
        Unit dd = place(Type.DESTROYER, human, sea);
        Unit tr = place(Type.TRANSPORT, robot, at(sea, 1));
        human.markReported(tr);
        assertTrue(human.isReported(tr));
        assertNull(PatrolLookout.unreportedEnemyInSight(game, dd));
    }

    @Test
    public void forgottenWhenOutOfSight() {
        place(Type.DESTROYER, human, sea);
        // Reported earlier, but now far away and out of sight
        Unit tr = place(Type.TRANSPORT, robot, openWater(game, 1, sea));
        human.markReported(tr);

        human.startNewTurn();

        assertFalse(human.isReported(tr));
    }

    @Test
    public void stillReportedWhileVisible() {
        place(Type.DESTROYER, human, sea);
        Unit tr = place(Type.TRANSPORT, robot, at(sea, 1));
        human.markReported(tr);

        human.startNewTurn();

        assertTrue(human.isReported(tr));
    }
}
