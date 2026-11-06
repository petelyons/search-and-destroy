package com.developingstorm.games.sad.fx;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.CombatResult;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.events.PatrolReportEvent;

/**
 * One card in the Reports panel: a battle, or a patrol sighting / blocked route.
 * Left is the attacker (battles) or the patrolling unit; right is the defender or the enemy seen.
 */
public record ReportEntry(
    Kind kind,
    int turn,
    Location location,
    Side left,
    Side right,
    Unit patroller,
    boolean humanAttacked,
    boolean attackerWon
) {

    public enum Kind {
        BATTLE_WON,
        BATTLE_LOST,
        SIGHTING,
        BLOCKED;

        public boolean isPatrol() {
            return this == SIGHTING || this == BLOCKED;
        }
    }

    /** One unit on a card. {@code hitFraction} is remaining health after a battle, null for patrol cards. */
    public record Side(String name, Type type, Player owner, Double hitFraction) {}

    public static ReportEntry battle(CombatResult r, int turn, Player human) {
        boolean humanAttacked = r.getAttackerOwner() == human;
        boolean humanWon = r.attackerWon() == humanAttacked;
        Side attacker = new Side(r.getAttackerName(), r.getAttackerTypeEnum(), r.getAttackerOwner(),
            fraction(r.getAttackerFinalHits(), r.getAttackerMaxHits()));
        Side defender = new Side(r.getDefenderName(), r.getDefenderTypeEnum(), r.getDefenderOwner(),
            fraction(r.getDefenderFinalHits(), r.getDefenderMaxHits()));
        return new ReportEntry(humanWon ? Kind.BATTLE_WON : Kind.BATTLE_LOST, turn, r.getBattleLocation(),
            attacker, defender, null, humanAttacked, r.attackerWon());
    }

    public static ReportEntry patrol(PatrolReportEvent e) {
        Unit p = e.getPatroller();
        Unit enemy = e.getEnemy();
        Side left = new Side(p.name, p.getType(), p.getOwner(), null);
        Side right = enemy == null ? null : new Side(enemy.name, enemy.getType(), enemy.getOwner(), null);
        Kind kind = e.getKind() == PatrolReportEvent.Kind.SIGHTING ? Kind.SIGHTING : Kind.BLOCKED;
        return new ReportEntry(kind, e.getTurn(), e.getLocation(), left, right, p, false, false);
    }

    private static double fraction(int hits, int max) {
        return max <= 0 ? 0.0 : Math.max(0, hits) / (double) max;
    }
}
