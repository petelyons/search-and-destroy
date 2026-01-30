package com.developingstorm.games.sad.testing;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.sad.brain.AIConfiguration;
import com.developingstorm.games.sad.brain.AIConfigurationLoader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Manages tournaments and A/B tests between different AI configurations.
 *
 * Features:
 * - Run multiple games in parallel
 * - A/B testing between two configurations
 * - Round-robin tournaments
 * - Statistical analysis of results
 */
public class TournamentManager {

    private final HexBoardMap map;
    private final HexBoardContext ctx;
    private final int turnLimit;
    private final int parallelGames;

    /**
     * Create a tournament manager.
     *
     * @param map The map to use for all games
     * @param ctx The hex board context
     * @param turnLimit Turn limit per game (0 = no limit)
     * @param parallelGames Number of games to run in parallel
     */
    public TournamentManager(
        HexBoardMap map,
        HexBoardContext ctx,
        int turnLimit,
        int parallelGames
    ) {
        this.map = map;
        this.ctx = ctx;
        this.turnLimit = turnLimit;
        this.parallelGames = parallelGames;
    }

    /**
     * Run an A/B test between two configurations.
     *
     * @param configA First configuration
     * @param configB Second configuration
     * @param numGames Number of games to run
     * @return Tournament results
     */
    public TournamentResult runABTest(
        AIConfiguration configA,
        AIConfiguration configB,
        int numGames
    ) throws InterruptedException {
        System.out.println("Starting A/B test:");
        System.out.println("  Config A: " + configA.getName());
        System.out.println("  Config B: " + configB.getName());
        System.out.println("  Games: " + numGames);
        System.out.println("  Parallel: " + parallelGames);

        List<MatchResult> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(parallelGames);
        List<Future<MatchResult>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Submit all games
        for (int i = 0; i < numGames; i++) {
            final int gameNum = i + 1;
            final boolean swap = (i % 2 == 1); // Alternate player positions

            Future<MatchResult> future = executor.submit(() -> {
                try {
                    AIConfiguration p1Config = swap ? configB : configA;
                    AIConfiguration p2Config = swap ? configA : configB;

                    HeadlessGameRunner runner = new HeadlessGameRunner(
                        map,
                        ctx,
                        turnLimit,
                        p1Config,
                        p2Config
                    );

                    System.out.println("  Starting game " + gameNum + "...");
                    HeadlessGameRunner.GameResult result = runner.run(300); // 5 min timeout

                    if (result == null) {
                        System.out.println("  Game " + gameNum + " timed out");
                        return new MatchResult(null, 0, swap, false);
                    }

                    System.out.println("  Game " + gameNum + " completed: " + result);

                    // Adjust winner based on swap
                    Integer adjustedWinner = result.getWinnerPlayerNumber();
                    if (adjustedWinner != null && swap) {
                        adjustedWinner = (adjustedWinner == 1) ? 2 : 1;
                    }

                    return new MatchResult(
                        adjustedWinner,
                        result.getTurns(),
                        swap,
                        true
                    );
                } catch (Exception e) {
                    System.err.println("  Game " + gameNum + " error: " + e.getMessage());
                    return new MatchResult(null, 0, swap, false);
                }
            });

            futures.add(future);
        }

        // Collect results
        for (Future<MatchResult> future : futures) {
            try {
                MatchResult result = future.get();
                results.add(result);
            } catch (Exception e) {
                System.err.println("Error getting result: " + e.getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);

        long endTime = System.currentTimeMillis();
        double durationSeconds = (endTime - startTime) / 1000.0;

        System.out.println("\nA/B test completed in " + String.format("%.1f", durationSeconds) + "s");

        return new TournamentResult(configA, configB, results, durationSeconds);
    }

    /**
     * Run an A/B test by loading configurations from files.
     */
    public TournamentResult runABTest(
        String configAPath,
        String configBPath,
        int numGames
    ) throws IOException, InterruptedException {
        AIConfiguration configA = AIConfigurationLoader.loadFromFile(configAPath);
        AIConfiguration configB = AIConfigurationLoader.loadFromFile(configBPath);
        return runABTest(configA, configB, numGames);
    }

    /**
     * Result of a single match.
     */
    private static class MatchResult {
        final Integer winnerConfig; // 1 = configA, 2 = configB, null = draw
        final int turns;
        final boolean swapped;
        final boolean completed;

        MatchResult(Integer winnerConfig, int turns, boolean swapped, boolean completed) {
            this.winnerConfig = winnerConfig;
            this.turns = turns;
            this.swapped = swapped;
            this.completed = completed;
        }
    }

    /**
     * Results of a tournament.
     */
    public static class TournamentResult {
        private final AIConfiguration configA;
        private final AIConfiguration configB;
        private final List<MatchResult> matches;
        private final double durationSeconds;

        public TournamentResult(
            AIConfiguration configA,
            AIConfiguration configB,
            List<MatchResult> matches,
            double durationSeconds
        ) {
            this.configA = configA;
            this.configB = configB;
            this.matches = matches;
            this.durationSeconds = durationSeconds;
        }

        /**
         * Get number of wins for configuration A.
         */
        public int getConfigAWins() {
            return (int) matches.stream()
                .filter(m -> m.completed && m.winnerConfig != null && m.winnerConfig == 1)
                .count();
        }

        /**
         * Get number of wins for configuration B.
         */
        public int getConfigBWins() {
            return (int) matches.stream()
                .filter(m -> m.completed && m.winnerConfig != null && m.winnerConfig == 2)
                .count();
        }

        /**
         * Get number of draws.
         */
        public int getDraws() {
            return (int) matches.stream()
                .filter(m -> m.completed && m.winnerConfig == null)
                .count();
        }

        /**
         * Get number of incomplete games (timeouts/errors).
         */
        public int getIncomplete() {
            return (int) matches.stream()
                .filter(m -> !m.completed)
                .count();
        }

        /**
         * Get total completed games.
         */
        public int getCompletedGames() {
            return (int) matches.stream()
                .filter(m -> m.completed)
                .count();
        }

        /**
         * Get win rate for configuration A (0.0 to 1.0).
         */
        public double getConfigAWinRate() {
            int completed = getCompletedGames();
            if (completed == 0) {
                return 0.0;
            }
            return (double) getConfigAWins() / completed;
        }

        /**
         * Get average game length in turns.
         */
        public double getAverageGameLength() {
            return matches.stream()
                .filter(m -> m.completed)
                .mapToInt(m -> m.turns)
                .average()
                .orElse(0.0);
        }

        /**
         * Calculate statistical significance (simple z-test).
         * Returns p-value (lower = more significant).
         */
        public double getPValue() {
            int n = getCompletedGames();
            if (n == 0) {
                return 1.0;
            }

            int winsA = getConfigAWins();
            int winsB = getConfigBWins();
            int totalDecisive = winsA + winsB;

            if (totalDecisive == 0) {
                return 1.0; // All draws
            }

            // Proportion test
            double p = (double) winsA / totalDecisive;
            double expectedP = 0.5;

            // Standard error
            double se = Math.sqrt(expectedP * (1 - expectedP) / totalDecisive);

            // Z-score
            double z = Math.abs(p - expectedP) / se;

            // Convert to approximate p-value (two-tailed)
            double pValue = 2 * (1 - approximateStandardNormalCDF(z));

            return pValue;
        }

        /**
         * Approximate standard normal CDF (for p-value calculation).
         */
        private double approximateStandardNormalCDF(double z) {
            // Using approximation formula
            double t = 1.0 / (1.0 + 0.2316419 * z);
            double pdf = Math.exp(-0.5 * z * z) / Math.sqrt(2 * Math.PI);
            double cdf = 1.0 - pdf * t * (
                0.319381530 +
                t * (-0.356563782 +
                t * (1.781477937 +
                t * (-1.821255978 +
                t * 1.330274429)))
            );
            return cdf;
        }

        /**
         * Get recommendation based on results.
         */
        public String getRecommendation() {
            double pValue = getPValue();
            double winRateA = getConfigAWinRate();

            if (pValue > 0.05) {
                return "NO_SIGNIFICANT_DIFFERENCE";
            } else if (winRateA > 0.5) {
                return "PREFER_CONFIG_A";
            } else {
                return "PREFER_CONFIG_B";
            }
        }

        /**
         * Generate a report of the tournament results.
         */
        public String generateReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n===========================================\n");
            sb.append("          Tournament Results\n");
            sb.append("===========================================\n\n");

            sb.append("Configuration A: ").append(configA.getName()).append("\n");
            sb.append("Configuration B: ").append(configB.getName()).append("\n\n");

            sb.append("Games Completed: ").append(getCompletedGames()).append("\n");
            sb.append("Games Incomplete: ").append(getIncomplete()).append("\n");
            sb.append("Duration: ").append(String.format("%.1f", durationSeconds)).append("s\n\n");

            sb.append("Results:\n");
            sb.append("  Config A wins: ").append(getConfigAWins())
                .append(" (").append(String.format("%.1f%%", getConfigAWinRate() * 100)).append(")\n");
            sb.append("  Config B wins: ").append(getConfigBWins())
                .append(" (").append(String.format("%.1f%%", (1 - getConfigAWinRate()) * 100)).append(")\n");
            sb.append("  Draws: ").append(getDraws()).append("\n\n");

            sb.append("Statistics:\n");
            sb.append("  Average game length: ")
                .append(String.format("%.1f", getAverageGameLength())).append(" turns\n");
            sb.append("  P-value: ").append(String.format("%.4f", getPValue()));

            if (getPValue() < 0.05) {
                sb.append(" (significant at 95% confidence)\n");
            } else {
                sb.append(" (not significant)\n");
            }

            sb.append("\nRecommendation: ").append(getRecommendation()).append("\n");

            if (getRecommendation().equals("PREFER_CONFIG_A")) {
                sb.append("  → Configuration A is significantly better\n");
            } else if (getRecommendation().equals("PREFER_CONFIG_B")) {
                sb.append("  → Configuration B is significantly better\n");
            } else {
                sb.append("  → No significant difference detected\n");
            }

            sb.append("\n===========================================\n");

            return sb.toString();
        }
    }
}
