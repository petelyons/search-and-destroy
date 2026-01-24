package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.games.sad.util.json.JsonObj;
import java.util.List;

public class City {

    private volatile String name;
    private volatile Player owner;
    private volatile Type produces;
    private volatile Location location;
    private volatile long round;
    private volatile long productionStart;
    private volatile Board board;
    private volatile Game game;
    private volatile boolean productionCompletedThisTurn;
    private EdictGovernor edicts;
    private transient JsonObj savedEdictsJson; // Temporary storage for edict restoration

    public City(Location loc, Game game) {
        name = GameNames.getName();
        owner = null;
        produces = null;
        location = loc;
        round = 0;
        productionStart = -1;
        this.game = game;
        board = this.game.getBoard();
        productionCompletedThisTurn = false;
    }

    @SuppressWarnings("boxing")
    public City(Game g, JsonObj json) {
        game = g;
        board = this.game.getBoard();
        name = json.getString("name");
        // For backward compatibility with old saves that didn't store city names
        if (name == null) {
            name = GameNames.getName();
        }
        String ownerName = json.getString("owner");
        owner = (ownerName != null) ? this.game.getPlayer(ownerName) : null;
        String productionType = json.getString("produces");
        produces = (productionType != null) ? Type.get(productionType) : null;
        location = Location.get(json.getObj("location"));
        round = json.getLong("round");
        productionStart = json.getLong("ps");
        productionCompletedThisTurn = json.getBoolean("prodcomplete");

        // Initialize edicts - if owner exists, create EdictGovernor
        if (owner != null) {
            edicts = new EdictGovernor(owner, this);

            // Restore edict settings if they were saved
            JsonObj edictsJson = json.getObj("edicts");
            if (edictsJson != null) {
                restoreEdicts(edictsJson);
            }
        }
    }

    /**
     * Restores edict settings from saved JSON data.
     * This is called after the game is fully loaded so city references can be resolved.
     */
    private void restoreEdicts(JsonObj edictsJson) {
        // Note: City references need to be resolved after all cities are loaded
        // Store the JSON for later restoration in a second pass
        this.savedEdictsJson = edictsJson;
    }

    /**
     * Second pass to restore edicts after all cities are loaded.
     * Called from GameStateSerializer after all cities exist.
     */
    public void restoreEdictsSecondPass() {
        if (this.savedEdictsJson == null || this.edicts == null) {
            return;
        }

        // Restore air path destination
        String airPathDestName = this.savedEdictsJson.getString("airPathDest");
        if (airPathDestName != null) {
            City dest = this.game.getBoard().getCityByName(airPathDestName);
            if (dest != null) {
                this.edicts.setAirPathDest(dest);
            }
        }

        // Restore land path destination
        String landPathDestName = this.savedEdictsJson.getString(
            "landPathDest"
        );
        if (landPathDestName != null) {
            City dest = this.game.getBoard().getCityByName(landPathDestName);
            if (dest != null) {
                this.edicts.setLandPathDest(dest);
            }
        }

        // Restore sea path destination
        String seaPathDestName = this.savedEdictsJson.getString("seaPathDest");
        if (seaPathDestName != null) {
            City dest = this.game.getBoard().getCityByName(seaPathDestName);
            if (dest != null) {
                this.edicts.setSeaPathDest(dest);
            }
        }

        // Restore air patrol
        Boolean hasAirPatrol = this.savedEdictsJson.getBoolean("hasAirPatrol");
        if (hasAirPatrol != null && hasAirPatrol) {
            this.edicts.setAirPatrol();
        }

        // Restore auto sentry
        Boolean hasAutoSentry = this.savedEdictsJson.getBoolean(
            "hasAutoSentry"
        );
        if (hasAutoSentry != null && hasAutoSentry) {
            this.edicts.setAutoSentry();
        }

        // Restore city air patrol
        JsonObj cityAirPatrolJson = this.savedEdictsJson.getObj(
            "cityAirPatrol"
        );
        if (cityAirPatrolJson != null) {
            com.developingstorm.games.sad.edicts.CityAirPatrol cityAirPatrol =
                com.developingstorm.games.sad.edicts.CityAirPatrol.fromJson(
                    this,
                    cityAirPatrolJson
                );
            this.edicts.setCityAirPatrol(cityAirPatrol);
        }

        this.savedEdictsJson = null; // Clear after restoration
    }

