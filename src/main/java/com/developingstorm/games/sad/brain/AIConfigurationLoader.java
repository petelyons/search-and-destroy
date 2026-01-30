package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.util.json.JsonFormatter;
import com.developingstorm.games.sad.util.json.JsonObj;
import com.developingstorm.games.sad.util.json.JsonParser;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads and saves AI configurations from/to JSON files.
 * Supports searching standard directories for configurations.
 */
public class AIConfigurationLoader {

    private static final String CONFIG_BASE_DIR = "ai-configs";
    private static final String[] SEARCH_DIRS = {
        "experiments",
        "baseline",
        "profiles",
    };

    /**
     * Load configuration from JSON file.
     */
    public static AIConfiguration loadFromFile(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new IOException("Configuration file not found: " + path);
        }

        String json = new String(Files.readAllBytes(file.toPath()));
        JsonObj root = (JsonObj) JsonParser.parse(json);

        AIConfiguration config = new AIConfiguration();

        // Metadata
        String name = root.getString("name");
        if (name != null) {
            config.setName(name);
        }
        String version = root.getString("version");
        if (version != null) {
            config.setVersion(version);
        }
        String description = root.getString("description");
        if (description != null) {
            config.setDescription(description);
        }
        String codeVersion = root.getString("code_version");
        if (codeVersion != null) {
            config.setCodeVersion(codeVersion);
        }
        String parentConfig = root.getString("parent_config");
        if (parentConfig != null) {
            config.setParentConfig(parentConfig);
        }

        // Parameters
        JsonObj params = root.getObj("parameters");
        if (params != null) {
            Double aggressionLevel = params.getDouble("aggressionLevel");
            if (aggressionLevel != null) {
                config.setAggressionLevel(aggressionLevel);
            }
            Double defenseBias = params.getDouble("defenseBias");
            if (defenseBias != null) {
                config.setDefenseBias(defenseBias);
            }
            Double expansionPriority = params.getDouble("expansionPriority");
            if (expansionPriority != null) {
                config.setExpansionPriority(expansionPriority);
            }
            Double riskTolerance = params.getDouble("riskTolerance");
            if (riskTolerance != null) {
                config.setRiskTolerance(riskTolerance);
            }
            Double combatThreshold = params.getDouble("combatThreshold");
            if (combatThreshold != null) {
                config.setCombatThreshold(combatThreshold);
            }
            Integer invasionReadiness = params.getInteger("invasionReadiness");
            if (invasionReadiness != null) {
                config.setInvasionReadiness(invasionReadiness);
            }
            String strategy = params.getString("productionStrategy");
            if (strategy != null) {
                config.setProductionStrategy(
                    AIConfiguration.ProductionStrategy.valueOf(strategy)
                );
            }

            // Value weights
            JsonObj weights = params.getObj("weights");
            if (weights != null) {
                Integer cityValue = weights.getInteger("cityValue");
                if (cityValue != null) {
                    config.setCityValue(cityValue);
                }
                Integer unitValue = weights.getInteger("unitValue");
                if (unitValue != null) {
                    config.setUnitValue(unitValue);
                }
                Integer territoryValue = weights.getInteger("territoryValue");
                if (territoryValue != null) {
                    config.setTerritoryValue(territoryValue);
                }
                Integer economicValue = weights.getInteger("economicValue");
                if (economicValue != null) {
                    config.setEconomicValue(economicValue);
                }
            }
        }

        // Features
        JsonObj features = root.getObj("features");
        if (features != null) {
            Boolean enableTransportOperations = features.getBoolean(
                "enableTransportOperations"
            );
            if (enableTransportOperations != null) {
                config.setEnableTransportOperations(enableTransportOperations);
            }
            Boolean enableAirSupport = features.getBoolean("enableAirSupport");
            if (enableAirSupport != null) {
                config.setEnableAirSupport(enableAirSupport);
            }
            Boolean enableCombinedArms = features.getBoolean(
                "enableCombinedArms"
            );
            if (enableCombinedArms != null) {
                config.setEnableCombinedArms(enableCombinedArms);
            }
            Boolean enableAdvancedThreatAssessment = features.getBoolean(
                "enableAdvancedThreatAssessment"
            );
            if (enableAdvancedThreatAssessment != null) {
                config.setEnableAdvancedThreatAssessment(
                    enableAdvancedThreatAssessment
                );
            }
            Boolean enableInvasionCoordination = features.getBoolean(
                "enableInvasionCoordination"
            );
            if (enableInvasionCoordination != null) {
                config.setEnableInvasionCoordination(
                    enableInvasionCoordination
                );
            }
        }

