package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.EdictGovernor;
import com.developingstorm.games.sad.Game;

/**
 * Command to toggle a city's air patrol status.
 * This ensures air patrol changes happen on the game thread.
 *
 * UI updates happen automatically after command execution via Game.processCommands().
 */
public class SetAirPatrolCommand implements GameCommand {

    private final City city;
    private final boolean enabled;

    public SetAirPatrolCommand(City city, boolean enabled) {
        this.city = city;
        this.enabled = enabled;
    }

    @Override
    public void execute(Game game) {
        if (city == null) {
            return;
        }

        EdictGovernor governor = city.getGovernor();
        if (governor == null) {
            return;
        }

        // Set or clear air patrol based on enabled flag
        if (enabled) {
            governor.setAirPatrol();
        } else {
            governor.clearAirPatrol();
        }
    }
}
