package com.developingstorm.games.sad.brain.commit;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CommitmentRulesTest {

    /** Hand-controlled world for rule tests. */
    static class FakeContext implements CommitmentContext {
        int turn = 1;
        final Set<Location> owned = new HashSet<>();
        final Map<Location, Integer> distance = new HashMap<>();
        final Set<Location> loadingPoints = new HashSet<>();
        final Set<Location> enemyNear = new HashSet<>();
        final Set<Unit> inOperation = new HashSet<>();

        public int currentTurn() { return turn; }
        public boolean ownedByMe(Location city) { return owned.contains(city); }
        public int pathDistance(Unit u, Location target) { return distance.getOrDefault(target, -1); }
        public boolean isLoadingPoint(Location l) { return loadingPoints.contains(l); }
        public boolean enemyWithin(Location l, int dist) { return enemyNear.contains(l); }
        public boolean assignedToOperation(Unit u) { return inOperation.contains(u); }
    }

    private static long nextId = 1;
    private Player owner;
    private FakeContext ctx;
    private CommitmentRules rules;
    private CommitmentBook book;
    private Location city;

    @BeforeEach
    public void setUp() {
        LocationMap.init(30, 30);
        owner = new Player("A", 1);
        ctx = new FakeContext();
        rules = new CommitmentRules();
        book = new CommitmentBook();
        city = Location.get(10, 10);
    }

    private Unit unit(Type t) {
        return Unit.createForTest(nextId++, t, owner, Location.get(5, 5));
    }

    private Commitment commit(Unit u, CommitmentKind k, Location target) {
        return book.commit(u, k, target, ctx.turn);
    }

    @Test
    public void captureCompletesWhenOwned() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.CAPTURE_CITY, city);
        ctx.distance.put(city, 4);
        ctx.owned.add(city);
        assertEquals(CommitmentStatus.COMPLETE, rules.status(c, ctx));
    }

    @Test
    public void captureTargetTakenByEnemyStaysActive() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.CAPTURE_CITY, city);
        ctx.distance.put(city, 4);
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
    }

    @Test
    public void noProgressExpires() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.CAPTURE_CITY, city);
        ctx.distance.put(city, 5);
        for (int t = 1; t <= 3; t++) {
            ctx.turn = t;
            assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx), "turn " + t);
        }
        ctx.turn = 4;
        assertEquals(CommitmentStatus.INVALID, rules.status(c, ctx));
    }

    @Test
    public void progressResetsExpiry() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.CAPTURE_CITY, city);
        ctx.distance.put(city, 5);
        rules.status(c, ctx);
        ctx.turn = 3;
        ctx.distance.put(city, 4);
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        ctx.turn = 5;
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        ctx.turn = 6;
        assertEquals(CommitmentStatus.INVALID, rules.status(c, ctx));
    }

    @Test
    public void noPathInvalid() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.CAPTURE_CITY, city);
        assertEquals(CommitmentStatus.INVALID, rules.status(c, ctx));
    }

    @Test
    public void defendCompletesAfterTwoQuietTurns() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.DEFEND, city);
        ctx.owned.add(city);
        ctx.enemyNear.add(city);
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        ctx.enemyNear.clear();
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        assertEquals(CommitmentStatus.COMPLETE, rules.status(c, ctx));
    }

    @Test
    public void defendQuietCountResetsWhenEnemyReturns() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.DEFEND, city);
        ctx.owned.add(city);
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        ctx.enemyNear.add(city);
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        ctx.enemyNear.clear();
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        assertEquals(CommitmentStatus.COMPLETE, rules.status(c, ctx));
    }

    @Test
    public void defendInvalidWhenCityLost() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.DEFEND, city);
        assertEquals(CommitmentStatus.INVALID, rules.status(c, ctx));
    }

    @Test
    public void stageCompletesWhenCarried() {
        Unit inf = unit(Type.INFANTRY);
        Location dock = Location.get(3, 3);
        ctx.loadingPoints.add(dock);
        Commitment c = commit(inf, CommitmentKind.STAGE, dock);
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        inf.onboard = unit(Type.TRANSPORT);
        assertEquals(CommitmentStatus.COMPLETE, rules.status(c, ctx));
    }

    @Test
    public void stageInvalidWhenNoLongerLoadingPoint() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.STAGE, Location.get(3, 3));
        assertEquals(CommitmentStatus.INVALID, rules.status(c, ctx));
    }

    @Test
    public void operationCompleteWhenUnassigned() {
        Unit inf = unit(Type.INFANTRY);
        Commitment c = commit(inf, CommitmentKind.OPERATION, null);
        ctx.inOperation.add(inf);
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
        ctx.inOperation.clear();
        assertEquals(CommitmentStatus.COMPLETE, rules.status(c, ctx));
    }

    @Test
    public void exploreStaysActive() {
        Commitment c = commit(unit(Type.INFANTRY), CommitmentKind.EXPLORE, null);
        assertEquals(CommitmentStatus.ACTIVE, rules.status(c, ctx));
    }

    @Test
    public void kindPriorityOrder() {
        CommitmentKind[] order = {
            CommitmentKind.OPERATION, CommitmentKind.DEFEND, CommitmentKind.CAPTURE_CITY,
            CommitmentKind.STAGE, CommitmentKind.EXPLORE
        };
        for (int i = 0; i < order.length; i++) {
            assertFalse(order[i].outranks(order[i]));
            for (int j = i + 1; j < order.length; j++) {
                assertTrue(order[i].outranks(order[j]), order[i] + " > " + order[j]);
                assertFalse(order[j].outranks(order[i]));
            }
        }
    }

    @Test
    public void bookReplacesAndReleases() {
        Unit inf = unit(Type.INFANTRY);
        commit(inf, CommitmentKind.EXPLORE, null);
        commit(inf, CommitmentKind.CAPTURE_CITY, city);
        assertEquals(CommitmentKind.CAPTURE_CITY, book.get(inf).kind);
        assertEquals(1, book.size());
        book.release(inf, "test");
        assertNull(book.get(inf));
    }

    @Test
    public void bookPrunesDeadUnits() {
        Unit alive = unit(Type.INFANTRY);
        Unit dead = unit(Type.INFANTRY);
        commit(alive, CommitmentKind.EXPLORE, null);
        commit(dead, CommitmentKind.EXPLORE, null);
        dead.life().kill();
        book.pruneDead();
        assertNotNull(book.get(alive));
        assertNull(book.get(dead));
    }
}
