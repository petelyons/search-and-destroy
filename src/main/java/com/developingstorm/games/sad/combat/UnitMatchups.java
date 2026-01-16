package com.developingstorm.games.sad.combat;

import com.developingstorm.games.sad.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Central unit matchup system that determines combat effectiveness modifiers.
 * Based on Captain target priority lists that define which units are effective against which enemies.
 */
public class UnitMatchups {

    // Multiplier for primary target matchups (1.5x = 50% bonus damage)
    private static final double PRIMARY_TARGET_MULTIPLIER = 1.5;

    // Multiplier for secondary target matchups (1.2x = 20% bonus damage)
    private static final double SECONDARY_TARGET_MULTIPLIER = 1.2;

    // Multiplier for poor matchups (0.75x = 25% damage penalty)
    private static final double POOR_MATCHUP_MULTIPLIER = 0.75;

    // Multiplier for non-combat units (Transport, Cargo)
    private static final double NONCOMBAT_MULTIPLIER = 0.5;

    // Maps attacker type -> primary targets
    private static final Map<Type, Set<Type>> PRIMARY_TARGETS = new HashMap<>();

    // Maps attacker type -> secondary targets
    private static final Map<Type, Set<Type>> SECONDARY_TARGETS = new HashMap<>();

    static {
        // Fighter - effective against air units and ground forces
        // Source: FighterCaptain.java
        PRIMARY_TARGETS.put(Type.FIGHTER, setOf(
            Type.TRANSPORT, Type.ARMOR, Type.INFANTRY, Type.BOMBER, Type.CARGO, Type.FIGHTER
        ));
        SECONDARY_TARGETS.put(Type.FIGHTER, setOf(
            Type.DESTROYER, Type.CRUISER, Type.BATTLESHIP, Type.CARRIER
        ));

        // Bomber - effective against ground forces and transports
        // Source: BomberCaptain.java
        PRIMARY_TARGETS.put(Type.BOMBER, setOf(
            Type.TRANSPORT, Type.ARMOR, Type.INFANTRY
        ));
        SECONDARY_TARGETS.put(Type.BOMBER, setOf(
            Type.DESTROYER, Type.CRUISER, Type.BATTLESHIP, Type.CARRIER
        ));

        // Armor - effective against infantry
        // Source: ArmorCaptain.java (uses occupyLandStrategy)
        PRIMARY_TARGETS.put(Type.ARMOR, setOf(
            Type.INFANTRY
        ));
        SECONDARY_TARGETS.put(Type.ARMOR, setOf(
            Type.ARMOR, Type.TRANSPORT
        ));

        // Infantry - effective against other infantry
        // Source: InfantryCaptain.java (uses occupyLandStrategy)
        PRIMARY_TARGETS.put(Type.INFANTRY, setOf(
            Type.INFANTRY
        ));
        SECONDARY_TARGETS.put(Type.INFANTRY, setOf(
            Type.ARMOR, Type.TRANSPORT
        ));

        // Destroyer - effective against submarines and transports
        // Source: DestroyerCaptain.java
        PRIMARY_TARGETS.put(Type.DESTROYER, setOf(
            Type.TRANSPORT, Type.SUBMARINE
        ));
        SECONDARY_TARGETS.put(Type.DESTROYER, setOf(
            Type.DESTROYER, Type.BOMBER, Type.FIGHTER
        ));

        // Cruiser - effective against destroyers and submarines
        // Source: CruiserCaptain.java
        PRIMARY_TARGETS.put(Type.CRUISER, setOf(
            Type.TRANSPORT, Type.DESTROYER, Type.SUBMARINE
        ));
        SECONDARY_TARGETS.put(Type.CRUISER, setOf(
            Type.CRUISER, Type.BOMBER, Type.FIGHTER
        ));

        // Battleship - effective against large ships
        // Source: BattleshipCaptain.java
        PRIMARY_TARGETS.put(Type.BATTLESHIP, setOf(
            Type.TRANSPORT, Type.CARRIER, Type.BATTLESHIP
        ));
        SECONDARY_TARGETS.put(Type.BATTLESHIP, setOf(
            Type.DESTROYER, Type.CRUISER
        ));

        // Submarine - effective against large ships
        // Source: SubmarineCaptain.java
        PRIMARY_TARGETS.put(Type.SUBMARINE, setOf(
            Type.TRANSPORT, Type.CARRIER, Type.BATTLESHIP
        ));
        SECONDARY_TARGETS.put(Type.SUBMARINE, setOf(
            Type.CRUISER, Type.SUBMARINE
        ));

        // Carrier - non-combat role, but has minimal attack capability
        PRIMARY_TARGETS.put(Type.CARRIER, setOf());
        SECONDARY_TARGETS.put(Type.CARRIER, setOf(Type.TRANSPORT));

        // Transport and Cargo - non-combat units with no effective targets
        PRIMARY_TARGETS.put(Type.TRANSPORT, setOf());
        SECONDARY_TARGETS.put(Type.TRANSPORT, setOf());

        PRIMARY_TARGETS.put(Type.CARGO, setOf());
        SECONDARY_TARGETS.put(Type.CARGO, setOf());
    }

    /**
     * Helper method to create a Set from varargs.
     */
    private static Set<Type> setOf(Type... types) {
        Set<Type> set = new HashSet<>();
        for (Type type : types) {
            set.add(type);
        }
        return set;
    }

    /**
     * Calculates the effectiveness multiplier for attacker vs defender matchup.
     *
     * @param attacker The attacking unit type
     * @param defender The defending unit type
     * @return Multiplier to apply to base attack damage (1.5x for primary, 1.2x for secondary,
     *         0.75x for poor matchup, 0.5x for non-combat units)
     */
    public static double getEffectivenessMultiplier(Type attacker, Type defender) {
        // Transport and Cargo are weak non-combat units
        if (attacker == Type.TRANSPORT || attacker == Type.CARGO) {
            return NONCOMBAT_MULTIPLIER;
        }

        // Check primary targets
        Set<Type> primaryTargets = PRIMARY_TARGETS.get(attacker);
        if (primaryTargets != null && primaryTargets.contains(defender)) {
            return PRIMARY_TARGET_MULTIPLIER;
        }

        // Check secondary targets
        Set<Type> secondaryTargets = SECONDARY_TARGETS.get(attacker);
        if (secondaryTargets != null && secondaryTargets.contains(defender)) {
            return SECONDARY_TARGET_MULTIPLIER;
        }

        // Poor matchup - attacking a unit type not in primary or secondary lists
        return POOR_MATCHUP_MULTIPLIER;
    }

    /**
     * Gets a description of the matchup for logging purposes.
     *
     * @param attacker The attacking unit type
     * @param defender The defending unit type
     * @return Human-readable description like "primary target", "secondary target", etc.
     */
    public static String getMatchupDescription(Type attacker, Type defender) {
        if (attacker == Type.TRANSPORT || attacker == Type.CARGO) {
            return "non-combat unit";
        }

        Set<Type> primaryTargets = PRIMARY_TARGETS.get(attacker);
        if (primaryTargets != null && primaryTargets.contains(defender)) {
            return "primary target";
        }

        Set<Type> secondaryTargets = SECONDARY_TARGETS.get(attacker);
        if (secondaryTargets != null && secondaryTargets.contains(defender)) {
            return "secondary target";
        }

        return "poor matchup";
    }
}
