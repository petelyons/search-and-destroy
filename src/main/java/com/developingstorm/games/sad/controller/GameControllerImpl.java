package com.developingstorm.games.sad.controller;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.commands.AssignOrderCommand;
import com.developingstorm.games.sad.commands.ResumeGameCommand;
import com.developingstorm.games.sad.commands.SelectUnitCommand;

/**
 * Implementation of GameController that uses the command pattern.
 * This acts as an adapter/facade to provide a clean interface for UI commands.
 *
 * All commands are submitted to the game's lock-free command queue and processed
 * on the game thread, ensuring thread-safety and avoiding race conditions.
 */
public class GameControllerImpl implements GameController {

    private final Game game;

    public GameControllerImpl(Game game) {
        this.game = game;
    }

    @Override
    public void issueOrder(Unit unit, Order order) {
        // Submit command to the lock-free queue
        game.submitCommand(new AssignOrderCommand(unit, order));
    }

    @Override
    public void selectUnit(Unit unit) {
        // Submit command to the lock-free queue
        game.submitCommand(new SelectUnitCommand(unit));
    }

    @Override
    public void resumeGame(Unit unit) {
        // Submit resume command to the lock-free queue
        game.submitCommand(new ResumeGameCommand(unit));
    }

    @Override
    public void pauseGame() {
        // Submit pause command
        game.submitCommand(game2 -> {
            game2.transitionState(
                com.developingstorm.games.sad.GameState.RUNNING,
                com.developingstorm.games.sad.GameState.PAUSED
            );
        });
    }

    @Override
    public void trackLocation(Location location) {
        // Tracking is read-only and thread-safe, can call directly
        game.trackLocation(location);
    }

    @Override
    public void trackUnit(Unit unit) {
        // Tracking is read-only and thread-safe, can call directly
        game.trackUnit(unit);
    }

    @Override
    public void postGameAction(Runnable action) {
        // Submit as a lambda command
        game.submitCommand(game2 -> action.run());
    }

    @Override
    public void postAndResume(Runnable action) {
        // Submit action and resume in sequence
        game.submitCommand(game2 -> action.run());
        game.submitCommand(new ResumeGameCommand(null));
    }
}
