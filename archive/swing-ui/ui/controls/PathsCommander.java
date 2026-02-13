package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.commands.SetPathCommand;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;

/**
 * Commander for path mode - allows setting city path edicts.
 * Uses the new concurrency model with commands and events.
 */
public class PathsCommander extends BaseCommander {

    private volatile City selectedCity;
    private volatile Travel selectedTravel;

    public PathsCommander(SaDFrame frame, Game game) {
        super(frame, game);
    }

    public void setPathOrigin(City c, Travel travel) {
        selectedCity = c;
        selectedTravel = travel;
    }

    public Location getCurrentLocation() {
        if (this.selectedCity != null) {
            return this.selectedCity.getLocation();
        } else {
            return null;
        }
    }

    public void choose(BoardHex hex) {}

    public boolean isDraggable(BoardHex hex) {
        return false;
    }

    public Location autoDraggingLocation() {
        if (selectedCity == null) {
            return null;
        }
        return this.selectedCity.getLocation();
    }

    public boolean isValidDestination(BoardHex hex) {
        City city = this.game.cityAtLocation(hex.getLocation());
        if (city == null) {
            return false;
        }
        if (this.selectedTravel.equals(Travel.SEA)) {
            return city.isCoastal();
        }

        if (this.selectedTravel.equals(Travel.LAND)) {
            return city.shareContinent(this.selectedCity);
        }

        return true;
    }

    public void setDestination(BoardHex hex) {
        City city = this.game.cityAtLocation(hex.getLocation());

        // Submit command directly using new concurrency model
        // Command will be executed on game thread, and MapUpdatedEvent will be fired automatically
        SetPathCommand cmd = new SetPathCommand(
            this.selectedCity,
            city,
            this.selectedTravel
        );
        this.game.submitCommand(cmd);
    }

    public void endPathsMode() {
        this.canvas.clearArrow();
        this.frame.returnGameMode();
    }
}