    @SuppressWarnings("boxing")
    public JsonObj toJson() {
        JsonObj json = new JsonObj();
        json.put("name", this.name);
        if (owner == null) {
            json.put("owner", null);
        } else {
            json.put("owner", this.owner.toJsonLink());
        }
        if (produces == null) {
            json.put("produces", null);
        } else {
            json.put("produces", this.produces.toJsonLink());
        }
        json.put("location", this.location.toJson());
        json.put("round", this.round);
        json.put("ps", this.productionStart);
        json.put("prodcomplete", this.productionCompletedThisTurn);

        if (this.edicts != null) {
            json.put("edicts", this.edicts.toJson());
        } else {
            json.put("edicts", null);
        }
        return json;
    }

    public Object toJsonLink() {
        return name;
    }

    public String toString() {
        if (owner == null) {
            return this.name + " at " + this.location + " unowned";
        }
        return this.name + " at " + this.location + " owned by:" + owner;
    }

    public Location getLocation() {
        return location;
    }

    public void setOwner(Player p) {
        if (this.owner != null) {
            this.owner.loseCity(this);
        }
        owner = p;
        produces = null;
        productionStart = -1;
        edicts = new EdictGovernor(this.owner, this);
        this.owner.captureCity(this);
    }

    public String getName() {
        return name;
    }

    public EdictGovernor getGovernor() {
        return edicts;
    }

    public Player getOwner() {
        return owner;
    }

    public void produce(Type t) {
        produces = t;
        productionStart = round;
    }

    public Type getProduction() {
        return produces;
    }

    public boolean productionCompleted() {
        return productionCompletedThisTurn;
    }

    public boolean isCoastal() {
        return this.board.isCoast(this.location);
    }

    public Continent getContinent() {
        return this.board.getContinent(this.location);
    }

    public List<Unit> getUnits() {
        return this.game.unitsAtLocation(this.location);
    }

    public Game getGame() {
        return game;
    }

    public void bombCity() {
        long d = this.round - productionStart;
        if (d >= 0) {
            d = d / 4;
        }
        this.productionStart += d;
    }

    public void startNewTurn() {
        int cost = this.produces.getCost();
        this.round++;
        productionCompletedThisTurn = false;
        if (
            this.productionStart != -1 &&
            this.productionStart + cost == this.round
        ) {
            Log.info(this, "Creating unit:" + this.produces);
            Unit u = this.game.createUnit(
                this.produces,
                this.owner,
                this.location
            );
            Log.debug(u, "Created at " + this);
            this.owner.addUnit(u);
            productionStart = round;
            productionCompletedThisTurn = true;
        }

        this.edicts.execute();
    }

    public boolean isNear(City target, int dist) {
        return (target.getLocation().isNear(this.location, dist));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result =
            prime * result +
            ((location == null) ? 0 : this.location.hashCode());
        result = prime * result + ((name == null) ? 0 : this.name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        City other = (City) obj;
        if (location == null) {
            if (other.location != null) return false;
        } else if (!this.location.equals(other.location)) return false;
        if (name == null) {
            if (other.name != null) return false;
        } else if (!this.name.equals(other.name)) return false;
        return true;
    }

    public boolean shareContinent(City selectedCity) {
        return true;
    }

    public UnitStats getContinentStats() {
        Continent cont = getContinent();
        UnitStats stats = this.owner.getContinentStats(cont);

        return stats;
    }
}

/**
 * STATIC void citini() { int loc,i,j,k;
 *
 * for (i = CITMAX; i--;) { memset(&city[i],0,sizeof(City)); city[i].loc =
 * city[i].own = 0; city[i].phs = -1; // no phase } for (i = 0, loc = MAPSIZE;
 * loc--;) if (typ[map[loc]] == X) city[i++].loc = loc; printf("%d cities\n",i);
 * assert(i <= CITMAX);
 *
 * // shuffle cities around
 *
 * for (i = CITMAX / 2; i--;) { j = random(CITMAX); k = random(CITMAX); loc =
 * city[j].loc; city[j].loc = city[k].loc; city[k].loc = loc; // swap city locs
 * } }
 */
