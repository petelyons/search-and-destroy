package com.developingstorm.games.sad.testing;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.UnitNames;
import com.developingstorm.games.sad.Vision;
import com.developingstorm.games.sad.brain.AIConfiguration;
import com.developingstorm.games.sad.brain.RobotBrain;
import com.developingstorm.games.sad.events.GameEvent;
import com.developingstorm.games.sad.events.GameEventBus;
import com.developingstorm.games.sad.events.GameEventListener;
import com.developingstorm.games.sad.events.GameEventType;
import com.developingstorm.games.sad.events.GameOverEvent;
import com.developingstorm.games.sad.events.NewTurnEvent;
import com.developingstorm.games.sad.events.TurnCompleteEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runs games without UI for automated testing and AI evaluation.
 *
 * Features:
 * - No rendering or display
 * - Accelerated execution (no animation delays)
 * - Configurable turn limit to prevent infinite games
 * - Turn-by-turn monitoring and reporting
 * - Automatic timeout detection (aborts if turn takes >3s longer than previous)
 * - AI configuration support for A/B testing
 * - Game outcome reporting
 */
public class HeadlessGameRunner {

    private final HexBoardMap map;
    private final HexBoardContext ctx;
    private final int turnLimit;
    private final AIConfiguration player1Config;
    private final AIConfiguration player2Config;

    private Game game;
    private Thread gameThread;
    private final AtomicReference<GameResult> result;
    private final CountDownLatch gameDone;
    private final AtomicBoolean abortRequested;

    // Turn monitoring
    private final Map<Integer, Long> lastTurnDuration = new HashMap<>();
    private final Map<Integer, TurnCompleteEvent> turnMetrics = new HashMap<>();
    private long turnStartTime;
    private int lastTurn = -1;
    private boolean turnLimitReached = false;

    /**
     * Create a headless game runner with default settings.
     *
     * @param map The map to play on
     * @param ctx The hex board context
     */
    public HeadlessGameRunner(HexBoardMap map, HexBoardContext ctx) {
        this(map, ctx, 200, null, null);
    }

    /**
     * Create a headless game runner with custom configurations.
     *
     * @param map The map to play on
     * @param ctx The hex board context
     * @param turnLimit Maximum turns before declaring stalemate (0 = no limit)
     * @param player1Config AI configuration for player 1 (null = use defaults)
     * @param player2Config AI configuration for player 2 (null = use defaults)
     */
    public HeadlessGameRunner(
        HexBoardMap map,
        HexBoardContext ctx,
        int turnLimit,
        AIConfiguration player1Config,
        AIConfiguration player2Config
    ) {
        this.map = map;
        this.ctx = ctx;
        this.turnLimit = turnLimit;
        this.player1Config = player1Config;
        this.player2Config = player2Config;
        this.result = new AtomicReference<>();
        this.gameDone = new CountDownLatch(1);
        this.abortRequested = new AtomicBoolean(false);
    }

    /**
     * Initialize the game with AI players.
     */
    private void initializeGame() {
        // Create players
        Player[] players = new Player[2];

        // Player 1 (AI)
        Robot robot1 = new Robot("AI Player 1", 1);
        if (player1Config != null) {
            robot1.setBrain(new RobotBrain(robot1, player1Config));
        } else {
            robot1.setBrain(new RobotBrain(robot1));
        }
        players[0] = robot1;

        // Player 2 (AI)
        Robot robot2 = new Robot("AI Player 2", 2);
        if (player2Config != null) {
            robot2.setBrain(new RobotBrain(robot2, player2Config));
        } else {
            robot2.setBrain(new RobotBrain(robot2));
        }
        players[1] = robot2;

        // Assign names
        UnitNames.autoAssignThemes(players.length);

        // Create game
        game = new Game(players, map, ctx);

        // Subscribe to events
        GameEventBus eventBus = game.getEventBus();
        eventBus.addListener(
            new GameEventListener() {
                @Override
                public void onGameEvent(GameEvent event) {
                    if (event instanceof GameOverEvent) {
                        handleGameOver((GameOverEvent) event);
                    } else if (event instanceof NewTurnEvent) {
                        handleTurnStart((NewTurnEvent) event);
                    } else if (
                        event instanceof
                            com.developingstorm.games.sad.events.TurnCompleteEvent
                    ) {
                        handleTurnComplete(
                            (com.developingstorm.games.sad.events.TurnCompleteEvent) event
                        );
                    }
                }

                @Override
                public GameEventType[] getInterestedEventTypes() {
                    return new GameEventType[] {
                        GameEventType.GAME_OVER,
                        GameEventType.NEW_TURN,
                        GameEventType.TURN_ENDED,
                    };
                }
            }
        );
    }

