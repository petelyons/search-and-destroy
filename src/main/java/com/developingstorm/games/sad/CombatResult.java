package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;

/**
 * Captures the result of a combat encounter between two units.
 */
public class CombatResult {

    private final String attackerName;
    private final String attackerType;
    private final Player attackerOwner;
    private final int attackerIconIndex;
    private final int attackerInitialHits;
    private final int attackerFinalHits;
    private final int attackerMaxHits;

    private final String defenderName;
    private final String defenderType;
    private final Player defenderOwner;
    private final int defenderIconIndex;
    private final int defenderInitialHits;
    private final int defenderFinalHits;
    private final int defenderMaxHits;

    private final boolean attackerWon;
    private final Location battleLocation;

    public CombatResult(
        Unit attacker,
        int attackerInitialHits,
        Unit defender,
        int defenderInitialHits,
        boolean attackerWon
    ) {
        this.attackerName =
            attacker.name != null ? attacker.name : "Unit #" + attacker.id;
        this.attackerType = attacker.getType().toString();
        this.attackerOwner = attacker.getOwner();
        this.attackerIconIndex = attacker.getType().getIcon();
        this.attackerInitialHits = attackerInitialHits;
        this.attackerFinalHits = attacker.life().hits;
        this.attackerMaxHits = attacker.getType().getHits();

        this.defenderName =
            defender.name != null ? defender.name : "Unit #" + defender.id;
        this.defenderType = defender.getType().toString();
        this.defenderOwner = defender.getOwner();
        this.defenderIconIndex = defender.getType().getIcon();
        this.defenderInitialHits = defenderInitialHits;
        this.defenderFinalHits = defender.life().hits;
        this.defenderMaxHits = defender.getType().getHits();

        this.attackerWon = attackerWon;
        this.battleLocation = defender.getLocation();
    }

    public String getAttackerName() {
        return attackerName;
    }

    public String getAttackerType() {
        return attackerType;
    }

    public Player getAttackerOwner() {
        return attackerOwner;
    }

    public int getAttackerIconIndex() {
        return attackerIconIndex;
    }

    public int getAttackerInitialHits() {
        return attackerInitialHits;
    }

    public int getAttackerFinalHits() {
        return attackerFinalHits;
    }

    public int getAttackerMaxHits() {
        return attackerMaxHits;
    }

    public int getAttackerDamage() {
        return attackerInitialHits - attackerFinalHits;
    }

    public String getDefenderName() {
        return defenderName;
    }

    public String getDefenderType() {
        return defenderType;
    }

    public Player getDefenderOwner() {
        return defenderOwner;
    }

    public int getDefenderIconIndex() {
        return defenderIconIndex;
    }

    public int getDefenderInitialHits() {
        return defenderInitialHits;
    }

    public int getDefenderFinalHits() {
        return defenderFinalHits;
    }

    public int getDefenderMaxHits() {
        return defenderMaxHits;
    }

    public int getDefenderDamage() {
        return defenderInitialHits - defenderFinalHits;
    }

    public boolean attackerWon() {
        return attackerWon;
    }

    public Location getBattleLocation() {
        return battleLocation;
    }
}
