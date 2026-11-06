package com.developingstorm.games.sad.brain;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MockSaDBoardContext;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.commit.Commitment;
import com.developingstorm.games.sad.brain.commit.CommitmentBook;
import com.developingstorm.games.sad.brain.commit.CommitmentKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RobotBrainCommitmentTest {

    private Game game;
    private Robot a;
    private RobotBrain brain;
    private City home;

    @BeforeEach
    public void setUp() {
        // City placement is random; fix it so assumptions below hold on every run
        com.developingstorm.util.RandomUtil.setSeed(20260931L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        a = new Robot("A", 1);
        brain = new RobotBrain(a);
        a.setBrain(brain);
        Robot b = new Robot("B", 2);
        b.setBrain(new RobotBrain(b));
        game = new Game(new Player[] {a, b}, map, new MockSaDBoardContext());
        home = a.getCities().get(0);
    }

    private Unit create(Type t, Location loc) {
        Unit u = game.createUnit(t, a, loc);
        a.addUnit(u);
        return u;
    }

    private CommitmentBook book() {
        return brain.getStrategyMemory().getCommitments();
    }

    /** Nearest unowned city on the unit's continent that it can path to. */
    private City reachableUnownedCity(Unit u) {
        City best = null;
        int bestLen = Integer.MAX_VALUE;
        for (City c : game.getBoard().getCities()) {
            if (c.getOwner() != null || c.getContinent() != u.getContinent()) {
                continue;
            }
            Path p = u.getPath(c.getLocation());
            if (p != null && p.length() > 0 && p.length() < bestLen) {
                best = c;
                bestLen = p.length();
            }
        }
        return best;
    }

    private Location waterNextTo(Location land) {
        for (Location l : land.getRing(1)) {
            if (game.getBoard().isWater(l) && game.unitsAtLocation(l).isEmpty()) {
                return l;
            }
        }
        return null;
    }

    @Test
    public void activeCaptureKeepsOrderAcrossTurns() {
        Unit inf = create(Type.INFANTRY, home.getLocation());
        City target = reachableUnownedCity(inf);
        assumeTrue(target != null, "map has a reachable unowned city");
        book().commit(inf, CommitmentKind.CAPTURE_CITY, target.getLocation(), game.getTurn());
        Order move = inf.newMoveOrder(target.getLocation());
        inf.assignOrder(move);

        brain.startNewTurn();

        assumeFalse(brain.getOperationsCoordinator().isAssigned(inf), "not drafted into an operation");
        assertSame(move, inf.getOrder(), "committed unit keeps its order");
        assertEquals(CommitmentKind.CAPTURE_CITY, book().get(inf).kind);
    }

    @Test
    public void completedCommitmentIsReplanned() {
        Unit inf = create(Type.INFANTRY, home.getLocation());
        City target = reachableUnownedCity(inf);
        assumeTrue(target != null);
        book().commit(inf, CommitmentKind.CAPTURE_CITY, target.getLocation(), game.getTurn());
        target.setOwner(a);

        brain.startNewTurn();

        Commitment c = book().get(inf);
        assertFalse(
            c != null && c.kind == CommitmentKind.CAPTURE_CITY && target.getLocation().equals(c.target),
            "captured target must not stay committed"
        );
    }

    @Test
    public void exploreUpgradesToCityOpportunity() {
        Unit inf = create(Type.INFANTRY, home.getLocation());
        City target = reachableUnownedCity(inf);
        assumeTrue(target != null);
        a.adjustVisibility(target);
        book().commit(inf, CommitmentKind.EXPLORE, null, game.getTurn());
        inf.assignOrder(inf.newExploreOrder());

        brain.startNewTurn();

        assumeFalse(brain.getOperationsCoordinator().isAssigned(inf));
        assertEquals(CommitmentKind.CAPTURE_CITY, book().get(inf).kind);
    }

    @Test
    public void navyUnitsHaveNoCommitment() {
        Location sea = waterNextTo(home.getLocation());
        assumeTrue(sea != null);
        Unit dd = create(Type.DESTROYER, sea);

        brain.startNewTurn();

        assertNull(book().get(dd));
    }

    @Test
    public void landUnitGetsCommitmentOrSkips() {
        Unit inf = create(Type.INFANTRY, home.getLocation());

        brain.startNewTurn();

        Order o = inf.getOrder();
        assertTrue(
            book().get(inf) != null || (o != null && o.getType() == com.developingstorm.games.sad.OrderType.SKIPTURN),
            "land unit should commit to something, or skip"
        );
    }

    @Test
    public void landTargetsStayOnTheUnitsContinent() {
        Unit inf = create(Type.INFANTRY, home.getLocation());
        brain.startNewTurn();

        City best = brain.getBattleplan().getBestCityTarget(inf);

        assertTrue(best == null || best.getContinent() == inf.getContinent(),
            "land unit targeted " + best + " on another continent");
    }

    /** Coordinator that claims every unit but has no order for it yet (operation still planning). */
    private OperationsCoordinator planningOnlyCoordinator() {
        return new OperationsCoordinator(a, brain.getStrategyMemory()) {
            @Override
            public boolean isAssigned(Unit unit) {
                return true;
            }

            @Override
            public Order getOperationOrder(Unit unit) {
                return null;
            }
        };
    }

    /** Coordinator that never assigns anyone. */
    private OperationsCoordinator idleCoordinator() {
        return new OperationsCoordinator(a, brain.getStrategyMemory()) {
            @Override
            public boolean isAssigned(Unit unit) {
                return false;
            }
        };
    }

    @Test
    public void operationWithoutOrderDoesNotPreempt() {
        Unit inf = create(Type.INFANTRY, home.getLocation());
        brain.startNewTurn();
        OperationsCoordinator coord = planningOnlyCoordinator();
        LandCaptain land = new LandCaptain(new General(brain.getBattleplan(), coord), brain.getBattleplan(), coord);
        Commitment stage = book().commit(inf, CommitmentKind.STAGE, home.getLocation(), game.getTurn());

        assertNotEquals("operation assignment", land.preemption(inf, stage));
    }

    @Test
    public void exploresHomeContinentBeforeStaging() {
        Unit inf = create(Type.INFANTRY, home.getLocation());
        // Nothing left to capture at home: every remaining target is overseas
        for (City c : game.getBoard().getCities()) {
            if (c.getContinent() == inf.getContinent() && c.getOwner() == null) {
                c.setOwner(a);
            }
        }
        home.produce(Type.TRANSPORT); // makes home a loading point
        brain.startNewTurn();
        assumeFalse(a.getFrontier(inf).isEmpty(), "home continent still has frontier");
        assumeFalse(brain.getBattleplan().getLoadingPoints().isEmpty(), "a loading point exists");
        OperationsCoordinator coord = idleCoordinator();
        LandCaptain land = new LandCaptain(new General(brain.getBattleplan(), coord), brain.getBattleplan(), coord);
        book().release(inf, "test reset");

        land.plan(inf);

        Commitment c = book().get(inf);
        assertNotNull(c);
        assertNotEquals(CommitmentKind.STAGE, c.kind, "should explore or capture before shipping out");
    }

    @Test
    public void carriedUnitStillHasALandingTarget() {
        brain.startNewTurn();
        // A water hex right next to an unowned coastal city
        Location sea = null;
        City shoreCity = null;
        for (City c : game.getBoard().getCities()) {
            if (c.getOwner() != null || !c.isCoastal()) {
                continue;
            }
            for (Location l : c.getLocation().getRing(1)) {
                if (game.getBoard().isWater(l) && game.unitsAtLocation(l).isEmpty()) {
                    sea = l;
                    shoreCity = c;
                    break;
                }
            }
            if (sea != null) {
                break;
            }
        }
        assumeTrue(sea != null);
        Unit tr = create(Type.TRANSPORT, sea);
        Unit inf = create(Type.INFANTRY, sea);
        tr.addCarried(inf);
        a.adjustVisibility(shoreCity);

        assertNotNull(brain.getBattleplan().getBestCityTarget(inf),
            "a unit aboard a transport should still pick a landing target");
    }

    @Test
    public void stuckUnitDoesNotRecommitToTheSameCity() {
        Unit inf = create(Type.INFANTRY, home.getLocation());
        City target = reachableUnownedCity(inf);
        assumeTrue(target != null);
        book().commit(inf, CommitmentKind.CAPTURE_CITY, target.getLocation(), game.getTurn());
        inf.assignOrder(inf.newMoveOrder(target.getLocation()));

        // The unit never moves (turns aren't played), so it makes no progress
        for (int i = 0; i < 5; i++) {
            game.turn++;
            brain.startNewTurn();
        }

        assumeFalse(brain.getOperationsCoordinator().isAssigned(inf));
        Commitment c = book().get(inf);
        assertFalse(
            c != null && c.kind == CommitmentKind.CAPTURE_CITY && target.getLocation().equals(c.target),
            "abandoned target must not be picked again right away"
        );
    }
}
