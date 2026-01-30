package com.developingstorm.games.sad.brain;

/**
 * Configuration for AI behavior and strategy.
 * Allows tuning AI parameters and enabling/disabling features for testing.
 */
public class AIConfiguration {

    // Metadata
    private String name;
    private String version;
    private String description;
    private String codeVersion;
    private String parentConfig;

    // Core parameters (0.0-1.0 scale)
    private double aggressionLevel = 0.5;      // How willing to attack
    private double defenseBias = 0.5;          // Preference for defense vs offense
    private double expansionPriority = 0.5;    // City capture priority
    private double riskTolerance = 0.5;        // Willingness to take risks

    // Combat parameters
    private double combatThreshold = 1.5;      // Min power ratio to attack (our/their)
    private double retreatThreshold = 0.5;     // Max power ratio to retreat
    private int defensiveDistance = 3;         // Distance to respond to threats

    // Strategic parameters
    private ProductionStrategy productionStrategy = ProductionStrategy.BALANCED;
    private int invasionReadiness = 5;         // Min units before launching invasion
    private int scoutingRange = 5;             // How far to explore

    // Value weights
    private int cityValue = 100;
    private int unitValue = 50;
    private int territoryValue = 30;
    private int economicValue = 40;

    // Feature flags
    private boolean enableTransportOperations = true;
    private boolean enableAirSupport = true;
    private boolean enableCombinedArms = false;
    private boolean enableAdvancedThreatAssessment = true;
    private boolean enableInvasionCoordination = false;

    public enum ProductionStrategy {
        AGGRESSIVE,  // Build attack units
        BALANCED,    // Mix of all types
        DEFENSIVE    // Build defensive units
    }

    /**
     * Create default configuration.
     */
    public AIConfiguration() {
        this.name = "default";
        this.version = "1.0.0";
        this.description = "Default AI configuration";
        this.codeVersion = "main";
    }