    /**
     * Handle turn start - monitor turn duration and report status.
     */
    private void handleTurnStart(NewTurnEvent event) {
        int currentTurn = event.getTurnNumber();

        // Check for turn limit stalemate (only trigger once)
        if (turnLimit > 0 && currentTurn > turnLimit && !turnLimitReached) {
            turnLimitReached = true;
            System.out.println(
                "\nTurn limit reached (" + turnLimit + " turns)"
            );
            System.out.flush();
            abortRequested.set(true);
            result.set(
                new GameResult(
                    null,
                    currentTurn,
                    0,
                    GameResult.EndReason.STALEMATE,
                    "Turn limit reached"
                )
            );
            game.end();
            return;
        }

        // If already aborted, don't process this turn
        if (abortRequested.get()) {
            return;
        }

        // Determine which player based on turn number (turns alternate between players)
        // Turn 1 = Player 1, Turn 2 = Player 2, Turn 3 = Player 1, etc.
        int playerIndex = ((currentTurn - 1) % game.getPlayers().length);
        Player currentPlayer = game.getPlayers()[playerIndex];

        // Check for turn timeout using metrics from TurnCompleteEvent
        // Note: We check all players since events may arrive out of order
        for (Player player : game.getPlayers()) {
            TurnCompleteEvent lastMetrics = turnMetrics.get(player.getId());
            if (lastMetrics != null) {
                long turnDuration = lastMetrics.getDurationMs();
                Long baselineDuration = lastTurnDuration.get(player.getId());

                if (baselineDuration != null) {
                    // Check if turn took >3 seconds longer than baseline
                    long threshold = baselineDuration + 3000;
                    if (turnDuration > threshold) {
                        System.err.println(
                            "ABORT: Player " +
                                player.getId() +
                                " turn took " +
                                turnDuration +
                                "ms, " +
                                "exceeding threshold of " +
                                threshold +
                                "ms (+3s from baseline " +
                                baselineDuration +
                                "ms)"
                        );
                        abortRequested.set(true);
                        result.set(
                            new GameResult(
                                null,
                                lastMetrics.getTurnNumber(),
                                0,
                                GameResult.EndReason.TIMEOUT,
                                "Turn exceeded time threshold by " +
                                    (turnDuration - threshold) +
                                    "ms"
                            )
                        );
                        game.end();
                        return;
                    }
                }

                // Update baseline for this player (use minimum duration seen)
                if (
                    baselineDuration == null || turnDuration < baselineDuration
                ) {
                    lastTurnDuration.put(player.getId(), turnDuration);
                }
            }
        }

        // Report turn status
        reportTurnStatus(currentTurn, currentPlayer);

        // Start timing this turn
        turnStartTime = System.currentTimeMillis();
        lastTurn = currentTurn;
    }

    /**
     * Handle turn completion event with metrics from the game thread.
     */
    private void handleTurnComplete(TurnCompleteEvent event) {
        // Store metrics for this player's turn
        turnMetrics.put(event.getPlayer().getId(), event);
    }

