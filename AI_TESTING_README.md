# AI Testing Framework - Search and Destroy

## Overview

This framework enables data-driven AI improvement through automated testing, A/B comparisons, and statistical analysis. You can now pit AI configurations against each other, collect detailed statistics, and make informed decisions about AI improvements.

## Architecture

```
AI Testing Framework
├── AIConfiguration - Tunable parameters and feature flags
├── AIConfigurationLoader - Load/save configurations from JSON
├── HeadlessGameRunner - Run games without UI
├── GameStatistics - Collect detailed metrics during games
├── TournamentManager - Run multiple games in parallel
└── AITestCLI - Command-line interface
```

## Quick Start

### 1. List Available Configurations

```bash
mvn exec:java -Dexec.mainClass="com.developingstorm.games.sad.testing.AITestCLI" -Dexec.args="list"
```

Output:
```
Available AI Configurations:
===========================
  baseline/v1.0.json
  profiles/easy.json
  profiles/medium.json
  profiles/hard.json

Total: 4 configurations
```

### 2. Run a Single Test Game

```bash
mvn exec:java -Dexec.mainClass="com.developingstorm.games.sad.testing.AITestCLI" \
  -Dexec.args="single baseline/v1.0 profiles/easy"
```

This runs one game between the baseline AI and the easy profile.

### 3. Run an A/B Test

```bash
mvn exec:java -Dexec.mainClass="com.developingstorm.games.sad.testing.AITestCLI" \
  -Dexec.args="ab-test baseline/v1.0 profiles/hard 50"
```

This runs 50 games comparing baseline vs hard profile, with statistical analysis.

## Directory Structure

```
ai-configs/
├── baseline/          # Stable baseline versions
│   └── v1.0.json     # Current production AI
├── experiments/       # Experimental configurations being tested
│   └── (your experiments here)
├── profiles/          # Difficulty presets
│   ├── easy.json
│   ├── medium.json
│   └── hard.json
└── archive/
    └── rejected/      # Configurations that tested poorly
```

## Configuration Files

### Example Configuration (JSON)

```json
{
  "name": "aggressive-v1",
  "version": "1.0.0",
  "created": "2025-01-29",
  "description": "High aggression, early attack strategy",
  "code_version": "main",
  "parent_config": "baseline/v1.0",
  
  "parameters": {
    "aggressionLevel": 0.8,
    "defenseBias": 0.2,
    "expansionPriority": 0.9,
    "riskTolerance": 0.7,
    "combatThreshold": 1.2,
    "invasionReadiness": 3,
    "productionStrategy": "AGGRESSIVE",
    
    "weights": {
      "cityValue": 100,
      "unitValue": 45,
      "territoryValue": 40,
      "economicValue": 35
    }
  },
  
  "features": {
    "enableTransportOperations": true,
    "enableAirSupport": true,
    "enableCombinedArms": true,
    "enableAdvancedThreatAssessment": true,
    "enableInvasionCoordination": true
  },
  
  "tactical": {
    "minAttackPowerRatio": 1.2,
    "retreatThreshold": 0.4,
    "defensiveDistance": 2,
    "scoutingRange": 6
  }
}
```

### Configuration Parameters

#### Core Parameters (0.0 - 1.0)
- **aggressionLevel**: How likely to initiate combat
- **defenseBias**: How much to prioritize defense vs offense
- **expansionPriority**: How much to prioritize capturing territory
- **riskTolerance**: Willingness to take risks in combat

#### Combat Parameters
- **combatThreshold**: Minimum power ratio to attack (1.5 = only attack if 1.5x stronger)
- **retreatThreshold**: Power ratio at which to retreat
- **defensiveDistance**: How far from cities to position defensive units

#### Production Strategy
- **BALANCED**: Mix of all unit types
- **AGGRESSIVE**: Focus on offensive units
- **DEFENSIVE**: Focus on defensive units

## Workflow for AI Improvement

### Step 1: Establish Baseline

```bash
# First, establish baseline performance
mvn exec:java -Dexec.mainClass="com.developingstorm.games.sad.testing.AITestCLI" \
  -Dexec.args="ab-test baseline/v1.0 baseline/v1.0 20"
```

Expected result: ~50% win rate (vs itself) - this measures consistency.

### Step 2: Create Experimental Configuration

Create `ai-configs/experiments/my_improvement.json`:
- Copy from baseline
- Modify specific parameters you want to test
- Document your hypothesis in the description

### Step 3: Run A/B Test

```bash
mvn exec:java -Dexec.mainClass="com.developingstorm.games.sad.testing.AITestCLI" \
  -Dexec.args="ab-test baseline/v1.0 experiments/my_improvement 100"
```

### Step 4: Analyze Results

The tool will output:
```
===========================================
          Tournament Results
===========================================

Configuration A: baseline-v1.0
Configuration B: my_improvement

Games Completed: 100
Duration: 847.3s

Results:
  Config A wins: 42 (42.0%)
  Config B wins: 58 (58.0%)
  Draws: 0

Statistics:
  Average game length: 87.3 turns
  P-value: 0.0123 (significant at 95% confidence)

Recommendation: PREFER_CONFIG_B
  → Configuration B is significantly better

===========================================
```

### Step 5: Decision Making

- **P-value < 0.05**: Statistically significant difference
- **P-value >= 0.05**: No significant difference (need more games or different approach)

