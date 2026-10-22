package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.orders.Unload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NextUnitSelectionTest {

    private Game game;
    private Player human;
    private Unit transport;
    private Unit first;
    private Unit second;
    private Unit ashore;

    @BeforeEach
    public void setUp() {
        com.developingstorm.util.RandomUtil.setSeed(20261022L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        human = new Player("Human", 1);
        Robot robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        game = new Game(new Player[] {human, robot}, map, new MockSaDBoardContext());

        Location coast = coastalWater();
        transport = create(Type.TRANSPORT, coast);
        first = create(Type.INFANTRY, coast);
        second = create(Type.INFANTRY, coast);
        transport.addCarried(first);
        transport.addCarried(second);
        ashore = create(Type.INFANTRY, freeLandAwayFrom(coast));
        // Start the turn as the game would; new units begin with their turn done
        for (Unit u : human.getUnits()) {
            u.turn().beginTurn();
        }
        new Unload(game, transport).executeInternal();
        transport.turn().completeTurn();
    }

    private Unit create(Type t, Location loc) {
        Unit u = game.createUnit(t, human, loc);
        human.addUnit(u);
        return u;
    }

    private Location freeLandAwayFrom(Location coast) {
        for (int x = 1; x < 49; x++) {
            for (int y = 1; y < 49; y++) {
                Location l = Location.get(x, y);
                if (game.getBoard().isLand(l) && !game.getBoard().isCity(l)
                    && game.unitsAtLocation(l).isEmpty() && l.distance(coast) > 3) {
                    return l;
                }
            }
        }
        throw new SaDException("no land");
    }

    private Location coastalWater() {
        for (int x = 1; x < 49; x++) {
            for (int y = 1; y < 49; y++) {
                Location l = Location.get(x, y);
                if (!game.getBoard().isWater(l) || !game.unitsAtLocation(l).isEmpty()) {
                    continue;
                }
                int freeLand = 0;
                for (Location n : l.getRing(1)) {
                    if (game.getBoard().isLand(n) && !game.getBoard().isCity(n) && game.unitsAtLocation(n).isEmpty()) {
                        freeLand++;
                    }
                }
                if (freeLand >= 2) {
                    return l;
                }
            }
        }
        throw new SaDException("no coast");
    }

    @Test
    public void skippingACarriedUnitMovesOnToTheNextUnit() {
        game.selectUnit(first);
        first.assignOrder(first.newSkipTurn());
        first.turn().attemptTurn(); // also skips the rest of the cargo

        assertSame(ashore, human.chooseUnitNeedingOrders());
    }

    @Test
    public void deadSelectedUnitIsNotKept() {
        game.selectUnit(first);
        game.killUnit(first);

        Unit next = human.chooseUnitNeedingOrders();
        assertNotSame(first, next);
        assertFalse(next.isDead());
    }

    @Test
    public void playableSelectedUnitIsKept() {
        game.selectUnit(second);
        assertSame(second, human.chooseUnitNeedingOrders());
    }
}

