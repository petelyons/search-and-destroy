package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.UnitStats;
import com.developingstorm.util.CollectionUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Battleplan {

    // We have explored the entire continent and control all the cities
    private final Set<Continent> secureContinents;

    // Known continents where we don't own a city
    private final Set<Continent> targetContinents;

    private final Board board;

    private final Robot player;

    private final Game game;

    private final Set<Location> loadingPoints;

    private final Set<Continent> battlezoneContinents;

    private final Set<Continent> defenseContinents;

    private Set<Location> defenseUnloadingPoints;

    private UnitStats us;

    private Set<Location> expandUnloadingPoints;

    public Battleplan(final Game game, final Robot p) {
        this.game = game;
        this.board = game.getBoard();
        this.player = p;

        us = this.player.getStats();

        Set<Continent> discovered = this.player.getDiscoveredContinents();
        HashSet<Continent> colonized = (HashSet<
            Continent
        >) this.player.getColonizedContinents();
        Set<Unit> enemies = this.player.getKnownEnemies();

        battlezoneContinents = calcBattlezones(enemies);
        secureContinents = calcSecureContinents(colonized);
        targetContinents = CollectionUtil.subtract(discovered, colonized);
        defenseContinents = CollectionUtil.intersect(
            colonized,
            this.battlezoneContinents
        );

        loadingPoints = calcLoadingLocations();
        defenseUnloadingPoints = calcDefenseUnloadingLocations();
        expandUnloadingPoints = calcExpandUnloadingLocations();
    }

    private static final String CRLF = "\r\n";

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(CRLF);
        sb.append("BATTLEPLAN:");
        sb.append(CRLF);
        sb.append("----------------------------------------");
        sb.append(CRLF);
        sb.append("Secure continents:");
        listContinents(sb, this.secureContinents);
        sb.append("Target continents:");
        listContinents(sb, this.targetContinents);
        sb.append("Defense continents:");
        listContinents(sb, this.defenseContinents);
        sb.append("BZone continents:");
        listContinents(sb, this.battlezoneContinents);
        sb.append("Loading Points");
        listLocations(sb, this.loadingPoints);
        sb.append("Defense Unloading Points");
        listLocations(sb, this.defenseUnloadingPoints);
        sb.append("Expland Unloading Points");
        listLocations(sb, this.expandUnloadingPoints);
        return sb.toString();
    }

    private void listContinents(StringBuilder sb, Set<Continent> continents) {
        sb.append(CRLF);
        int counter = 0;
        if (continents != null) {
            for (Continent cont : continents) {
                if (counter > 0) {
                    sb.append(", ");
                }
                sb.append(cont);
                counter++;
            }
        }
        sb.append(CRLF);
    }

    private void listLocations(StringBuilder sb, Set<Location> locations) {
        sb.append(CRLF);
        int counter = 0;
        if (locations != null) {
            for (Location loc : locations) {
                if (counter > 0) {
                    sb.append(", ");
                }
                sb.append(loc);
                counter++;
            }
        }
        sb.append(CRLF);
    }

    private HashSet<Continent> calcSecureContinents(Set<Continent> colonized) {
        HashSet<Continent> set = new HashSet<Continent>();
        for (Continent cont : colonized) {
            if (cont == null) {
                throw new SaDException(
                    "Null values not allowed in continent sets"
                );
            }
            int totalCities = cont.getCityCount();
            int ownedCities = 0;
            for (City city : this.player.getCities()) {
                if (city.getContinent().equals(cont)) {
                    ownedCities++;
                }
            }
            if (ownedCities == totalCities) {
                set.add(cont);
            }
        }
        return set;
    }

    /**
     * Provide the Set of continents where enemy land units have been spotted
     * @param enemies
     * @return
     */
    private Set<Continent> calcBattlezones(Set<Unit> enemies) {
        Set<Continent> contested = new HashSet<Continent>();
        for (Unit u : enemies) {
            if (u.getTravel().equals(Travel.LAND)) {
                contested.add(this.board.getContinent(u.getLocation()));
            }
        }
        return contested;
    }

    private Set<Location> calcLoadingLocations() {
        Set<Location> loadingPoints = new HashSet<Location>();
        for (City c : this.player.getCities()) {
            if (c.isCoastal() && c.getProduction().equals(Type.TRANSPORT)) {
                Location loc = c.getLocation();
                loadingPoints.add(loc);
            }
        }
        return loadingPoints;
    }

    private static Set<Location> coastline(Set<Continent> continents) {
        Set<Location> points = new HashSet<Location>();
        for (Continent con : continents) {
            points.addAll(con.getCoastalWaters());
        }
        return points;
    }

    private Set<Location> calcDefenseUnloadingLocations() {
        Set<Location> unloadingPoints = new HashSet<Location>();
        if (!this.defenseContinents.isEmpty()) {
            return coastline(this.defenseContinents);
        }
        return unloadingPoints;
    }

    private Set<Location> calcExpandUnloadingLocations() {
        Set<Location> unloadingPoints = new HashSet<Location>();
        if (!this.targetContinents.isEmpty()) {
            return coastline(this.targetContinents);
        }
        return unloadingPoints;
    }

    /**
     * Provide the Set of locations loading cargo and transports
     * @param enemies
     * @return
     */
    public Set<Location> getLoadingPoints() {
        return loadingPoints;
    }

    /**
     * Provide the Set of locations for unloading transports
     * @param enemies
     * @return
     */
    public Set<Location> getDefenseUnloadingPoints() {
        return defenseUnloadingPoints;
    }

    public Set<Location> getExpandUnloadingPoints() {
        return expandUnloadingPoints;
    }

    private Type supplyBasedProductionChoice(City c) {
        this.us.recalc();
        if (c.isCoastal()) {
            return coastalProductionChoice(c);
        } else {
            return inlandProductionChoice(c);
        }
    }

    private Type inlandProductionChoice(City c) {
        Continent cont = c.getContinent();
        List<City> coastal = cont.coastalCities();
        UnitStats stats = c.getContinentStats();

        Type currrentProduction = c.getProduction();
        stats.decrementProduction(currrentProduction);

        int infantry = stats.getProduction(Type.INFANTRY);
        int armor = stats.getProduction(Type.ARMOR);
        int bomber = stats.getProduction(Type.BOMBER);
        int fighter = stats.getProduction(Type.FIGHTER);

        if (infantry + armor > bomber + fighter) {
            if (bomber > fighter) {
                return Type.FIGHTER;
            } else {
                return Type.BOMBER;
            }
        } else {
            if (infantry > armor) {
                return Type.INFANTRY;
            } else {
                return Type.ARMOR;
            }
        }
    }

    private Type coastalProductionChoice(City c) {
        UnitStats stats = c.getContinentStats();
        Type currrentProduction = c.getProduction();
        stats.decrementProduction(currrentProduction);

        int infantry = stats.getProduction(Type.INFANTRY);
        int armor = stats.getProduction(Type.ARMOR);
        int transports = stats.getProduction(Type.TRANSPORT);

        if (this.us.getCount(Type.INFANTRY) < 6) {
            return Type.INFANTRY;
        }

        if (this.us.getCount(Type.TRANSPORT) == 0) {
            return Type.TRANSPORT;
        }

        if (infantry + armor > 0 && transports == 0) {
            return Type.TRANSPORT;
        }

        Type t = percentageCoastalChoice(c);
        this.us.incrementProduction(t);
        return t;
    }

    private Type percentageCoastalChoice(City c) {
        if (
            this.us.getPercentage(Type.DESTROYER) < 0.075
        ) return Type.DESTROYER;
        if (
            this.us.getPercentage(Type.SUBMARINE) < 0.075
        ) return Type.SUBMARINE;
        if (this.us.getPercentage(Type.CRUISER) < 0.05) return Type.CRUISER;
        if (this.us.getPercentage(Type.CARRIER) < 0.05) return Type.CARRIER;
        if (
            this.us.getPercentage(Type.BATTLESHIP) < 0.05
        ) return Type.BATTLESHIP;
        return Type.SUBMARINE;
    }

    public Type productionChoice(City city) {
        Continent cont = city.getContinent();
        if (this.secureContinents.contains(cont)) {
            Type t = supplyBasedProductionChoice(city);
            return t;
        }

        return Type.INFANTRY;
    }

    Game getGame() {
        return game;
    }

    Board getBoard() {
        return board;
    }

    Robot getPlayer() {
        return player;
    }
}