If improvement is significant:
1. Promote to baseline: `cp experiments/my_improvement.json baseline/v1.1.json`
2. Git tag: `git tag -a baseline-v1.1 -m "With my improvement"`
3. Update baseline version in configs

If no improvement:
1. Archive: `mv experiments/my_improvement.json archive/rejected/`
2. Document why it didn't work
3. Try different approach

## Example Testing Sessions

### Testing Combat Aggressiveness

```bash
# Create variations with different aggression levels
# experiments/aggression_0.3.json - aggressionLevel: 0.3
# experiments/aggression_0.5.json - aggressionLevel: 0.5
# experiments/aggression_0.7.json - aggressionLevel: 0.7

# Test each
mvn exec:java ... -Dexec.args="ab-test baseline/v1.0 experiments/aggression_0.3 50"
mvn exec:java ... -Dexec.args="ab-test baseline/v1.0 experiments/aggression_0.5 50"
mvn exec:java ... -Dexec.args="ab-test baseline/v1.0 experiments/aggression_0.7 50"

# Find optimal value
```

### Testing Feature Flags

```bash
# Test enabling/disabling transport operations
# experiments/no_transport.json - enableTransportOperations: false
# baseline/v1.0.json - enableTransportOperations: true

mvn exec:java ... -Dexec.args="ab-test experiments/no_transport baseline/v1.0 100"
```

## Advanced Usage

### Running Tests Programmatically

```java
import com.developingstorm.games.sad.testing.*;
import com.developingstorm.games.sad.brain.*;

// Load map
HexBoardMap map = HexBoardMap.loadMapAsResource(MyClass.class, "MedMap.sdm");

// Create context
HexBoardContext ctx = ...; // (see AITestCLI for example)

// Load configurations
AIConfiguration configA = AIConfigurationLoader.loadByName("baseline/v1.0");
AIConfiguration configB = AIConfigurationLoader.loadByName("experiments/my_test");

// Run tournament
TournamentManager manager = new TournamentManager(map, ctx, 200, 4);
TournamentManager.TournamentResult result = manager.runABTest(configA, configB, 100);

// Analyze
System.out.println(result.generateReport());
```

### Collecting Detailed Statistics

```java
// Create statistics collector
GameStatistics stats = new GameStatistics(game);

// Game runs...

// Get results
GameStatistics.PlayerStats p1Stats = stats.getPlayerStats(1);
System.out.println("Combat win rate: " + p1Stats.getCombatWinRate());
System.out.println("K/D ratio: " + p1Stats.getKillDeathRatio());
System.out.println("Final units: " + p1Stats.getFinalUnitCount());
```

## Performance Notes

- **Parallel Games**: Default is 4 parallel games. Adjust based on CPU cores.
- **Turn Limit**: Default is 200 turns. Prevents infinite games.
- **Game Duration**: ~5-10 seconds per game (headless mode)
- **A/B Test Duration**: 100 games ≈ 10-15 minutes (with 4 parallel)

## Statistical Interpretation

### P-Value
- **< 0.01**: Very strong evidence of difference
- **< 0.05**: Strong evidence (standard threshold)
- **< 0.10**: Weak evidence
- **>= 0.10**: No significant difference

### Win Rate Confidence
- **20 games**: ±22% margin of error
- **50 games**: ±14% margin of error
- **100 games**: ±10% margin of error
- **200 games**: ±7% margin of error

### Recommendation
- Run at least 50 games for meaningful comparison
- Run 100+ games for high-confidence decisions
- If results are borderline, run more games

## Troubleshooting

### "Configuration not found"
- Check filename (case-sensitive)
- Ensure file is in ai-configs directory structure
- Try full path: `ai-configs/baseline/v1.0.json`

### "Game timed out"
- Increase turn limit in code
- Check for infinite loops in AI logic
- Verify map is valid

### "P-value = 1.0 (not significant)"
- All games were draws - configurations may be too similar
- Try more extreme parameter differences
- Check if games are actually running properly

## Future Enhancements

Potential additions to the framework:
1. **Parameter Sweeps**: Automatically test ranges of parameters
2. **Multi-Configuration Tournaments**: Round-robin between many configs
3. **Elo Ratings**: Track relative strength of configurations over time
4. **Replay Saving**: Save interesting games for manual review
5. **Visual Analysis**: Generate charts of win rates, combat stats, etc.
6. **Regression Testing**: Automatically test new code against baseline AI

## Files Created

### Core Components
- `AIConfiguration.java` - Configuration data structure
- `AIConfigurationLoader.java` - JSON serialization
- `HeadlessGameRunner.java` - Headless game execution
- `GameStatistics.java` - Metrics collection
- `TournamentManager.java` - Parallel testing
- `AITestCLI.java` - Command-line interface

### Configuration Files
- `ai-configs/baseline/v1.0.json` - Production baseline
- `ai-configs/profiles/easy.json` - Easy difficulty
- `ai-configs/profiles/medium.json` - Medium difficulty
- `ai-configs/profiles/hard.json` - Hard difficulty

### Modified Files
- `RobotBrain.java` - Accepts configuration
- `Battleplan.java` - Passes configuration through
- `General.java` - Configuration accessor
- `Robot.java` - Custom brain setter

## Questions?

See the plan document at `~/.claude/plans/typed-booping-gadget.md` for detailed implementation notes and architectural decisions.
