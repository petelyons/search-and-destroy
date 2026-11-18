package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Player;
import javafx.scene.paint.Color;

/** Each player's colour, the one their units and cities wear on the board. */
public final class PlayerColors {

    private static final Color RED = Color.rgb(250, 100, 100);
    private static final Color BLUE = Color.rgb(150, 150, 250);
    private static final Color GREEN = Color.rgb(100, 250, 100);

    private PlayerColors() {}

    public static Color of(int playerId) {
        return playerId == 1 ? RED : playerId == 2 ? BLUE : GREEN;
    }

    public static Color of(Player player) {
        return of(player == null ? 0 : player.getId());
    }

    /** The colour as a CSS value, for styled controls. */
    public static String css(Player player) {
        Color c = of(player);
        return "rgb(" + Math.round(c.getRed() * 255) + "," + Math.round(c.getGreen() * 255) + ","
            + Math.round(c.getBlue() * 255) + ")";
    }
}