    /**
     * Report turn status to stdout.
     */
    private void reportTurnStatus(int turn, Player player) {
        // Count cities per player
        int[] cityCounts = new int[game.getPlayers().length + 1];
        for (City city : game.getBoard().getCities()) {
            Player owner = city.getOwner();
            if (owner != null) {
                cityCounts[owner.getId()]++;
            }
        }

        // Count occupied continents per player (continents with at least one owned city)
        @SuppressWarnings("unchecked")
        java.util.Set<
            com.developingstorm.games.sad.Continent
        >[] occupiedContinents = new java.util.Set[game.getPlayers().length +
        1];
        for (int i = 0; i < occupiedContinents.length; i++) {
            occupiedContinents[i] = new java.util.HashSet<>();
        }

        for (City city : game.getBoard().getCities()) {
            Player owner = city.getOwner();
            if (owner != null) {
                com.developingstorm.games.sad.Continent continent =
                    city.getContinent();
                if (continent != null) {
                    occupiedContinents[owner.getId()].add(continent);
                }
            }
        }

        // Get turn duration from metrics (this player's actual turn duration)
        long turnDuration = 0;
        TurnCompleteEvent metrics = turnMetrics.get(player.getId());
        if (metrics != null) {
            turnDuration = metrics.getDurationMs();
        }

        // Count units per player by type
        int[] infantryCounts = new int[game.getPlayers().length + 1];
        int[] armorCounts = new int[game.getPlayers().length + 1];
        int[] fighterCounts = new int[game.getPlayers().length + 1];
        int[] bomberCounts = new int[game.getPlayers().length + 1];
        int[] transportCounts = new int[game.getPlayers().length + 1];
        int[] destroyerCounts = new int[game.getPlayers().length + 1];
        int[] submarineCounts = new int[game.getPlayers().length + 1];
        int[] cruiserCounts = new int[game.getPlayers().length + 1];
        int[] battleshipCounts = new int[game.getPlayers().length + 1];
        int[] carrierCounts = new int[game.getPlayers().length + 1];
        int[] totalUnitCounts = new int[game.getPlayers().length + 1];

        // Create a snapshot to avoid ConcurrentModificationException
        List<Unit> unitSnapshot = new ArrayList<>(game.units());

        for (Unit unit : unitSnapshot) {
            Player owner = unit.getOwner();
            if (owner != null) {
                totalUnitCounts[owner.getId()]++;
                Type type = unit.getType();
                if (type == Type.INFANTRY) {
                    infantryCounts[owner.getId()]++;
                } else if (type == Type.ARMOR) {
                    armorCounts[owner.getId()]++;
                } else if (type == Type.FIGHTER) {
                    fighterCounts[owner.getId()]++;
                } else if (type == Type.BOMBER) {
                    bomberCounts[owner.getId()]++;
                } else if (type == Type.TRANSPORT) {
                    transportCounts[owner.getId()]++;
                } else if (type == Type.DESTROYER) {
                    destroyerCounts[owner.getId()]++;
                } else if (type == Type.SUBMARINE) {
                    submarineCounts[owner.getId()]++;
                } else if (type == Type.CRUISER) {
                    cruiserCounts[owner.getId()]++;
                } else if (type == Type.BATTLESHIP) {
                    battleshipCounts[owner.getId()]++;
                } else if (type == Type.CARRIER) {
                    carrierCounts[owner.getId()]++;
                }
            }
        }

        // Calculate exploration percentage (sample every 4th hex for speed)
        int totalSamples = 0;
        int exploredSamples = 0;

        for (int x = 0; x < map.getWidth(); x += 4) {
            for (int y = 0; y < map.getHeight(); y += 4) {
                totalSamples++;
                Location loc = Location.get(x, y);
                // Check if either player has explored this hex
                for (Player p : game.getPlayers()) {
                    if (p.isExplored(loc)) {
                        exploredSamples++;
                        break;
                    }
                }
            }
        }

        double explorationPct = (exploredSamples * 100.0) / totalSamples;

        // Report
        System.out.printf("T%3d|P%d|Cities:", turn, player.getId());
        for (int i = 1; i < cityCounts.length; i++) {
            if (i > 1) System.out.print("/");
            System.out.printf("%d", cityCounts[i]);
        }

        System.out.printf("|Conts:");
        for (int i = 1; i < occupiedContinents.length; i++) {
            if (i > 1) System.out.print("/");
            System.out.printf("%d", occupiedContinents[i].size());
        }

        System.out.printf("|Units:");
        for (int i = 1; i < cityCounts.length; i++) {
            if (i > 1) System.out.print("/");
            System.out.printf(
                "%d(I%d,A%d,F%d,B%d,T%d,D%d,S%d,C%d,BB%d,CV%d)",
                totalUnitCounts[i],
                infantryCounts[i],
                armorCounts[i],
                fighterCounts[i],
                bomberCounts[i],
                transportCounts[i],
                destroyerCounts[i],
                submarineCounts[i],
                cruiserCounts[i],
                battleshipCounts[i],
                carrierCounts[i]
            );
        }

        System.out.printf(
            "|Exp:%.0f%%|Time:%dms%n",
            explorationPct,
            turnDuration
        );
        System.out.flush(); // Ensure output is visible immediately
    }

    /**
     * Run the game to completion.
     *
     * @return The game result
     * @throws InterruptedException if the thread is interrupted
     */
    public GameResult run() throws InterruptedException {
        return run(0);
    }

