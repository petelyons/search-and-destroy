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
import com.developingstorm.games.sad.util.Log;
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

    private final AIConfiguration config;

    private final Set<Location> loadingPoints;

    private final Set<Continent> battlezoneContinents;

    private final Set<Continent> defenseContinents;

    private Set<Location> defenseUnloadingPoints;

    private UnitStats us;

    private Set<Location> expandUnloadingPoints;

    // Strategic intelligence systems
    private ThreatMap threatMap;

    private StrategyMemory strategyMemory;

    private TargetPrioritizer targetPrioritizer;

    // Continent-specific strategies
    private com.developingstorm.games.sad.brain.strategy.ContinentClassifier continentClassifier;

    private java.util.Map<
        Continent,
        com.developingstorm.games.sad.brain.strategy.ContinentStrategy
    > continentStrategies;

    // Production pairs for amphibious operations
    private java.util.List<ProductionPair> productionPairs;

    public Battleplan(
        final Game game,
        final Robot p,
        final AIConfiguration config
    ) {
        this.game = game;
        this.board = game.getBoard();
        this.player = p;
        this.config = config;

        us = this.player.getStats();

        Set<Continent> discovered = this.player.getDiscoveredContinents();
        HashSet<Continent> colonized = (HashSet<
            Continent
        >) this.player.getColonizedContinents();
        Set<Unit> enemies = this.player.getKnownEnemies();

        battlezoneContinents = calcBattlezones(enemies);
        secureContinents = calcSecureContinents(colonized);
        // Target continents = continents with enemy or unoccupied cities
        targetContinents = calcTargetContinents();
        defenseContinents = CollectionUtil.intersect(
            colonized,
            this.battlezoneContinents
        );

        loadingPoints = calcLoadingLocations();
        defenseUnloadingPoints = calcDefenseUnloadingLocations();
        expandUnloadingPoints = calcExpandUnloadingLocations();

        // Initialize strategic intelligence systems
        this.threatMap = new ThreatMap(game, player);
        this.strategyMemory = new StrategyMemory();
        this.targetPrioritizer = new TargetPrioritizer(game, player, threatMap);

        // Initialize continent-based strategies
        this.continentClassifier =
            new com.developingstorm.games.sad.brain.strategy.ContinentClassifier(
                this,
                player
            );
        this.continentStrategies = new java.util.HashMap<>();
        classifyContinents();

        // Initialize production pairs for amphibious operations
        this.productionPairs = new java.util.ArrayList<>();
        createProductionPairs();
    }

    public AIConfiguration getConfig() {
        return config;
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

    /**
     * Calculate target continents - continents with enemy or unoccupied cities.
     * This includes continents where we already have a presence but haven't captured all cities.
     */
    private Set<Continent> calcTargetContinents() {
        Set<Continent> targets = new HashSet<>();

        // Check all cities on the board
        for (City city : board.getCities()) {
            // Skip cities we already own
            if (city.getOwner() == this.player) {
                continue;
            }

            // Check if we've explored this city's location
            Location cityLoc = city.getLocation();
            if (player.isExplored(cityLoc)) {
                Continent cont = board.getContinent(cityLoc);
                if (cont != null) {
                    targets.add(cont);
                }
            }
        }

        return targets;
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

    public Type supplyBasedProductionChoice(City c) {
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

        // PRIORITY 1: Production pairs - dedicated amphibious factories
        // These pairs ignore most other concerns and focus on invasion
        ProductionPair pair = getProductionPair(city);
        if (pair != null) {
            if (city.equals(pair.getCoastalCity())) {
                return Type.TRANSPORT;
            } else {
                return Type.INFANTRY;
            }
        }

        // PRIORITY 2: Support active amphibious operations
        // Check if we need units for planned invasions
        Type operationNeed = getOperationProductionNeed(city);
        if (operationNeed != null) {
            return operationNeed;
        }

        // PRIORITY 3: Emergency defense under threat
        // Check if this city is under immediate threat
        double threatLevel = threatMap.getThreatLevel(city.getLocation());
        boolean underThreat =
            threatLevel > 3.0 || threatMap.isThreatenedCity(city.getLocation());

        // Emergency production under threat: prioritize defensive units
        if (underThreat) {
            // Check what kind of threat we're facing
            List<Unit> nearbyEnemies = threatMap.getNearbyEnemies(
                city.getLocation(),
                3
            );
            boolean airThreat = false;
            boolean navalThreat = false;

            for (Unit enemy : nearbyEnemies) {
                if (enemy.getTravel() == Travel.AIR) {
                    airThreat = true;
                } else if (enemy.getTravel() == Travel.SEA) {
                    navalThreat = true;
                }
            }

            // Respond to specific threats
            if (airThreat && city.isCoastal()) {
                return Type.DESTROYER; // AA defense
            } else if (airThreat) {
                return Type.FIGHTER; // Air defense
            } else if (navalThreat && city.isCoastal()) {
                return Type.DESTROYER; // Naval defense
            } else {
                return Type.INFANTRY; // Ground defense
            }
        }

        // Query continent strategy for production decision
        com.developingstorm.games.sad.brain.strategy.ContinentStrategy strategy =
            continentStrategies.get(cont);
        if (strategy != null) {
            return strategy.getProductionPriority(city);
        }

        // Fallback to legacy logic if no strategy found
        if (this.secureContinents.contains(cont)) {
            Type t = supplyBasedProductionChoice(city);
            return t;
        }

        // Default to infantry for contested areas
        return Type.INFANTRY;
    }

    public Game getGame() {
        return game;
    }

    public Board getBoard() {
        return board;
    }

    public Robot getPlayer() {
        return player;
    }

    public Set<Continent> getTargetContinents() {
        return targetContinents;
    }

    public Set<Continent> getSecureContinents() {
        return secureContinents;
    }

    public java.util.List<ProductionPair> getProductionPairs() {
        return productionPairs;
    }

    // Strategic intelligence accessors
    public ThreatMap getThreatMap() {
        return threatMap;
    }

    public StrategyMemory getStrategyMemory() {
        return strategyMemory;
    }

    public TargetPrioritizer getTargetPrioritizer() {
        return targetPrioritizer;
    }

    /**
     * Get the best city target for a given unit based on strategic priorities
     */
    public City getBestCityTarget(Unit unit) {
        return targetPrioritizer.getBestCityTarget(unit);
    }

    /**
     * Get the best enemy unit target for a given unit
     */
    public Unit getBestEnemyTarget(Unit unit) {
        return targetPrioritizer.getBestUnitTarget(unit);
    }

    /**
     * Get all prioritized city targets in order of importance
     */
    public List<TargetPrioritizer.CityTarget> getPrioritizedCities() {
        return targetPrioritizer.prioritizeCities();
    }

    /**
     * Get all prioritized enemy unit targets in order of importance
     */
    public List<TargetPrioritizer.UnitTarget> getPrioritizedEnemyUnits() {
        return targetPrioritizer.prioritizeEnemyUnits();
    }

    /**
     * Classify all continents and assign strategies
     */
    private void classifyContinents() {
        continentStrategies.clear();

        Set<Continent> allContinents = player.getDiscoveredContinents();
        for (Continent cont : allContinents) {
            com.developingstorm.games.sad.brain.strategy.ContinentStrategy strategy =
                continentClassifier.classifyContinent(cont);
            continentStrategies.put(cont, strategy);
        }
    }

    /**
     * Get the strategy for a specific continent
     */
    public com.developingstorm.games.sad.brain.strategy.ContinentStrategy getContinentStrategy(
        Continent continent
    ) {
        return continentStrategies.get(continent);
    }

    /**
     * Create production pairs: coastal cities paired with nearby inland cities
     * for dedicated amphibious assault production.
     */
    private void createProductionPairs() {
        // Get all our cities
        java.util.List<City> coastalCities = player
            .getCities()
            .stream()
            .filter(City::isCoastal)
            .collect(java.util.stream.Collectors.toList());

        java.util.List<City> inlandCities = player
            .getCities()
            .stream()
            .filter(c -> !c.isCoastal())
            .collect(java.util.stream.Collectors.toList());

        // For each coastal city, find the nearest inland city
        for (City coastal : coastalCities) {
            City nearestInland = null;
            int minDistance = Integer.MAX_VALUE;

            for (City inland : inlandCities) {
                // Skip if already paired
                boolean alreadyPaired = productionPairs
                    .stream()
                    .anyMatch(pair -> pair.getInlandCity().equals(inland));
                if (alreadyPaired) continue;

                int distance = coastal
                    .getLocation()
                    .distance(inland.getLocation());
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestInland = inland;
                }
            }

            // Create pair if we found a suitable inland city (within 10 hexes)
            if (nearestInland != null && minDistance <= 10) {
                ProductionPair pair = new ProductionPair(
                    coastal,
                    nearestInland,
                    minDistance
                );
                productionPairs.add(pair);
                Log.info("Created production pair: " + pair);
            }
        }

        Log.info(
            "Created " +
                productionPairs.size() +
                " production pairs for amphibious operations"
        );
    }

    /**
     * Get the production pair that includes this city, if any.
     */
    private ProductionPair getProductionPair(City city) {
        return productionPairs
            .stream()
            .filter(pair -> pair.contains(city))
            .findFirst()
            .orElse(null);
    }

    /**
     * Calculate what unit type should be produced to support active operations.
     * Returns null if no specific operational need.
     */
    private Type getOperationProductionNeed(City city) {
        // Get active invasion plans from strategy memory
        List<StrategyMemory.InvasionPlan> activeOps =
            strategyMemory.getActiveInvasions();

        if (activeOps.isEmpty()) {
            return null; // No operations, no specific needs
        }

        // Count what we need across all active operations
        int neededTransports = 0;
        int neededCargo = 0;

        for (StrategyMemory.InvasionPlan op : activeOps) {
            // Skip completed/aborted operations
            if (
                op.phase == StrategyMemory.OperationPhase.COMPLETED ||
                op.phase == StrategyMemory.OperationPhase.ABORTED
            ) {
                continue;
            }

            // Calculate shortfall for this operation
            int transports = op.transports.size();
            int cargo = op.cargo.size();

            if (transports < op.requiredTransports) {
                neededTransports += (op.requiredTransports - transports);
            }
            if (cargo < op.requiredCargo) {
                neededCargo += (op.requiredCargo - cargo);
            }
        }

        // Prioritize transports first (they're the bottleneck)
        if (neededTransports > 0 && city.isCoastal()) {
            return Type.TRANSPORT;
        }

        // Then cargo (infantry for now, could be armor)
        if (neededCargo > 0) {
            return Type.INFANTRY;
        }

        return null; // Operations are fully staffed
    }
}
