package com.developingstorm.games.sad.fx;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.CombatResult;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.events.PatrolReportEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReportTextTest {

    private Player human;
    private Player enemy;
    private long nextId = 1;

    @BeforeEach
    public void setUp() {
        LocationMap.init(50, 50);
        human = new Player("Human", 1);
        enemy = new Player("Robot", 2);
    }

    private Unit unit(Type t, Player owner, String name) {
        Unit u = Unit.createForTest(nextId++, t, owner, Location.get(5, 6));
        u.name = name;
        return u;
    }

    private ReportEntry battle(Unit attacker, Unit defender, boolean attackerWon) {
        CombatResult r = new CombatResult(attacker, attacker.life().hits, defender, defender.life().hits, attackerWon);
        return ReportEntry.battle(r, 12, human);
    }

    @Test
    public void humanAttackWonVerbsByDefenderTravel() {
        ReportEntry land = battle(unit(Type.ARMOR, human, "Iron Wolves"), unit(Type.INFANTRY, enemy, "3rd Foot"), true);
        assertEquals(ReportEntry.Kind.BATTLE_WON, land.kind());
        assertEquals("Turn 12 · You attacked", ReportText.header(land));
        assertEquals("VICTORY", ReportText.badge(land));
        assertEquals("destroyed", ReportText.verb(land));

        assertEquals("sank", ReportText.verb(battle(unit(Type.BATTLESHIP, human, "Iowa"), unit(Type.TRANSPORT, enemy, "Gull"), true)));
        assertEquals("shot down", ReportText.verb(battle(unit(Type.FIGHTER, human, "Hawk"), unit(Type.BOMBER, enemy, "Raven"), true)));
    }

    @Test
    public void attackerLostIsBeatenBy() {
        ReportEntry e = battle(unit(Type.INFANTRY, human, "1st Foot"), unit(Type.ARMOR, enemy, "Panzer"), false);
        assertEquals(ReportEntry.Kind.BATTLE_LOST, e.kind());
        assertEquals("LOST", ReportText.badge(e));
        assertEquals("beaten by", ReportText.verb(e));
    }

    @Test
    public void humanDefenderWinning() {
        ReportEntry e = battle(unit(Type.CRUISER, enemy, "Resolute"), unit(Type.DESTROYER, human, "Valiant"), false);
        assertEquals(ReportEntry.Kind.BATTLE_WON, e.kind());
        assertEquals("VICTORY", ReportText.badge(e));
        assertEquals("Turn 12 · Enemy attacked", ReportText.header(e));
    }

    @Test
    public void sightingCard() {
        Unit dd = unit(Type.DESTROYER, human, "Valiant");
        Unit tr = unit(Type.TRANSPORT, enemy, "Black Gull");
        ReportEntry e = ReportEntry.patrol(new PatrolReportEvent(PatrolReportEvent.Kind.SIGHTING, dd, tr, Location.get(31, 8), 64));
        assertEquals(ReportEntry.Kind.SIGHTING, e.kind());
        assertEquals("Turn 64 · Patrol sighting · (31,8)", ReportText.header(e));
        assertEquals("SPOTTED", ReportText.badge(e));
        assertEquals("spotted", ReportText.verb(e));
        assertSame(dd, e.patroller());
        assertEquals("Black Gull", e.right().name());
    }

    @Test
    public void blockedCardWithoutEnemy() {
        Unit dd = unit(Type.DESTROYER, human, "Valiant");
        ReportEntry e = ReportEntry.patrol(new PatrolReportEvent(PatrolReportEvent.Kind.BLOCKED, dd, null, Location.get(3, 4), 7));
        assertEquals("Turn 7 · Route blocked · (3,4)", ReportText.header(e));
        assertEquals("BLOCKED", ReportText.badge(e));
        assertEquals("blocked", ReportText.verb(e));
        assertNull(e.right());
    }

    @Test
    public void displayNameFallsBackToType() {
        assertEquals(Type.DESTROYER.getName(), ReportText.displayName(null, Type.DESTROYER));
        assertEquals(Type.DESTROYER.getName(), ReportText.displayName("  ", Type.DESTROYER));
        assertEquals(Type.DESTROYER.getName(), ReportText.displayName("Unit #42", Type.DESTROYER));
        assertEquals("Valiant", ReportText.displayName("Valiant", Type.DESTROYER));
    }
}
