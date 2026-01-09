package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.HexBoard;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Encapsulate SaD board behaviors
 */
public class Board extends HexBoard {

    public HexBoardMap map;
    private int[][] mapData;
    private int[][] contData;
    private HashMap<Location, City> ctoks;
    private Game game;
    private Set<Continent> continents;
    private List<City> cities;
    private boolean isInitialized = false;

    public Board(Game game, HexBoardMap grid, HexBoardContext ctx) {
        super(ctx);
        map = grid;
        mapData = this.map.getData();
        ctoks = new HashMap<Location, City>();
        this.game = game;
        continents = new HashSet<Continent>();
        cities = new ArrayList<City>();

        calcContinents();
    }

    public Location randomLand() {
        int count = 0;
        while (true) {
            Location loc = this.map.random();
            if (isLand(loc)) return loc;
            count++;
            if (count > 1000) throw new Error("No Land on Map");
        }
    }

    public synchronized void init() {
        sprinkleCities((this.map.getHeight() + this.map.getWidth()) / 2);

        // this must follow the sprinkling of the cities
        for (Continent cont : this.continents) {
            cont.init();
        }

        isInitialized = true;
        validateBoard();
    }

    private void validateBoard() {
        for (City c : this.cities) {
            Continent cont = c.getContinent();
            if (cont == null) {
                throw new SaDException(
                    "City not on continent: " +
                        c +
                        " T=" +
                        getTerrain(c.getLocation())
                );
            }
        }
        Continent cont = this.getContinent(Location.get(12, 35));
        if (cont == null) {
            throw new SaDException("Location 12/35 should be a continent");
        }
    }

    private synchronized boolean isInitialized() {
        return isInitialized;
    }

    public List<City> getCities() {
        return cities;
    }

    private City getCity(int x) {
        return (City) this.cities.get(x);
    }

    private void sprinkleCities(int count) {
        if (this.continents.size() == 0) {
            throw new SaDException("Map must have some land");
        }

        boolean[] usedConts = new boolean[this.continents.size() + 1];
        int usedCount = 0;
        for (int x = 0; x < count; x++) {
            boolean again = false;
            int againCount = 0;
            Location loc;
            do {
                loc = randomLand();
                again = false;
                for (int z = 0; z < x; z++) {
                    Location locz = getCity(z).getLocation();
                    if (
                        loc.isNear(locz, 2) &&
                        this.contData[locz.x][locz.y] ==
                        this.contData[loc.x][loc.y]
                    ) {
                        again = true;
                        againCount++;
                        if (againCount == 100) again = false;
                        break;
                    }
                }

                if (again == false) {
                    int continent = this.contData[loc.x][loc.y];
                    if (usedConts[continent] == false) {
                        usedConts[continent] = true;
                        usedCount++;
                    } else {
                        // sprinkle cities to 90% of the continents before
                        // putting cities on
                        // the same continent
                        if (
                            usedCount <
                            this.continents.size() -
                            (this.continents.size() / 10)
                        ) {
                            again = true;
                        }
                    }
                }
            } while (again);

            if (loc == null) {
                throw new SaDException(
                    "A city cannot be placed at a null location!"
                );
            }
            City nc = new City(loc, this.game);
            this.cities.add(nc);
            this.ctoks.put(loc, nc);
        }
    }

    static int wrap(int val, int max) {
        int val2 = val;
        if (val2 < 0) {
            val2 += max;
        }
        if (val2 >= max) {
            val2 -= max;
        }
        return val2;
    }

    public boolean isSame(int x, int y, Board other) {
        return (this.mapData[x][y] == other.mapData[x][y]);
    }

    public boolean isCoast(Location loc) {
        if (!isLand(loc)) {
            return false;
        }

        List<BoardHex> ring = getRing(loc, 1);
        for (BoardHex hex : ring) {
            if (isWater(hex.getLocation())) {
                return true;
            }
        }
        return false;
    }

    public boolean isLand(int x, int y) {
        return isLand(Location.get(x, y));
    }

    public City getCity(Location loc) {
        return (City) this.ctoks.get(loc);
    }

    public boolean isCity(Location loc) {
        return (getCity(loc) != null);
    }

    public boolean isWater(Location loc) {
        return (this.mapData[loc.x][loc.y] == 0);
    }

    public boolean isLand(Location loc) {
        return (this.mapData[loc.x][loc.y] == 1);
    }

    public int getTerrain(Location loc) {
        return (this.mapData[loc.x][loc.y]);
    }

    public boolean isTravelable(Unit u, Location loc) {
        Type t = u.getType();
        Travel trav = t.getTravel();
        return isTravelable(trav, loc);
    }

    public void makeWater(Location loc) {
        this.mapData[loc.x][loc.y] = 0;
    }

    public void makeLand(Location loc) {
        this.mapData[loc.x][loc.y] = 1;
    }

    private void markContinent(Location loc, int c, Continent cont) {
        this.contData[loc.x][loc.y] = c;
        cont.add(loc);
        List<BoardHex> ring = getRing(loc, 1);

        for (BoardHex hex : ring) {
            Location l2 = hex.getLocation();
            if (l2.x < 0 || l2.y < 0) {
                continue;
            }
            if (
                this.contData[l2.x][l2.y] == 0 && this.mapData[l2.x][l2.y] > 0
            ) {
                this.contData[l2.x][l2.y] = c;
                cont.add(Location.get(l2.x, l2.y));
                markContinent(l2, c, cont); // recursive!
            }
        }
    }

    private void calcContinents() {
        int count = 0;
        contData = new int[this.map.getWidth()][this.map.getHeight()];
        for (int x = 0; x < this.map.getWidth(); x++) {
            for (int y = 0; y < this.map.getHeight(); y++) {
                int md = this.mapData[x][y];
                int contd = this.contData[x][y];

                if (x == 12 && y == 35) {
                    Log.info("Running test for 12/35");
                }

                if (md > 0 && contd == 0) {
                    count++;
                    Continent cont = new Continent(this, count);
                    this.continents.add(cont);
                    Location loc = Location.get(x, y);

                    markContinent(loc, count, cont);
                }
            }
        }
    }

    public boolean isWaterPath(Path p) {
        for (Location loc : p) {
            if (isLand(loc) && !isCity(loc)) {
                return false;
            }
        }
        return true;
    }

    public boolean isLandPath(Path p) {
        for (Location loc : p) {
            if (!isLand(loc)) {
                return false;
            }
        }
        return true;
    }

    public Set<Continent> getContinents() {
        return continents;
    }

    public Continent getContinent(Location loc) {
        if (!isInitialized()) {
            throw new SaDException("Board not ready");
        }
        int id = getContinentId(loc);
        if (id == 0) {
            return null;
        }
        return getContinentById(id);
    }

    public Continent getContinentById(int id) {
        for (Continent cont : this.continents) {
            if (cont.getID() == id) {
                return cont;
            }
        }
        throw new SaDException("Invalid continent ID");
    }

    public int getContinentId(Location loc) {
        return this.contData[loc.x][loc.y];
    }

    public int getContinentCount() {
        return this.continents.size();
    }

    public boolean isTravelable(Travel trav, Location loc) {
        if (!onBoard(loc)) {
            return false;
        }
        if (trav == Travel.LAND) {
            return isLand(loc);
        }
        if (trav == Travel.SEA) {
            return (isWater(loc) || isCity(loc));
        }
        return true;
    }
}
