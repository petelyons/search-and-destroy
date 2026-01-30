package com.developingstorm.games.sad.testing;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.events.CombatResolvedEvent;
import com.developingstorm.games.sad.events.GameEvent;
import com.developingstorm.games.sad.events.GameEventListener;
import com.developingstorm.games.sad.events.GameEventType;
import com.developingstorm.games.sad.events.NewTurnEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects detailed statistics during a game for analysis and comparison.
 *
 * Tracks:
 * - Per-player metrics (units, cities, production, casualties)
 * - Combat statistics (win rate, power ratios)
 * - Territory control over time
 * - Critical events timeline
 */
public class GameStatistics implements GameEventListener {

    private final Game game;
    private final Map<Integer, PlayerStats> playerStats;
    private final List<GameEvent> eventTimeline;
    private int currentTurn;

    public GameStatistics(Game game) {
        this.game = game;
        this.playerStats = new HashMap<>();
        this.eventTimeline = new ArrayList<>();
        this.currentTurn = 0;

        // Initialize stats for each player
        for (Player player : game.getPlayers()) {
            playerStats.put(player.getId(), new PlayerStats(player.getId()));
        }

        // Subscribe to game events
        game.getEventBus().addListener(this);
    }

    @Override
    public void onGameEvent(GameEvent event) {
        // Record event in timeline
        eventTimeline.add(event);

        // Process specific event types
        if (event instanceof NewTurnEvent) {
            handleNewTurn((NewTurnEvent) event);
        } else if (event instanceof CombatResolvedEvent) {
            handleCombat((CombatResolvedEvent) event);
        }
    }

    @Override
    public GameEventType[] getInterestedEventTypes() {
        return new GameEventType[] {
            GameEventType.NEW_TURN,
            GameEventType.COMBAT_RESOLVED,
        };
    }

    /**
     * Handle new turn event - snapshot current state.
     */
    private void handleNewTurn(NewTurnEvent event) {
        currentTurn = game.turn;

        // Snapshot current state for each player
        for (Player player : game.getPlayers()) {
            PlayerStats stats = playerStats.get(player.getId());
            if (stats != null) {
                stats.recordTurn(
                    currentTurn,
                    player.unitCount(),
                    player.cityCount()
                );
            }
        }
    }

    /**
     * Handle combat event - track combat statistics.
     */
    private void handleCombat(CombatResolvedEvent event) {
        // Get combat result
        if (event.getResult() == null) {
            return;
        }

        // Extract combat participants
        Player attacker = event.getResult().getAttackerOwner();
        Player defender = event.getResult().getDefenderOwner();

        if (attacker == null || defender == null) {
            return;
        }

        PlayerStats attackerStats = playerStats.get(attacker.getId());
        PlayerStats defenderStats = playerStats.get(defender.getId());

        if (attackerStats != null && defenderStats != null) {
            // Track combat engagement
            attackerStats.combatEngagements++;
            defenderStats.combatEngagements++;

            // Track casualties based on who won
            if (event.getResult().attackerWon()) {
                // Attacker won - defender lost a unit
                defenderStats.unitsLost++;
                attackerStats.unitsKilled++;
                attackerStats.combatWins++;
            } else {
                // Defender won - attacker lost a unit
                attackerStats.unitsLost++;
                defenderStats.unitsKilled++;
                defenderStats.combatWins++;
            }
        }
    }

    /**
     * Get statistics for a specific player.
     */
    public PlayerStats getPlayerStats(int playerId) {
        return playerStats.get(playerId);
    }

    /**
     * Get all player statistics.
     */
    public Map<Integer, PlayerStats> getAllPlayerStats() {
        return new HashMap<>(playerStats);
    }

    /**
     * Get the event timeline.
     */
    public List<GameEvent> getEventTimeline() {
        return new ArrayList<>(eventTimeline);
    }

    /**
     * Get current turn number.
     */
    public int getCurrentTurn() {
        return currentTurn;
    }

    /**
     * Statistics for a single player.
     */
    public static class PlayerStats {

        private final int playerId;

        // Combat statistics
        public int combatEngagements = 0;
        public int combatWins = 0;
        public int unitsKilled = 0;
        public int unitsLost = 0;

        // Territory control timeline
        private final List<TurnSnapshot> turnSnapshots = new ArrayList<>();