    /**
     * Create named configuration with defaults.
     */
    public AIConfiguration(String name) {
        this();
        this.name = name;
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCodeVersion() {
        return codeVersion;
    }

    public void setCodeVersion(String codeVersion) {
        this.codeVersion = codeVersion;
    }

    public String getParentConfig() {
        return parentConfig;
    }

    public void setParentConfig(String parentConfig) {
        this.parentConfig = parentConfig;
    }

    public double getAggressionLevel() {
        return aggressionLevel;
    }

    public void setAggressionLevel(double aggressionLevel) {
        this.aggressionLevel = clamp(aggressionLevel, 0.0, 1.0);
    }

    public double getDefenseBias() {
        return defenseBias;
    }

    public void setDefenseBias(double defenseBias) {
        this.defenseBias = clamp(defenseBias, 0.0, 1.0);
    }

    public double getExpansionPriority() {
        return expansionPriority;
    }

    public void setExpansionPriority(double expansionPriority) {
        this.expansionPriority = clamp(expansionPriority, 0.0, 1.0);
    }

    public double getRiskTolerance() {
        return riskTolerance;
    }

    public void setRiskTolerance(double riskTolerance) {
        this.riskTolerance = clamp(riskTolerance, 0.0, 1.0);
    }

    public double getCombatThreshold() {
        return combatThreshold;
    }

    public void setCombatThreshold(double combatThreshold) {
        this.combatThreshold = Math.max(0.1, combatThreshold);
    }

    public double getRetreatThreshold() {
        return retreatThreshold;
    }

    public void setRetreatThreshold(double retreatThreshold) {
        this.retreatThreshold = Math.max(0.0, retreatThreshold);
    }

    public int getDefensiveDistance() {
        return defensiveDistance;
    }

    public void setDefensiveDistance(int defensiveDistance) {
        this.defensiveDistance = Math.max(1, defensiveDistance);
    }

    public ProductionStrategy getProductionStrategy() {
        return productionStrategy;
    }

    public void setProductionStrategy(ProductionStrategy productionStrategy) {
        this.productionStrategy = productionStrategy;
    }

    public int getInvasionReadiness() {
        return invasionReadiness;
    }

    public void setInvasionReadiness(int invasionReadiness) {
        this.invasionReadiness = Math.max(1, invasionReadiness);
    }

    public int getScoutingRange() {
        return scoutingRange;
    }

    public void setScoutingRange(int scoutingRange) {
        this.scoutingRange = Math.max(1, scoutingRange);
    }

    public int getCityValue() {
        return cityValue;
    }

    public void setCityValue(int cityValue) {
        this.cityValue = Math.max(0, cityValue);
    }

    public int getUnitValue() {
        return unitValue;
    }

    public void setUnitValue(int unitValue) {
        this.unitValue = Math.max(0, unitValue);
    }

    public int getTerritoryValue() {
        return territoryValue;
    }

    public void setTerritoryValue(int territoryValue) {
        this.territoryValue = Math.max(0, territoryValue);
    }

    public int getEconomicValue() {
        return economicValue;
    }

    public void setEconomicValue(int economicValue) {
        this.economicValue = Math.max(0, economicValue);
    }

    public boolean isEnableTransportOperations() {
        return enableTransportOperations;
    }

    public void setEnableTransportOperations(boolean enableTransportOperations) {
        this.enableTransportOperations = enableTransportOperations;
    }

    public boolean isEnableAirSupport() {
        return enableAirSupport;
    }

    public void setEnableAirSupport(boolean enableAirSupport) {
        this.enableAirSupport = enableAirSupport;
    }

    public boolean isEnableCombinedArms() {
        return enableCombinedArms;
    }

    public void setEnableCombinedArms(boolean enableCombinedArms) {
        this.enableCombinedArms = enableCombinedArms;
    }

    public boolean isEnableAdvancedThreatAssessment() {
        return enableAdvancedThreatAssessment;
    }

    public void setEnableAdvancedThreatAssessment(boolean enableAdvancedThreatAssessment) {
        this.enableAdvancedThreatAssessment = enableAdvancedThreatAssessment;
    }

    public boolean isEnableInvasionCoordination() {
        return enableInvasionCoordination;
    }

    public void setEnableInvasionCoordination(boolean enableInvasionCoordination) {
        this.enableInvasionCoordination = enableInvasionCoordination;
    }

    /**
     * Helper to clamp values between min and max.
     */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Create an "easy" difficulty configuration.
     */
    public static AIConfiguration createEasy() {
        AIConfiguration config = new AIConfiguration("easy");
        config.setDescription("Beginner AI opponent");
        config.setAggressionLevel(0.3);
        config.setDefenseBias(0.6);
        config.setExpansionPriority(0.4);
        config.setRiskTolerance(0.3);
        config.setCombatThreshold(1.8);
        config.setEnableTransportOperations(false);
        config.setEnableCombinedArms(false);
        config.setEnableAdvancedThreatAssessment(false);
        return config;
    }

    /**
     * Create a "medium" difficulty configuration.
     */
    public static AIConfiguration createMedium() {
        AIConfiguration config = new AIConfiguration("medium");
        config.setDescription("Intermediate AI opponent");
        config.setAggressionLevel(0.5);
        config.setDefenseBias(0.5);
        config.setExpansionPriority(0.6);
        config.setRiskTolerance(0.5);
        config.setCombatThreshold(1.4);
        config.setEnableTransportOperations(true);
        config.setEnableCombinedArms(false);
        config.setEnableAdvancedThreatAssessment(true);
        return config;
    }

    /**
     * Create a "hard" difficulty configuration.
     */
    public static AIConfiguration createHard() {
        AIConfiguration config = new AIConfiguration("hard");
        config.setDescription("Expert AI opponent");
        config.setAggressionLevel(0.7);
        config.setDefenseBias(0.3);
        config.setExpansionPriority(0.8);
        config.setRiskTolerance(0.6);
        config.setCombatThreshold(1.2);
        config.setEnableTransportOperations(true);
        config.setEnableCombinedArms(true);
        config.setEnableAdvancedThreatAssessment(true);
        config.setEnableInvasionCoordination(true);
        return config;
    }

    @Override
    public String toString() {
        return String.format("AIConfiguration[name=%s, version=%s, aggression=%.2f, defense=%.2f]",
                name, version, aggressionLevel, defenseBias);
    }
}
