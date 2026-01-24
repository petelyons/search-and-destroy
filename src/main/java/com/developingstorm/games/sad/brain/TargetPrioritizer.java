package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Prioritizes strategic targets (cities, enemy units) based on
 * various factors like proximity, defensibility, and strategic value.
 */
public class TargetPrioritizer {

    private Game game;
    private Player player;
    private ThreatMap threatMap;

    public static class CityTarget {

        public City city;
        public double score;
        public String reason;

        public CityTarget(City c, double s, String r) {
            this.city = c;
            this.score = s;
            this.reason = r;
        }
    }

    public static class UnitTarget {

        public Unit unit;
        public double score;
        public String reason;

        public UnitTarget(Unit u, double s, String r) {
            this.unit = u;
            this.score = s;
            this.reason = r;
        }
    }

    public TargetPrioritizer(Game game, Player player, ThreatMap threatMap) {
        this.game = game;
        this.player = player;
        this.threatMap = threatMap;
    }

    /**
     * Score and prioritize enemy/unowned cities for conquest
     */
    public List<CityTarget> prioritizeCities() {
        List<CityTarget> targets = new ArrayList<>();

        for (City city : this.game.getBoard().getCities()) {
            // Skip our own cities
            if (city.getOwner() == this.player) {
                continue;
            }

            double score = scoreCityTarget(city);
            String reason = getScoreReason(city, score);

            targets.add(new CityTarget(city, score, reason));
        }

        // Sort by score (highest first)
        targets.sort((a, b) -> Double.compare(b.score, a.score));

        return targets;
    }

    /**
     * Calculate strategic value score for a city
     */
    private double scoreCityTarget(City city) {
        double score = 100.0; // Base score

        Location loc = city.getLocation();

        // Factor 1: Proximity to our forces (closer = better)
        int minDist = Integer.MAX_VALUE;
        for (Unit u : this.player.getUnits()) {
            if (!u.isDead()) {
                int dist = u.getLocation().distance(loc);
                if (dist < minDist) {
                    minDist = dist;
                }
            }
        }
        if (minDist < Integer.MAX_VALUE) {
            score += (20.0 / (1.0 + minDist)); // Closer cities more attractive
        }

        // Factor 2: Defensibility (coastal cities harder to defend)
        if (city.isCoastal()) {
            score += 15.0; // Easier to attack by sea
        }

        // Factor 3: Enemy presence (fewer defenders = better)
        int nearbyEnemies = countNearbyEnemies(loc, 3);
        score -= nearbyEnemies * 5.0;

        // Factor 4: Ownership (unowned cities are easiest)
        if (city.getOwner() == null) {
            score += 30.0; // Unowned cities are prime targets
        } else {
            score -= 10.0; // Enemy cities require more effort
        }

        // Factor 5: Continent strategic value
        Continent continent = this.game.getBoard().getContinent(loc);
        if (continent != null) {
            int citiesOnContinent = getCitiesOnContinent(continent);
            score += citiesOnContinent * 2.0; // More valuable continents
        }

        // Factor 6: Current production (avoid cities producing expensive units)
        if (city.getProduction() != null) {
            int cost = city.getProduction().getCost();
            if (cost > 30) {
                score -= 5.0; // City will complete expensive unit soon
            }
        }

        return Math.max(0, score);
    }

    /**
     * Generate human-readable reason for city score
     */
    private String getScoreReason(City city, double score) {
        if (city.getOwner() == null) {
            return "Unowned, easy capture";
        } else if (city.isCoastal()) {
            return "Coastal, vulnerable";
        } else if (score > 120) {
            return "High priority target";
        } else if (score < 50) {
            return "Well defended";
        } else {
            return "Standard target";
        }
    }