        public PlayerStats(int playerId) {
            this.playerId = playerId;
        }

        /**
         * Record state at a specific turn.
         */
        public void recordTurn(int turn, int units, int cities) {
            turnSnapshots.add(new TurnSnapshot(turn, units, cities));
        }

        /**
         * Get combat win rate (0.0 to 1.0).
         */
        public double getCombatWinRate() {
            if (combatEngagements == 0) {
                return 0.0;
            }
            return (double) combatWins / combatEngagements;
        }

        /**
         * Get unit survival rate (0.0 to 1.0).
         */
        public double getUnitSurvivalRate() {
            int totalCombats = combatEngagements;
            if (totalCombats == 0) {
                return 1.0;
            }
            return 1.0 - ((double) unitsLost / totalCombats);
        }

        /**
         * Get kill/death ratio.
         */
        public double getKillDeathRatio() {
            if (unitsLost == 0) {
                return unitsKilled > 0 ? Double.POSITIVE_INFINITY : 0.0;
            }
            return (double) unitsKilled / unitsLost;
        }

        /**
         * Get final unit count.
         */
        public int getFinalUnitCount() {
            if (turnSnapshots.isEmpty()) {
                return 0;
            }
            return turnSnapshots.get(turnSnapshots.size() - 1).units;
        }

        /**
         * Get final city count.
         */
        public int getFinalCityCount() {
            if (turnSnapshots.isEmpty()) {
                return 0;
            }
            return turnSnapshots.get(turnSnapshots.size() - 1).cities;
        }

        /**
         * Get average units per turn.
         */
        public double getAverageUnits() {
            if (turnSnapshots.isEmpty()) {
                return 0.0;
            }
            return turnSnapshots
                .stream()
                .mapToInt(s -> s.units)
                .average()
                .orElse(0.0);
        }

        /**
         * Get average cities per turn.
         */
        public double getAverageCities() {
            if (turnSnapshots.isEmpty()) {
                return 0.0;
            }
            return turnSnapshots
                .stream()
                .mapToInt(s -> s.cities)
                .average()
                .orElse(0.0);
        }

        /**
         * Get all turn snapshots.
         */
        public List<TurnSnapshot> getTurnSnapshots() {
            return new ArrayList<>(turnSnapshots);
        }

        /**
         * Get player ID.
         */
        public int getPlayerId() {
            return playerId;
        }

        @Override
        public String toString() {
            return String.format(
                "Player %d: %d combats (%.1f%% wins), %d kills, %d losses (K/D: %.2f), Final: %d units, %d cities",
                playerId,
                combatEngagements,
                getCombatWinRate() * 100,
                unitsKilled,
                unitsLost,
                getKillDeathRatio(),
                getFinalUnitCount(),
                getFinalCityCount()
            );
        }
    }

    /**
     * Snapshot of player state at a specific turn.
     */
    public static class TurnSnapshot {

        public final int turn;
        public final int units;
        public final int cities;

        public TurnSnapshot(int turn, int units, int cities) {
            this.turn = turn;
            this.units = units;
            this.cities = cities;
        }
    }

    /**
     * Generate a summary report of the game statistics.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Game Statistics Report\n");
        sb.append("======================\n");
        sb.append("Turns: ").append(currentTurn).append("\n\n");

        for (PlayerStats stats : playerStats.values()) {
            sb.append(stats.toString()).append("\n");
        }

        sb.append("\nCombat Summary:\n");
        for (PlayerStats stats : playerStats.values()) {
            sb.append(
                String.format(
                    "  Player %d: Win Rate=%.1f%%, K/D=%.2f\n",
                    stats.getPlayerId(),
                    stats.getCombatWinRate() * 100,
                    stats.getKillDeathRatio()
                )
            );
        }

        sb.append("\nFinal State:\n");
        for (PlayerStats stats : playerStats.values()) {
            sb.append(
                String.format(
                    "  Player %d: %d units, %d cities (avg %.1f units, %.1f cities)\n",
                    stats.getPlayerId(),
                    stats.getFinalUnitCount(),
                    stats.getFinalCityCount(),
                    stats.getAverageUnits(),
                    stats.getAverageCities()
                )
            );
        }

        return sb.toString();
    }
}