        // Tactical parameters
        JsonObj tactical = root.getObj("tactical");
        if (tactical != null) {
            Double minAttackPowerRatio = tactical.getDouble(
                "minAttackPowerRatio"
            );
            if (minAttackPowerRatio != null) {
                config.setCombatThreshold(minAttackPowerRatio);
            }
            Double retreatThreshold = tactical.getDouble("retreatThreshold");
            if (retreatThreshold != null) {
                config.setRetreatThreshold(retreatThreshold);
            }
            Integer defensiveDistance = tactical.getInteger(
                "defensiveDistance"
            );
            if (defensiveDistance != null) {
                config.setDefensiveDistance(defensiveDistance);
            }
            Integer scoutingRange = tactical.getInteger("scoutingRange");
            if (scoutingRange != null) {
                config.setScoutingRange(scoutingRange);
            }
        }

        return config;
    }

    /**
     * Load configuration by name (searches standard directories).
     */
    public static AIConfiguration loadByName(String name) throws IOException {
        // Try with .json extension if not present
        String fileName = name.endsWith(".json") ? name : name + ".json";

        // Search in standard directories
        for (String dir : SEARCH_DIRS) {
            String path = CONFIG_BASE_DIR + "/" + dir + "/" + fileName;
            File file = new File(path);
            if (file.exists()) {
                return loadFromFile(path);
            }
        }

        // Try direct path
        File file = new File(CONFIG_BASE_DIR + "/" + fileName);
        if (file.exists()) {
            return loadFromFile(CONFIG_BASE_DIR + "/" + fileName);
        }

        throw new IOException("Configuration not found: " + name);
    }

    /**
     * Get all available configurations.
     */
    public static List<String> listConfigurations() {
        List<String> configs = new ArrayList<>();

        for (String dir : SEARCH_DIRS) {
            File directory = new File(CONFIG_BASE_DIR + "/" + dir);
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles((d, name) ->
                    name.endsWith(".json")
                );
                if (files != null) {
                    for (File file : files) {
                        configs.add(dir + "/" + file.getName());
                    }
                }
            }
        }

        return configs;
    }

    /**
     * Save configuration to JSON file.
     */
    public static void saveConfiguration(AIConfiguration config, String path)
        throws IOException {
        JsonObj root = new JsonObj();

        // Metadata
        root.put("name", config.getName());
        root.put("version", config.getVersion());
        root.put("description", config.getDescription());
        root.put("code_version", config.getCodeVersion());
        if (config.getParentConfig() != null) {
            root.put("parent_config", config.getParentConfig());
        }

        // Parameters
        JsonObj params = new JsonObj();
        params.put("aggressionLevel", config.getAggressionLevel());
        params.put("defenseBias", config.getDefenseBias());
        params.put("expansionPriority", config.getExpansionPriority());
        params.put("riskTolerance", config.getRiskTolerance());
        params.put("combatThreshold", config.getCombatThreshold());
        params.put("invasionReadiness", config.getInvasionReadiness());
        params.put("productionStrategy", config.getProductionStrategy().name());

        // Value weights
        JsonObj weights = new JsonObj();
        weights.put("cityValue", config.getCityValue());
        weights.put("unitValue", config.getUnitValue());
        weights.put("territoryValue", config.getTerritoryValue());
        weights.put("economicValue", config.getEconomicValue());
        params.put("weights", weights);

        root.put("parameters", params);

        // Features
        JsonObj features = new JsonObj();
        features.put(
            "enableTransportOperations",
            config.isEnableTransportOperations()
        );
        features.put("enableAirSupport", config.isEnableAirSupport());
        features.put("enableCombinedArms", config.isEnableCombinedArms());
        features.put(
            "enableAdvancedThreatAssessment",
            config.isEnableAdvancedThreatAssessment()
        );
        features.put(
            "enableInvasionCoordination",
            config.isEnableInvasionCoordination()
        );
        root.put("features", features);

        // Tactical
        JsonObj tactical = new JsonObj();
        tactical.put("minAttackPowerRatio", config.getCombatThreshold());
        tactical.put("retreatThreshold", config.getRetreatThreshold());
        tactical.put("defensiveDistance", config.getDefensiveDistance());
        tactical.put("scoutingRange", config.getScoutingRange());
        root.put("tactical", tactical);

        // Format and write
        String json = JsonFormatter.format(root);

        // Ensure parent directory exists
        File file = new File(path);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(json);
        }
    }

    /**
     * Ensure configuration directories exist.
     */
    public static void initializeDirectories() {
        new File(CONFIG_BASE_DIR + "/baseline").mkdirs();
        new File(CONFIG_BASE_DIR + "/experiments").mkdirs();
        new File(CONFIG_BASE_DIR + "/profiles").mkdirs();
        new File(CONFIG_BASE_DIR + "/archive/rejected").mkdirs();
    }
}
