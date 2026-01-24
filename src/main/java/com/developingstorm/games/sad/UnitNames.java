package com.developingstorm.games.sad;

/**
 * Manages unit names with player-specific naming themes.
 * Land and air units use numbered designations with thematic prefixes.
 */
public class UnitNames {

    /**
     * Naming themes for different players
     */
    public enum Theme {
        CLASSICAL, // Roman/Greek inspired
        MODERN, // NATO/Modern military
        MEDIEVAL, // Fantasy/Medieval
        EASTERN, // Asian inspired
    }

    /**
     * Unit naming configuration for a theme
     */
    private static class ThemeNames {

        String infantryPrefix;
        String armorPrefix;
        String fighterPrefix;
        String bomberPrefix;
        String cargoPrefix;

        // Counters for each unit type
        int infantryCounter = 1;
        int armorCounter = 1;
        int fighterCounter = 1;
        int bomberCounter = 1;
        int cargoCounter = 1;
    }

    private static UnitNames instance = new UnitNames();

    private final java.util.Map<Theme, ThemeNames> themeConfigs;
    private final java.util.Map<Integer, Theme> playerThemes;

    private UnitNames() {
        themeConfigs = new java.util.HashMap<>();
        playerThemes = new java.util.HashMap<>();

        initializeThemes();
    }

    private void initializeThemes() {
        // Classical/Roman theme - Regiment/Panzer
        ThemeNames classical = new ThemeNames();
        classical.infantryPrefix = "Regiment";
        classical.armorPrefix = "Panzer";
        classical.fighterPrefix = "Squadron";
        classical.bomberPrefix = "Wing";
        classical.cargoPrefix = "Airlift";
        themeConfigs.put(Theme.CLASSICAL, classical);

        // Modern theme - Guards/Tanks
        ThemeNames modern = new ThemeNames();
        modern.infantryPrefix = "Guards";
        modern.armorPrefix = "Tanks";
        modern.fighterPrefix = "Squadron";
        modern.bomberPrefix = "Wing";
        modern.cargoPrefix = "Airlift";
        themeConfigs.put(Theme.MODERN, modern);

        // Medieval theme - Legion/Mechs
        ThemeNames medieval = new ThemeNames();
        medieval.infantryPrefix = "Legion";
        medieval.armorPrefix = "Mechs";
        medieval.fighterPrefix = "Squadron";
        medieval.bomberPrefix = "Wing";
        medieval.cargoPrefix = "Airlift";
        themeConfigs.put(Theme.MEDIEVAL, medieval);

        // Eastern theme - Battalion/Armor
        ThemeNames eastern = new ThemeNames();
        eastern.infantryPrefix = "Battalion";
        eastern.armorPrefix = "Armor";
        eastern.fighterPrefix = "Squadron";
        eastern.bomberPrefix = "Wing";
        eastern.cargoPrefix = "Airlift";
        themeConfigs.put(Theme.EASTERN, eastern);
    }

    private String allocateName(Theme theme, Type unitType) {
        ThemeNames config = themeConfigs.get(theme);
        if (config == null) {
            return null;
        }

        String prefix;
        int number;

        switch (unitType) {
            case INFANTRY:
                prefix = config.infantryPrefix;
                number = config.infantryCounter++;
                break;
            case ARMOR:
                prefix = config.armorPrefix;
                number = config.armorCounter++;
                break;
            case FIGHTER:
                prefix = config.fighterPrefix;
                number = config.fighterCounter++;
                break;
            case BOMBER:
                prefix = config.bomberPrefix;
                number = config.bomberCounter++;
                break;
            case CARGO:
                prefix = config.cargoPrefix;
                number = config.cargoCounter++;
                break;
            default:
                return null;
        }

        return prefix + " " + number;
    }

    /**
     * Assigns a theme to a player.
     * @param playerId The player's ID
     * @param theme The naming theme for this player
     */
    public static void setPlayerTheme(int playerId, Theme theme) {
        instance.playerThemes.put(playerId, theme);
    }

    /**
     * Gets a player's assigned theme.
     * @param playerId The player's ID
     * @return The theme, or MODERN if not set
     */
    public static Theme getPlayerTheme(int playerId) {
        return instance.playerThemes.getOrDefault(playerId, Theme.MODERN);
    }

    /**
     * Gets a name for a unit based on player's theme.
     * @param player The owner of the unit
     * @param unitType The type of unit
     * @return A themed unit name
     */
    public static String getName(Player player, Type unitType) {
        if (player == null || unitType == null) {
            return null;
        }

        Theme theme = getPlayerTheme(player.id);
        return instance.allocateName(theme, unitType);
    }

    /**
     * Checks if a unit type should be named (land/air units).
     * @param type The unit type
     * @return true if this type should be named
     */
    public static boolean shouldNameUnit(Type type) {
        return (
            type == Type.INFANTRY ||
            type == Type.ARMOR ||
            type == Type.FIGHTER ||
            type == Type.BOMBER ||
            type == Type.CARGO
        );
    }

    /**
     * Resets all counters.
     */
    public static void reset() {
        for (ThemeNames config : instance.themeConfigs.values()) {
            config.infantryCounter = 1;
            config.armorCounter = 1;
            config.fighterCounter = 1;
            config.bomberCounter = 1;
            config.cargoCounter = 1;
        }
    }

    /**
     * Automatically assigns themes to players based on their IDs.
     * Player 0 = CLASSICAL, Player 1 = MODERN, Player 2 = MEDIEVAL, Player 3 = EASTERN
     * @param playerCount Number of players
     */
    public static void autoAssignThemes(int playerCount) {
        Theme[] themes = {
            Theme.CLASSICAL,
            Theme.MODERN,
            Theme.MEDIEVAL,
            Theme.EASTERN,
        };
        for (int i = 0; i < playerCount; i++) {
            setPlayerTheme(i, themes[i % themes.length]);
        }
    }
}
