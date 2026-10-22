package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.events.GameEvent;
import com.developingstorm.games.sad.events.GameEventListener;
import com.developingstorm.games.sad.events.GameEventType;
import com.developingstorm.games.sad.events.MoveRefusedEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MoveRefusedEventTest {

    private Game game;
    private Player human;
    private Robot robot;
    private final List<MoveRefusedEvent> events = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        com.developingstorm.util.RandomUtil.setSeed(20261001L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        human = new Player("Human", 1);
        robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        game = new Game(new Player[] {human, robot}, map, new MockSaDBoardContext());
        game.getEventBus().addListener(new GameEventListener() {
            @Override
            public void onGameEvent(GameEvent event) {
                events.add((MoveRefusedEvent) event);
            }

            @Override
            public GameEventType[] getInterestedEventTypes() {
                return new GameEventType[] { GameEventType.MOVE_REFUSED };
            }
        });
    }

    /** A land hex next to water, with the water hex: [land, water]. */
    private Location[] shore() {
        for (int x = 1; x < 49; x++) {
            for (int y = 1; y < 49; y++) {
                Location l = Location.get(x, y);
                if (!game.getBoard().isLand(l) || game.getBoard().isCity(l) || !game.unitsAtLocation(l).isEmpty()) {
                    continue;
                }
                for (Location n : l.getRing(1)) {
                    if (game.getBoard().isWater(n)) {
                        return new Location[] { l, n };
                    }
                }
            }
        }
        throw new SaDException("no shore");
    }

    private Unit infantry(Player owner, Location at) {
        Unit u = game.createUnit(Type.INFANTRY, owner, at);
        owner.addUnit(u);
        return u;
    }

    @Test
    public void humanMoveIntoWaterIsReportedWithTheTargetHex() {
        Location[] s = shore();
        Unit inf = infantry(human, s[0]);
        inf.assignOrder(inf.newMoveOrder(s[1]));

        inf.turn().attemptTurn();

        assertEquals(1, events.size());
        assertSame(inf, events.get(0).getUnit());
        assertEquals(s[1], events.get(0).getTarget());
    }

    @Test
    public void robotRefusalsAreNotReported() {
        Location[] s = shore();
        Unit inf = infantry(robot, s[0]);
        inf.assignOrder(inf.newMoveOrder(s[1]));

        inf.turn().attemptTurn();

        assertTrue(events.isEmpty());
    }

    @Test
    public void successfulMoveIsNotReported() {
        Location[] s = shore();
        Unit inf = infantry(human, s[0]);
        Location landStep = null;
        for (Location n : s[0].getRing(1)) {
            if (game.getBoard().isLand(n) && !game.getBoard().isCity(n) && game.unitsAtLocation(n).isEmpty()) {
                landStep = n;
                break;
            }
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(landStep != null);
        inf.assignOrder(inf.newMoveOrder(landStep));

        inf.turn().attemptTurn();

        assertTrue(events.isEmpty());
        assertEquals(landStep, inf.getLocation());
    }

    @Test
    public void arrivingWithMovesLeftIsNotARefusal() {
        // Open water: a hex whose 2-ring is all water, so a 2-step path exists
        Location from = null;
        Location to = null;
        for (int x = 3; x < 47 && from == null; x++) {
            for (int y = 3; y < 47; y++) {
                Location l = Location.get(x, y);
                boolean open = game.getBoard().isWater(l);
                for (Location n : l.getRing(2)) {
                    open &= game.getBoard().isWater(n) && game.unitsAtLocation(n).isEmpty();
                }
                if (open) {
                    from = l;
                    for (Location n : l.getRing(2)) {
                        if (l.distance(n) == 2) {
                            to = n;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        org.junit.jupiter.api.Assumptions.assumeTrue(from != null && to != null);
        Unit dd = game.createUnit(Type.DESTROYER, human, from);
        human.addUnit(dd);
        dd.turn().beginTurn();
        dd.assignOrder(dd.newMoveOrder(to));

        dd.turn().attemptTurn();

        assertEquals(to, dd.getLocation());
        assertTrue(events.isEmpty(), "arrival must not flash a refusal X");
    }
}