    /**
     * Count enemy units near a location
     */
    private int countNearbyEnemies(Location loc, int range) {
        int count = 0;
        for (Unit enemy : this.threatMap.getVisibleEnemies()) {
            if (loc.distance(enemy.getLocation()) <= range) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count cities on a continent
     */
    private int getCitiesOnContinent(Continent continent) {
        int count = 0;
        for (City city : this.game.getBoard().getCities()) {
            if (
                this.game.getBoard().getContinent(city.getLocation()) ==
                continent
            ) {
                count++;
            }
        }
        return count;
    }

    /**
     * Prioritize enemy units for attack
     */
    public List<UnitTarget> prioritizeEnemyUnits() {
        List<UnitTarget> targets = new ArrayList<>();

        for (Unit enemy : this.threatMap.getVisibleEnemies()) {
            double score = scoreUnitTarget(enemy);
            String reason = getUnitScoreReason(enemy, score);

            targets.add(new UnitTarget(enemy, score, reason));
        }

        // Sort by score (highest first)
        targets.sort((a, b) -> Double.compare(b.score, a.score));

        return targets;
    }

    /**
     * Calculate priority score for attacking an enemy unit
     */
    private double scoreUnitTarget(Unit enemy) {
        double score = 50.0; // Base score

        // Factor 1: Unit value (carriers, battleships, loaded transports)
        if (enemy.isCarrier()) {
            score += 40.0;
        } else if (enemy.isBattleship()) {
            score += 35.0;
        } else if (enemy.isTransport() && enemy.hasCargo()) {
            score += 45.0; // Loaded transports are prime targets
        } else if (enemy.isTransport()) {
            score += 25.0;
        } else if (enemy.isCruiser()) {
            score += 20.0;
        } else if (enemy.isDestroyer()) {
            score += 15.0;
        } else if (enemy.isBomber()) {
            score += 30.0; // Bombers can attack cities
        } else if (enemy.isFighter()) {
            score += 20.0;
        } else if (enemy.isArmour()) {
            score += 15.0;
        } else if (enemy.isInfantry()) {
            score += 10.0;
        }

        // Factor 2: Health (wounded units easier to kill)
        double healthPercent =
            enemy.life().hits / (double) enemy.getType().getHits();
        if (healthPercent < 0.5) {
            score += 20.0; // Wounded = easier kill
        }

        // Factor 3: Proximity to our cities
        int minDistToCity = Integer.MAX_VALUE;
        List<City> cities = this.player.getCities();
        for (City city : cities) {
            Location cityLoc = city.getLocation();
            int dist = enemy.getLocation().distance(cityLoc);
            if (dist < minDistToCity) {
                minDistToCity = dist;
            }
        }
        if (minDistToCity <= 3) {
            score += 30.0; // Threatening our cities
        }

        // Factor 4: Isolation (lone units easier to destroy)
        int nearbyAllies = countNearbyEnemies(enemy.getLocation(), 2);
        if (nearbyAllies == 1) {
            score += 15.0; // Isolated unit
        }

        return score;
    }

    /**
     * Generate human-readable reason for unit score
     */
    private String getUnitScoreReason(Unit enemy, double score) {
        if (enemy.isTransport() && enemy.hasCargo()) {
            return "Loaded transport - high value";
        } else if (enemy.isCarrier() || enemy.isBattleship()) {
            return "Capital ship - high value";
        } else if (score > 100) {
            return "Priority target - threatening";
        } else if (score < 40) {
            return "Low priority";
        } else {
            return "Standard target";
        }
    }

    /**
     * Find best city target for a specific unit
     */
    public City getBestCityTarget(Unit unit) {
        List<CityTarget> cities = prioritizeCities();

        for (CityTarget target : cities) {
            // Check if unit can reach this city
            Location targetLoc = target.city.getLocation();
            if (unit.getPath(targetLoc) != null) {
                return target.city;
            }
        }

        return null;
    }

    /**
     * Find best enemy unit target for a specific unit
     */
    public Unit getBestUnitTarget(Unit unit) {
        List<UnitTarget> enemies = prioritizeEnemyUnits();

        for (UnitTarget target : enemies) {
            // Check if unit can reach this enemy
            if (unit.getPath(target.unit.getLocation()) != null) {
                return target.unit;
            }
        }

        return null;
    }
}
