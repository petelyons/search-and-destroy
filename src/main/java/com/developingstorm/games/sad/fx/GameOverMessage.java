package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Player;

/**
 * Text for the end-of-game dialog, from the point of view of the human player.
 */
record GameOverMessage(String header, String content) {

    static GameOverMessage of(Player winner, Player human) {
        if (winner == null) {
            return new GameOverMessage("Draw", "The game has ended with no winner.");
        }
        if (winner.equals(human)) {
            return new GameOverMessage(
                "Victory",
                winner.getName() + " wins: the enemy has no cities and no land units left to take one."
            );
        }
        return new GameOverMessage(
            "Defeat",
            winner.getName() + " wins: you have no cities and no land units left to take one."
        );
    }
}
