package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.commands.SetPathCommand;
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
 * Path setting mode for city production paths.
 * Shows an arrow from origin city following the cursor.
 * Click on a valid destination city to set the path.
 *
 * Matches Swing's PathsCommander/PathsModeController behavior.
 */
public class PathMode extends AbstractMapCanvasMode {

    private City originCity;
    private Travel travelType;
    private FxArrowSprite arrow;

    public PathMode(
        MapCanvas canvas,
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        super(canvas, game, controller, query);
    }

    @Override
    public UIMode getMode() {
        return UIMode.PATHS;
    }

    /**
     * Set the origin city and travel type for this path.
     * Call this before entering the mode.
     */
    public void setOrigin(City city, Travel travel) {
        this.originCity = city;
        this.travelType = travel;

        // Create arrow with appropriate color
        Color arrowColor = switch (travel) {
            case AIR -> Color.GRAY;
            case SEA -> Color.BLUE;
            case LAND -> Color.GREEN.darker().darker();
            default -> Color.WHITE;
        };

        this.arrow = new FxArrowSprite(arrowColor);
    }

    @Override
    public void enter() {
        if (originCity == null || travelType == null) {
            throw new IllegalStateException(
                "Must call setOrigin() before entering PathMode"
            );
        }
    }

    @Override
    public void exit() {
        // Clean up state
        originCity = null;
        travelType = null;
        arrow = null;
    }

    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        if (!isPrimaryButton(event)) {
            return;
        }

        City clickedCity = query.getCityAtLocation(location);
        if (clickedCity != null && isValidDestination(clickedCity)) {
            // Store references for use in command
            final City destCity = clickedCity;
            final City origCity = originCity;
            final Travel travel = travelType;

            // Submit command using new concurrency model
            // Command will be executed on game thread, and MapUpdatedEvent will be fired automatically
            // MapUpdatedEvent listener in GameView will refresh paths automatically
            controller.postAndResume(() -> {
                SetPathCommand cmd = new SetPathCommand(
                    origCity,
                    destCity,
                    travel
                );
                cmd.execute(game);
            });

            // Return to game mode after path is set
            // MapUpdatedEvent will trigger path refresh automatically
            javafx.application.Platform.runLater(() -> {
                canvas.getModeManager().switchMode(UIMode.GAME);
            });
        }
    }

    @Override
    public void onMouseMoved(MouseEvent event, Location location) {
        // Update arrow to follow cursor
        if (originCity != null && arrow != null) {
            double[] originCenter = canvas.getHexCenter(
                originCity.getLocation()
            );
            double[] cursorPos = { event.getX(), event.getY() };
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
        switch (event.getCode()) {
            case ESCAPE:
                // Cancel path mode
                canvas.getModeManager().switchMode(UIMode.GAME);
                return true;
            case ENTER:
                // Set path if hovering over valid city
                // (Implementation would need cursor tracking)
                return true;
            default:
                return false;
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        // Draw the path arrow
        if (arrow != null) {
            arrow.draw(gc);
        }
    }

    /**
     * Check if a city is a valid destination for the current path.
     */
    private boolean isValidDestination(City destination) {
        if (originCity == null || travelType == null || destination == null) {
            return false;
        }

        // Can't set path to same city
        if (destination.equals(originCity)) {
            return false;
        }

        // Check travel-specific constraints
        if (travelType == Travel.SEA) {
            return destination.isCoastal();
        } else if (travelType == Travel.LAND) {
            return destination.shareContinent(originCity);
        }

        // AIR can go anywhere
        return true;
    }
}
