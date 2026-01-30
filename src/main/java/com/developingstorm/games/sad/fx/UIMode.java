package com.developingstorm.games.sad.fx;

/**
 * Enumeration of UI interaction modes for JavaFX.
 * Matches the Swing UIMode but for JavaFX-specific implementation.
 */
public enum UIMode {
    /**
     * Default game mode - select units, move units, activate hexes.
     */
    GAME,

    /**
     * Path setting mode - set city production paths (land, sea, air).
     */
    PATHS,

    /**
     * Explore mode - set exploration paths for units.
     */
    EXPLORE,

    /**
     * Patrol mode - set patrol routes for ships.
     */
    PATROL,

    /**
     * Attack mode - select attack targets for units.
     */
    ATTACK,

    /**
     * Escort mode - set up ship escort relationships.
     */
    ESCORT
}
