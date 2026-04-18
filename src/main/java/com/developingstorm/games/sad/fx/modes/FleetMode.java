package com.developingstorm.games.sad.fx.modes;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.controller.GameController;
import com.developingstorm.games.sad.controller.GameQueryService;
import com.developingstorm.games.sad.fx.MapCanvas;
import com.developingstorm.games.sad.fx.UIMode;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class FleetMode extends AbstractMapCanvasMode {

    private Unit flagship;
    private final List<Unit> selected = new ArrayList<>();

    public FleetMode(
        MapCanvas canvas,
        Game game,
        GameController controller,
        GameQueryService query
    ) {
        super(canvas, game, controller, query);
    }

    @Override
    public UIMode getMode() {
        return UIMode.FLEET;
    }

    public void setFlagship(Unit flagship) {
        this.flagship = flagship;
    }

    @Override
    public void enter() {
        if (flagship == null) {
            throw new IllegalStateException(
                "setFlagship() required before entering"
            );
        }
        selected.clear();
    }

    @Override
    public void exit() {
        flagship = null;
        selected.clear();
    }

    @Override
    public void onMousePressed(MouseEvent event, Location location) {
        if (!isPrimaryButton(event)) return;
        Unit target = query.getUnitAtLocation(location);
        if (target == null || !isEligibleMember(target)) return;
        if (selected.contains(target)) {
            selected.remove(target);
        } else {
            selected.add(target);
        }
        canvas.refresh();
    }

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && !selected.isEmpty()) {
            controller.formFleet(flagship, selected);
            canvas.getModeManager().switchMode(UIMode.GAME);
            return true;
        }
        if (event.getCode() == KeyCode.ESCAPE) {
            canvas.getModeManager().switchMode(UIMode.GAME);
            return true;
        }
        return false;
    }

    @Override
    public void draw(GraphicsContext gc) {
        gc.setStroke(Color.GOLD);
        gc.setLineWidth(3.0);
        for (Unit u : selected) {
            double[] c = canvas.getHexCenter(u.getLocation());
            gc.strokeOval(c[0] - 18, c[1] - 18, 36, 36);
        }
        if (flagship != null) {
            double[] fc = canvas.getHexCenter(flagship.getLocation());
            gc.setStroke(Color.ORANGE);
            gc.setLineWidth(3.5);
            gc.strokeOval(fc[0] - 20, fc[1] - 20, 40, 40);
        }
    }

    private boolean isEligibleMember(Unit u) {
        if (u.getOwner() != flagship.getOwner()) return false;
        if (u == flagship) return false;
        if (u.getFleet() != null) return false;
        if (u.getTravel() != Travel.SEA) return false;
        if (u.isCarried()) return false;
        Type t = u.getType();
        return (
            t == Type.DESTROYER ||
            t == Type.CRUISER ||
            t == Type.SUBMARINE ||
            t == Type.CARRIER ||
            t == Type.BATTLESHIP ||
            t == Type.TRANSPORT
        );
    }
}
