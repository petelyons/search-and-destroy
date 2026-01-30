package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.EdictGovernor;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;

/**
 * Command to clear a city's path destination for a specific travel type.
 * This ensures path clearing happens on the game thread.
 *
 * UI updates happen automatically after command execution via Game.processCommands().
 */
public class ClearPathCommand implements GameCommand {

    private final City city;
    private final Travel travelType;

    public ClearPathCommand(City city, Travel travelType) {
        this.city = city;
        this.travelType = travelType;
    }

    @Override
    public void execute(Game game) {
        if (city == null || travelType == null) {
            return;
        }

        EdictGovernor governor = city.getGovernor();
        if (governor == null) {
            return;
        }

        // Clear the appropriate path based on travel type
        // UI update will happen automatically after this command completes
        switch (travelType) {
            case AIR:
                governor.clearAirPath();
                break;
            case SEA:
                governor.clearSeaPath();
                break;
            case LAND:
                governor.clearLandPath();
                break;
        }
    }
}
