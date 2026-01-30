package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.commands.DisbandUnitCommand;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.orders.Explore;
import com.developingstorm.games.sad.orders.HeadHome;
import com.developingstorm.games.sad.orders.Sentry;
import com.developingstorm.games.sad.orders.SkipTurn;
import com.developingstorm.games.sad.orders.Unload;

/**
 * Bridge class that handles game commands.
 * Provides a clean interface between the UI (GameView) and the game controller.
 * Encapsulates all the command execution logic.
 */
public class GameCommandHandler {

    private final GameController controller;
    private final GameQueryService query;

    public GameCommandHandler(
        GameController controller,
        GameQueryService query
    ) {
        this.controller = controller;
        this.query = query;
    }

    /**
     * Put a unit in sentry mode.
     * Uses the controller's issueOrder method which properly queues commands.
     */
    public void sentry(Unit unit) {
        if (unit == null) return;
        controller.issueOrder(unit, new Sentry(query.getGame(), unit));
        controller.resumeGame(unit);
    }

    /**
     * Skip a unit's turn.
     * Uses the controller's issueOrder method which properly queues commands.
     */
    public void skipTurn(Unit unit) {
        if (unit == null) return;
        controller.issueOrder(unit, new SkipTurn(query.getGame(), unit));
        controller.resumeGame(unit);
    }

    /**
     * Order a unit to explore.
     * Uses the controller's issueOrder method which properly queues commands.
     */
    public void explore(Unit unit) {
        if (unit == null) return;
        controller.issueOrder(unit, new Explore(query.getGame(), unit));
        controller.resumeGame(unit);
    }

    /**
     * Unload units from a transport/carrier.
     * Uses the controller's issueOrder method which properly queues commands.
     */
    public void unload(Unit unit) {
        if (unit == null) return;
        controller.issueOrder(unit, new Unload(query.getGame(), unit));
        controller.resumeGame(unit);
    }

    /**
     * Order a unit to head home.
     * Uses the controller's issueOrder method which properly queues commands.
     */
    public void headHome(Unit unit) {
        if (unit == null) return;
        controller.issueOrder(unit, new HeadHome(query.getGame(), unit));
        controller.resumeGame(unit);
    }

    /**
     * Disband a unit (kill it).
     * Uses DisbandUnitCommand to ensure thread-safe execution.
     */
    public void disband(Unit unit) {
        if (unit == null) return;

        // Submit command via controller
        controller.postGameAction(() -> {
            DisbandUnitCommand cmd = new DisbandUnitCommand(unit);
            cmd.execute(query.getGame());
        });
        controller.resumeGame(null);
    }

    /**
     * Select a unit (delegates to controller).
     */
    public void selectUnit(Unit unit) {
        controller.selectUnit(unit);
    }
}
