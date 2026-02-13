package com.developingstorm.games.sad.controller;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.GameState;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import java.util.List;

/**
 * Implementation of GameQueryService that reads from the Game instance.
 * All access is read-only and thread-safe.
 */
public class GameQueryServiceImpl implements GameQueryService {

    private final Game game;

    public GameQueryServiceImpl(Game game) {
        this.game = game;
    }

    @Override
    public GameState getGameState() {
        return game.getGameState();
    }

    @Override
    public Unit getSelectedUnit() {
        return game.selectedUnit();
    }

    @Override
    public Player getCurrentPlayer() {
        return game.currentPlayer();
    }

    @Override
    public Player getHumanPlayer() {
        return game.getHumanPlayer();
    }

    @Override
    public Player[] getPlayers() {
        return game.getPlayers();
    }

    @Override
    public int getTurn() {
        return game.getTurn();
    }

    @Override
    public Board getBoard() {
        return game.getBoard();
    }

    @Override
    public List<Unit> getUnitsAtLocation(Location location) {
        return game.unitsAtLocation(location);
    }

    @Override
    public Unit getUnitAtLocation(Location location) {
        return game.unitAtLocation(location);
    }

    @Override
    public City getCityAtLocation(Location location) {
        return game.cityAtLocation(location);
    }

    @Override
    public boolean isCity(Location location) {
        return game.isCity(location);
    }

    @Override
    public List<Unit> getAllUnits() {
        return game.units();
    }

    @Override
    public Unit getUnitById(long unitId) {
        return game.getUnitById(unitId);
    }

    @Override
    public boolean isPaused() {
        return game.isPaused();
    }

    @Override
    public Game getGame() {
        return game;
    }
}
