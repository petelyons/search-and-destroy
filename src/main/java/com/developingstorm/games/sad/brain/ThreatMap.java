package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.Vision;
import com.developingstorm.games.sad.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks enemy unit positions and threat levels across the map.
 * Provides strategic intelligence about where enemies are concentrated
 * and which areas need defensive attention.
 */
public class ThreatMap {

    private Game game;
    private Player player;
    private Map<Location, ThreatInfo> threatsByLocation;
    private Map<Continent, Double> threatsByContinent;
    private Set<Location> threatenedCities;
    private List<Unit> visibleEnemies;

    public static class ThreatInfo {

        public Location location;
        public List<Unit> enemies;
        public double threatLevel;

        public ThreatInfo(Location loc) {
            this.location = loc;
            this.enemies = new ArrayList<>();
            this.threatLevel = 0.0;
        }

        public void addEnemy(Unit enemy) {
            this.enemies.add(enemy);
            // Threat level based on unit attack power and health
            double unitThreat =
                enemy.getAttack() *
                (enemy.life().hits / (double) enemy.getType().getHits());
            this.threatLevel += unitThreat;
        }
    }

    public ThreatMap(Game game, Player player) {
        this.game = game;
        this.player = player;
        this.threatsByLocation = new HashMap<>();
        this.threatsByContinent = new HashMap<>();
        this.threatenedCities = new HashSet<>();
        this.visibleEnemies = new ArrayList<>();

        analyze();
    }

    /**
     * Analyzes all visible enemy units and calculates threat levels
     */
    private void analyze() {
        // Find all visible enemy units
        for (Player enemy : this.game.getPlayers()) {
            if (enemy.equals(this.player)) {
                continue;
            }

            for (Unit enemyUnit : enemy.getUnits()) {
                if (enemyUnit.isDead()) {
                    continue;
                }

                Location loc = enemyUnit.getLocation();
                // Check if location is visible (not NONE)
                Vision visibility = this.player.getVisibility(loc);
                if (visibility != Vision.NONE) {
                    this.visibleEnemies.add(enemyUnit);

                    // Record threat at this location
                    ThreatInfo threat = this.threatsByLocation.get(loc);
                    if (threat == null) {
                        threat = new ThreatInfo(loc);
                        this.threatsByLocation.put(loc, threat);
                    }
                    threat.addEnemy(enemyUnit);

                    // Add to continent threat
                    Continent continent = this.game.getBoard().getContinent(
                        loc
                    );
                    if (continent != null) {
                        double continentThreat =
                            this.threatsByContinent.getOrDefault(
                                continent,
                                0.0
                            );
                        continentThreat += threat.threatLevel;
                        this.threatsByContinent.put(continent, continentThreat);
                    }

                    // Check if enemy threatens our cities
                    checkCityThreats(enemyUnit);
                }
            }
        }

        Log.info(
            "ThreatMap: Found " +
                this.visibleEnemies.size() +
                " visible enemies, " +
                this.threatenedCities.size() +
                " threatened cities"
        );
    }

    /**
     * Check if enemy unit threatens any of our cities
     */
    private void checkCityThreats(Unit enemy) {
        int threatRange = enemy.getType().getDist() + 2; // Can reach in 1-2 turns

        List<City> cities = this.player.getCities();
        for (City city : cities) {
            Location cityLoc = city.getLocation();
            if (enemy.getLocation().distance(cityLoc) <= threatRange) {
                this.threatenedCities.add(cityLoc);
            }
        }
    }

    /**
     * Get threat level at a specific location
     */
    public double getThreatLevel(Location loc) {
        ThreatInfo threat = this.threatsByLocation.get(loc);
        return threat != null ? threat.threatLevel : 0.0;
    }

    /**
     * Get threat level for an entire continent
     */
    public double getContinentThreatLevel(Continent continent) {
        return this.threatsByContinent.getOrDefault(continent, 0.0);
    }

    /**
     * Check if a location is in a high-threat area
     */
    public boolean isHighThreat(Location loc) {
        return getThreatLevel(loc) > 3.0; // 3+ attack power
    }

    /**
     * Check if a continent is under significant threat
     */
    public boolean isContinentThreatened(Continent continent) {
        return getContinentThreatLevel(continent) > 5.0;
    }

    /**
     * Get all locations with enemy presence
     */
    public Set<Location> getThreatenedLocations() {
        return this.threatsByLocation.keySet();
    }

    /**
     * Get cities that are under threat
     */
    public Set<Location> getThreatenedCities() {
        return this.threatenedCities;
    }

    /**
     * Check if a specific city location is threatened
     */
    public boolean isThreatenedCity(Location loc) {
        return this.threatenedCities.contains(loc);
    }

    /**
     * Get enemies within a certain distance of a location
     */
    public List<Unit> getNearbyEnemies(Location loc, int maxDistance) {
        List<Unit> nearby = new ArrayList<>();
        for (Unit enemy : this.visibleEnemies) {
            if (loc.distance(enemy.getLocation()) <= maxDistance) {
                nearby.add(enemy);
            }
        }
        return nearby;
    }

    /**
     * Get all visible enemy units
     */
    public List<Unit> getVisibleEnemies() {
        return this.visibleEnemies;
    }

    /**
     * Find enemy units of specific types
     */
    public List<Unit> getEnemiesOfType(Type... types) {
        List<Unit> result = new ArrayList<>();
        Set<Type> typeSet = new HashSet<>();
        for (Type t : types) {
            typeSet.add(t);
        }

        for (Unit enemy : this.visibleEnemies) {
            if (typeSet.contains(enemy.getType())) {
                result.add(enemy);
            }
        }
        return result;
    }

    /**
     * Find the closest enemy unit to a given location
     */
    public Unit getClosestEnemy(Location loc) {
        Unit closest = null;
        int minDist = Integer.MAX_VALUE;

        for (Unit enemy : this.visibleEnemies) {
            int dist = loc.distance(enemy.getLocation());
            if (dist < minDist) {
                minDist = dist;
                closest = enemy;
            }
        }

        return closest;
    }

    /**
     * Find high-value enemy targets (carriers, battleships, transports with cargo)
     */
    public List<Unit> getHighValueTargets() {
        List<Unit> targets = new ArrayList<>();

        for (Unit enemy : this.visibleEnemies) {
            Type type = enemy.getType();
            if (
                type == Type.CARRIER ||
                type == Type.BATTLESHIP ||
                type == Type.CRUISER ||
                (type == Type.TRANSPORT && enemy.hasCargo())
            ) {
                targets.add(enemy);
            }
        }

        return targets;
    }

    /**
     * Get continents ordered by threat level (most threatened first)
     */
    public List<Continent> getContinentsByThreat() {
        List<Continent> continents = new ArrayList<>(
            this.threatsByContinent.keySet()
        );
        continents.sort((c1, c2) ->
            Double.compare(
                getContinentThreatLevel(c2),
                getContinentThreatLevel(c1)
            )
        );
        return continents;
    }
}
