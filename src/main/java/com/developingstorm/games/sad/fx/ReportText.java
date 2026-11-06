package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;

/**
 * Wording for Reports panel cards.
 */
public final class ReportText {

    private ReportText() {}

    public static String header(ReportEntry e) {
        String turn = "Turn " + e.turn();
        switch (e.kind()) {
            case SIGHTING:
                return turn + " · Patrol sighting · (" + e.location() + ")";
            case BLOCKED:
                return turn + " · Route blocked · (" + e.location() + ")";
            default:
                return turn + (e.humanAttacked() ? " · You attacked" : " · Enemy attacked");
        }
    }

    public static String badge(ReportEntry e) {
        switch (e.kind()) {
            case BATTLE_WON:
                return "VICTORY";
            case BATTLE_LOST:
                return "LOST";
            case SIGHTING:
                return "SPOTTED";
            default:
                return "BLOCKED";
        }
    }

    /** The action shown between the two units. */
    public static String verb(ReportEntry e) {
        switch (e.kind()) {
            case SIGHTING:
                return "spotted";
            case BLOCKED:
                return "blocked";
            default:
                if (!e.attackerWon()) {
                    return "beaten by";
                }
                Travel t = e.right().type().getTravel();
                return t == Travel.SEA ? "sank" : t == Travel.AIR ? "shot down" : "destroyed";
        }
    }

    /** The unit's name, or its type when it has none (combat results label unnamed units "Unit #id"). */
    public static String displayName(String name, Type type) {
        if (name == null || name.isBlank() || name.startsWith("Unit #")) {
            return type.getName();
        }
        return name;
    }
}
