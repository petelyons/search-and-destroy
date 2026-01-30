package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.fx.MapCanvas;
import com.developingstorm.games.sad.fx.UIMode;
import com.developingstorm.games.sad.fx.sprites.FxArrowSprite;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * Explore mode for setting exploration paths.
 * Similar to PathMode but for unit exploration destinations.
 *
 * TODO: Implement full exploration path logic.
 * Matches Swing's ExploreCommander/ExploreModeController behavior.
 */
public class ExploreMode extends AbstractMapCanvasMode {

    private City originCity;
    private Travel travelType;
    private FxArrowSprite arrow;

    public ExploreMode(
        MapCanvas canvas,
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        super(canvas, game, controller, query);
    }

    @Override
    public UIMode getMode() {
        return UIMode.EXPLORE;
    }

    /**
     * Set the origin city and travel type for exploration.
     */
    public void setOrigin(City city, Travel travel) {
        this.originCity = city;
        this.travelType = travel;

        // Create arrow
        Color arrowColor = switch (travel) {
            case AIR -> Color.LIGHTGRAY;
            case SEA -> Color.LIGHTBLUE;
            case LAND -> Color.LIGHTGREEN;
            default -> Color.WHITE;
        };

        this.arrow = new FxArrowSprite(arrowColor);
    }

    @Override
    public void enter() {
        if (originCity == null || travelType == null) {
            throw new IllegalStateException("Must call setOrigin() before entering ExploreMode");
        }
    }

    @Override
    public void exit() {
        originCity = null;
        travelType = null;
        arrow = null;
    }

    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        if (!isPrimaryButton(event)) {
            return;
        }

        // TODO: Implement exploration destination setting
        System.out.println("ExploreMode: Set exploration destination to " + location);

        // Return to game mode
        canvas.getModeManager().switchMode(UIMode.GAME);
    }

    @Override
    public void onMouseMoved(MouseEvent event, Location location) {
        // Update arrow to follow cursor
        if (originCity != null && arrow != null) {
            double[] originCenter = canvas.getHexCenter(originCity.getLocation());
            double[] cursorPos = {event.getX(), event.getY()};
            arrow.setArrow(
                originCenter[0],
                originCenter[1],
                cursorPos[0],
                cursorPos[1]
            );
            canvas.refresh();
        }
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            canvas.getModeManager().switchMode(UIMode.GAME);
            return true;
        }
        return false;
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (arrow != null) {
            arrow.draw(gc);
        }
    }
}