    /**
     * Run the game to completion with a timeout.
     *
     * @param timeoutSeconds Maximum seconds to wait (0 = no timeout)
     * @return The game result, or null if timed out
     * @throws InterruptedException if the thread is interrupted
     */
    public GameResult run(int timeoutSeconds) throws InterruptedException {
        System.out.println("Initializing game...");
        System.out.flush();
        initializeGame();

        System.out.println(
            "Starting game (turn limit: " +
                (turnLimit > 0 ? turnLimit : "none") +
                ")"
        );
        System.out.flush();

        // Run game in same thread (simpler, easier to debug)
        try {
            long startTime = System.currentTimeMillis();
            game.play();
            long endTime = System.currentTimeMillis();

            System.out.println("\nGame finished after " + game.turn + " turns");
            System.out.flush();

            // Check if already aborted
            if (abortRequested.get()) {
                return result.get();
            }

            // Check for turn limit stalemate
            if (turnLimit > 0 && game.turn >= turnLimit) {
                return new GameResult(
                    null,
                    game.turn,
                    (endTime - startTime) / 1000.0,
                    GameResult.EndReason.STALEMATE
                );
            }

            // Normal completion
            return result.get() != null
                ? result.get()
                : new GameResult(
                      null,
                      game.turn,
                      (endTime - startTime) / 1000.0,
                      GameResult.EndReason.DRAW
                  );
        } catch (SaDException e) {
            // If we already aborted (turn limit or timeout), return that result silently
            // (exceptions like DEAD UNIT are expected when aborting mid-game)
            if (abortRequested.get() && result.get() != null) {
                return result.get();
            }

            // Otherwise this is a real game error
            System.err.println("\nGame error: " + e.getMessage());
            e.printStackTrace();
            return new GameResult(
                null,
                game.turn,
                0,
                GameResult.EndReason.ERROR,
                e.getMessage()
            );
        } catch (Exception e) {
            // If we already aborted, return that result silently
            if (abortRequested.get() && result.get() != null) {
                return result.get();
            }

            // Otherwise this is an unexpected error
            System.err.println("\nUnexpected error during game execution:");
            e.printStackTrace();
            return new GameResult(
                null,
                game.turn,
                0,
                GameResult.EndReason.ERROR,
                e.getMessage()
            );
        }
    }

    /**
     * Handle game over event.
     */
    private void handleGameOver(GameOverEvent event) {
        if (abortRequested.get()) {
            return; // Already handled
        }

        Player winner = event.getWinner();

        // Only set result if we haven't already aborted
        if (!abortRequested.get()) {
            result.set(
                new GameResult(
                    winner != null ? winner.getId() : null,
                    game.turn,
                    0, // Will be set by run method
                    winner != null
                        ? GameResult.EndReason.VICTORY
                        : GameResult.EndReason.DRAW
                )
            );
        }
    }

    /**
     * Get the game instance (for accessing statistics).
     */
    public Game getGame() {
        return game;
    }

    /**
     * Result of a headless game run.
     */
    public static class GameResult {

        public enum EndReason {
            VICTORY, // One player won
            DRAW, // No winner
            STALEMATE, // Turn limit reached
            TIMEOUT, // Turn took too long
            ERROR, // Game crashed
        }

        private final Integer winnerPlayerNumber;
        private final int turns;
        private final double durationSeconds;
        private final EndReason endReason;
        private final String errorMessage;

        public GameResult(
            Integer winnerPlayerNumber,
            int turns,
            double durationSeconds,
            EndReason endReason
        ) {
            this(winnerPlayerNumber, turns, durationSeconds, endReason, null);
        }

        public GameResult(
            Integer winnerPlayerNumber,
            int turns,
            double durationSeconds,
            EndReason endReason,
            String errorMessage
        ) {
            this.winnerPlayerNumber = winnerPlayerNumber;
            this.turns = turns;
            this.durationSeconds = durationSeconds;
            this.endReason = endReason;
            this.errorMessage = errorMessage;
        }

        public Integer getWinnerPlayerNumber() {
            return winnerPlayerNumber;
        }

        public int getTurns() {
            return turns;
        }

        public double getDurationSeconds() {
            return durationSeconds;
        }

        public EndReason getEndReason() {
            return endReason;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Game Result: ");
            switch (endReason) {
                case VICTORY:
                    sb
                        .append("Player ")
                        .append(winnerPlayerNumber)
                        .append(" wins");
                    break;
                case DRAW:
                    sb.append("Draw");
                    break;
                case STALEMATE:
                    sb.append("Stalemate (turn limit reached)");
                    break;
                case TIMEOUT:
                    sb.append("Timeout: ").append(errorMessage);
                    break;
                case ERROR:
                    sb.append("Error: ").append(errorMessage);
                    break;
            }
            sb.append(" after ").append(turns).append(" turns");
            sb
                .append(" (")
                .append(String.format("%.1f", durationSeconds))
                .append("s)");
            return sb.toString();
        }
    }
}
