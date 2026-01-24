package com.developingstorm.games.sad;

import com.developingstorm.games.sad.edicts.AirPatrol;
import com.developingstorm.games.sad.edicts.AutoSentry;
import com.developingstorm.games.sad.edicts.CityAirPatrol;
import com.developingstorm.games.sad.edicts.SendAirUnits;
import com.developingstorm.games.sad.edicts.SendLandUnits;
import com.developingstorm.games.sad.edicts.SendSeaUnits;
import java.util.ArrayList;
import java.util.List;

/**
 * Each city has an EdictGovernor that executes its assigned edicts.
 */
public class EdictGovernor {

    private final City c;
    private final Player player;
    private volatile SendAirUnits airPath;
    private volatile SendLandUnits landPath;
    private volatile SendSeaUnits seaPath;
    private volatile AirPatrol airPatrol;
    private volatile AutoSentry autoSentry;
    private volatile CityAirPatrol cityAirPatrol; // New persistent city-level air patrol

    private final Game game;

    EdictGovernor(Player player, City c) {
        this.c = c;
        this.player = player;
        game = this.c.getGame();
    }

    public void setAirPathDest(City c) {
        airPath = this.player.edictFactory().sendAirUnits(this.c, c);
        this.airPath.execute(this.game);
    }

    public void setLandPathDest(City c) {
        landPath = this.player.edictFactory().sendLandUnits(this.c, c);
        this.landPath.execute(this.game);
    }

    public void setSeaPathDest(City c) {
        seaPath = this.player.edictFactory().sendSeaUnits(this.c, c);
        this.seaPath.execute(this.game);
    }

    public void setAirPatrol() {
        airPatrol = this.player.edictFactory().airPatrol(this.c);
        this.airPatrol.execute(this.game);
    }

    public void setAutoSentry() {
        autoSentry = this.player.edictFactory().autoSentry(this.c);
        this.autoSentry.execute(this.game);
    }

    public City getAirPathDest() {
        if (this.airPath != null) {
            return this.airPath.destination();
        }
        return null;
    }

    public City getLandPathDest() {
        if (this.landPath != null) {
            return this.landPath.destination();
        }
        return null;
    }

    public City getSeaPathDest() {
        if (this.seaPath != null) {
            return this.seaPath.destination();
        }
        return null;
    }

    public boolean hastAirPath() {
        return (this.airPath != null);
    }

    public boolean hasLandPath() {
        return (this.landPath != null);
    }

    public boolean hasSeaPath() {
        return (this.seaPath != null);
    }

    public boolean hasAirPatrol() {
        return this.airPatrol != null;
    }

    public boolean hasAutoSentry() {
        return this.autoSentry != null;
    }

    public void setCityAirPatrol(CityAirPatrol patrol) {
        this.cityAirPatrol = patrol;
    }

    public CityAirPatrol getCityAirPatrol() {
        return this.cityAirPatrol;
    }

    public void clearCityAirPatrol() {
        this.cityAirPatrol = null;
    }

    public boolean hasCityAirPatrol() {
        return this.cityAirPatrol != null;
    }

    private List<Edict> getEdicts() {
        List<Edict> edicts = new ArrayList<Edict>();
        if (this.airPatrol != null) {
            edicts.add(this.airPatrol);
        }
        if (this.cityAirPatrol != null) {
            edicts.add(this.cityAirPatrol);
        }
        if (this.autoSentry != null) {
            edicts.add(this.autoSentry);
        }
        if (this.seaPath != null) {
            edicts.add(this.seaPath);
        }
        if (this.landPath != null) {
            edicts.add(this.landPath);
        }
        if (this.airPath != null) {
            edicts.add(this.airPath);
        }
        return edicts;
    }

    public void execute() {
        List<Edict> edicts = getEdicts();
        for (Edict e : edicts) {
            e.execute(this.game);
        }
    }

    public void clearSeaPath() {
        seaPath = null;
    }

    public void clearAirPath() {
        airPath = null;
    }

    public void clearLandPath() {
        landPath = null;
    }

    public void clearAirPatrol() {
        airPatrol = null;
    }

    public void clearAutoSenty() {
        autoSentry = null;
    }

    public Object toJson() {
        com.developingstorm.games.sad.util.json.JsonObj json =
            new com.developingstorm.games.sad.util.json.JsonObj();

        // Serialize air path destination
        if (this.airPath != null && this.airPath.destination() != null) {
            json.put("airPathDest", this.airPath.destination().toJsonLink());
        }

        // Serialize land path destination
        if (this.landPath != null && this.landPath.destination() != null) {
            json.put("landPathDest", this.landPath.destination().toJsonLink());
        }

        // Serialize sea path destination
        if (this.seaPath != null && this.seaPath.destination() != null) {
            json.put("seaPathDest", this.seaPath.destination().toJsonLink());
        }

        // Serialize boolean flags for air patrol and auto sentry
        json.put("hasAirPatrol", this.airPatrol != null);
        json.put("hasAutoSentry", this.autoSentry != null);

        // Serialize city air patrol
        if (this.cityAirPatrol != null) {
            json.put("cityAirPatrol", this.cityAirPatrol.toJson());
        }

        return json;
    }
}
