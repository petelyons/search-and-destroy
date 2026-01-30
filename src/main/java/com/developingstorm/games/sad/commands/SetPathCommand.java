package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.EdictGovernor;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;

/**
 * Command to set a city's path destination for a specific travel type.
 * This ensures path setting happens on the game thread.
 *
 * UI updates happen automatically after command execution via Game.processCommands().
 */
public class SetPathCommand implements GameCommand {

    private final City originCity;
    private final City destinationCity;
    private final Travel travelType;

    public SetPathCommand(
        City originCity,
        City destinationCity,
        Travel travelType
    ) {
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.travelType = travelType;
    }

    @Override
    public void execute(Game game) {
        if (
            originCity == null || destinationCity == null || travelType == null
        ) {
            return;
        }

        EdictGovernor governor = originCity.getGovernor();
        if (governor == null) {
            return;
        }

        // Set the appropriate path based on travel type
        // UI update will happen automatically after this command completes
        switch (travelType) {
            case AIR:
                governor.setAirPathDest(destinationCity);
                break;
            case SEA:
                governor.setSeaPathDest(destinationCity);
                break;
            case LAND:
                governor.setLandPathDest(destinationCity);
                break;
        }
    }
}
