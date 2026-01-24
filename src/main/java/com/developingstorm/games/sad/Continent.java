package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**

 *
 */
public class Continent {

    private final Set<Location> locations;
    private final int id;
    private final Board board;
    private final Set<Location> coastWater;
    private final List<City> cities;
    private final List<City> coastalCities;
    private final List<City> inlandCities;
    private String name;

    Continent(Board b, int id) {
        this.board = b;
        this.id = id;
        this.locations = new HashSet<Location>();
        this.cities = new ArrayList<City>();
        this.coastWater = new HashSet<Location>();
        this.coastalCities = new ArrayList<City>();
        this.inlandCities = new ArrayList<City>();
        this.name = ContinentNames.getName();
    }

    public String toString() {
        return name != null ? name : "Ct-" + id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void add(Location loc) {
        this.locations.add(loc);
    }

    void init() {
        calcCoastalWaters();
        calcCities();
    }

    public Set<Location> getCoastalWaters(int continent) {
        return coastWater;
    }

    public int getCityCount() {
        return this.cities.size();
    }

    private void calcCities() {
        for (Location loc : this.locations) {
            City c = this.board.getCity(loc);
            if (c != null) {
                this.cities.add(c);
                if (c.isCoastal()) {
                    this.coastalCities.add(c);
                } else {
                    this.inlandCities.add(c);
                }
            }
        }
    }

    private void calcCoastalWaters() {
        for (Location loc : this.locations) {
            List<BoardHex> ring = this.board.getRing(loc, 1);
            for (BoardHex hex : ring) {
                Location loc2 = hex.getLocation();
                if (this.board.isWater(loc2)) {
                    this.coastWater.add(loc2);
                }
            }
        }
    }

    public Set<Location> getLocations() {
        return locations;
    }

    public int getID() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Continent other = (Continent) obj;
        if (this.id != other.id) return false;
        return true;
    }

    public Set<Location> getCoastalWaters() {
        return coastWater;
    }

    public List<City> getCities() {
        return cities;
    }

    public List<City> getOtherCities(City except) {
        List<City> cities = new ArrayList<City>(this.cities);
        cities.remove(except);
        return cities;
    }

    public List<City> coastalCities() {
        return coastalCities;
    }

    public List<City> inlandCities() {
        return inlandCities;
    }

    /**
     * Serializes continent name to JSON for save games.
     * Only the ID and name need to be saved - everything else is recalculated from the map.
     */
    public com.developingstorm.games.sad.util.json.JsonObj toJson() {
        com.developingstorm.games.sad.util.json.JsonObj json =
            new com.developingstorm.games.sad.util.json.JsonObj();
        json.put("id", id);
        json.put("name", name);
        return json;
    }

    /**
     * Restores continent name from JSON.
     */
    public void fromJson(com.developingstorm.games.sad.util.json.JsonObj json) {
        // ID is already set during construction
        String savedName = json.getString("name");
        if (savedName != null) {
            this.name = savedName;
        }
    }
}
