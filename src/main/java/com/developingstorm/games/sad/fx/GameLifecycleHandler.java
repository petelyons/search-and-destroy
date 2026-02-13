package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Game;
import java.io.File;

/**
 * Interface for handling game lifecycle operations like loading and saving.
 */
public interface GameLifecycleHandler {
    /**
     * Load a game from a file.
     * @param saveFile The save file to load from
     */
    void loadGame(File saveFile);

    /**
     * Save the current game to a file.
     * @param saveFile The file to save to
     */
    void saveGame(File saveFile);

    /**
     * Get the current game instance.
     * @return The current game, or null if no game is loaded
     */
    Game getCurrentGame();

    /**
     * Get the current save name (null if never saved).
     * @return The current save name, or null
     */
    String getCurrentSaveName();

    /**
     * Quick save with the current save name.
     * @param saveName The name to save with
     */
    void quickSave(String saveName);

    /**
     * Save with a new name (Save As).
     * @param saveName The new name to save with
     */
    void saveAs(String saveName);

    /**
     * Start a new game with the given settings.
     * @param player1Name Name for player 1
     * @param player1Type Type for player 1 (0=Human, 1=Robot)
     * @param player2Name Name for player 2
     * @param player2Type Type for player 2 (0=Human, 1=Robot)
     * @param mapResource Map resource filename (e.g. "MedMap.sdm")
     */
    void newGame(
        String player1Name,
        int player1Type,
        String player2Name,
        int player2Type,
        String mapResource
    );
}
