package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 * Dialog for selecting units at a city location and issuing orders to the
 * selection. Multiple units can be selected at once — non-modal action
 * buttons on the right apply to all selected units.
 */
public class CityUnitsDialog extends Dialog<Void> {

    public enum Action {
        SENTRY,
        UNLOAD,
        EXPLORE,
        HEAD_HOME,
        PATROL,
        BOMBARD,
        ESCORT,
        FORM_FLEET,
    }

    /**
     * Called when the user clicks an action button. Implementers apply the
     * action to every unit in {@code units} (single-selection actions will
     * pass a one-element list).
     */
    public interface ActionHandler {
        void handle(Action action, List<Unit> units);
    }

    private final ListView<UnitListItem> unitListView;

    public CityUnitsDialog(
        City city,
        List<Unit> units,
        ActionHandler handler
    ) {
        setTitle("Units at " + city.getName());
        setHeaderText("Select unit(s), then choose an action");
        initModality(Modality.APPLICATION_MODAL);

        unitListView = new ListView<>();
        unitListView
            .getSelectionModel()
            .setSelectionMode(SelectionMode.MULTIPLE);
        for (Unit unit : units) {
            unitListView.getItems().add(new UnitListItem(unit));
        }
        unitListView.setPrefWidth(280);
        unitListView.setPrefHeight(240);

        Button sentryBtn = new Button("Sentry / Load");
        Button unloadBtn = new Button("Unload");
        Button exploreBtn = new Button("Explore");
        Button headHomeBtn = new Button("Head Home");
        Button patrolBtn = new Button("Define Patrol...");
        Button bombardBtn = new Button("Bombard...");
        Button escortBtn = new Button("Escort...");
        Button formFleetBtn = new Button("Form Fleet...");

        Button[] allButtons = {
            sentryBtn,
            unloadBtn,
            exploreBtn,
            headHomeBtn,
            patrolBtn,
            bombardBtn,
            escortBtn,
            formFleetBtn,
        };
        double btnWidth = 160;
        for (Button b : allButtons) {
            b.setPrefWidth(btnWidth);
            b.setMaxWidth(Double.MAX_VALUE);
        }

        sentryBtn.setOnAction(e -> fire(handler, Action.SENTRY));
        unloadBtn.setOnAction(e -> fire(handler, Action.UNLOAD));
        exploreBtn.setOnAction(e -> fire(handler, Action.EXPLORE));
        headHomeBtn.setOnAction(e -> fire(handler, Action.HEAD_HOME));
        patrolBtn.setOnAction(e -> fire(handler, Action.PATROL));
        bombardBtn.setOnAction(e -> fire(handler, Action.BOMBARD));
        escortBtn.setOnAction(e -> fire(handler, Action.ESCORT));
        formFleetBtn.setOnAction(e -> fire(handler, Action.FORM_FLEET));

        VBox actionBox = new VBox(6);
        actionBox.setAlignment(Pos.TOP_LEFT);
        actionBox.getChildren().addAll(allButtons);
        actionBox.setStyle("-fx-padding: 0 0 0 10;");

        Runnable refreshButtons = () -> updateButtonStates(
            selectedUnits(),
            sentryBtn,
            unloadBtn,
            exploreBtn,
            headHomeBtn,
            patrolBtn,
            bombardBtn,
            escortBtn,
            formFleetBtn
        );

        unitListView
            .getSelectionModel()
            .getSelectedItems()
            .addListener(
                (javafx.collections.ListChangeListener<UnitListItem>) c ->
                    refreshButtons.run()
            );

        HBox main = new HBox(
            new VBox(4, new Label("Units:"), unitListView),
            actionBox
        );
        HBox.setHgrow(actionBox, Priority.NEVER);
        main.setStyle("-fx-padding: 10;");

        getDialogPane().setContent(main);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        setResultConverter(bt -> null);

        refreshButtons.run();
    }

    private void fire(ActionHandler handler, Action action) {
        List<Unit> sel = selectedUnits();
        if (sel.isEmpty()) {
            return;
        }
        handler.handle(action, sel);
        close();
    }

    private List<Unit> selectedUnits() {
        List<Unit> out = new ArrayList<>();
        for (UnitListItem item : unitListView
            .getSelectionModel()
            .getSelectedItems()) {
            out.add(item.getUnit());
        }
        return out;
    }

    private void updateButtonStates(
        List<Unit> sel,
        Button sentry,
        Button unload,
        Button explore,
        Button headHome,
        Button patrol,
        Button bombard,
        Button escort,
        Button formFleet
    ) {
        boolean any = !sel.isEmpty();
        boolean single = sel.size() == 1;

        sentry.setDisable(!any);
        explore.setDisable(!any);
        headHome.setDisable(!any);

        boolean anyCanCarry = false;
        boolean allSea = any;
        boolean allBombardCapable = any;
        for (Unit u : sel) {
            if (u.canCarry()) anyCanCarry = true;
            if (u.getTravel() != Travel.SEA) allSea = false;
            if (!(u.isBattleship() || u.isCruiser())) {
                allBombardCapable = false;
            }
        }

        unload.setDisable(!anyCanCarry);
        patrol.setDisable(!single);
        bombard.setDisable(!(single && allBombardCapable));
        escort.setDisable(!(single && allSea));

        boolean singleCapital = single && sel.get(0).getType().isCapitalShip();
        formFleet.setDisable(!singleCapital);
    }

    private static class UnitListItem {

        private final Unit unit;

        UnitListItem(Unit unit) {
            this.unit = unit;
        }

        Unit getUnit() {
            return unit;
        }

        @Override
        public String toString() {
            return unit.toUIString();
        }
    }
}
