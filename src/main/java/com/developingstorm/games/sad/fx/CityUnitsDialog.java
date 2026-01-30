package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Unit;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 * Dialog for selecting units at a city location.
 * Matches Swing CityDialog behavior.
 */
public class CityUnitsDialog extends Dialog<List<Unit>> {

    private ListView<UnitListItem> unitListView;

    public CityUnitsDialog(City city, List<Unit> units) {
        setTitle("Units at " + city.getName());
        setHeaderText("Issue Orders to Unit(s)");
        initModality(Modality.APPLICATION_MODAL);

        // Create list view with unit items
        unitListView = new ListView<>();
        unitListView
            .getSelectionModel()
            .setSelectionMode(SelectionMode.MULTIPLE);

        // Populate with units
        for (Unit unit : units) {
            unitListView.getItems().add(new UnitListItem(unit));
        }

        // Set preferred size
        unitListView.setPrefWidth(300);
        unitListView.setPrefHeight(200);

        // Layout
        VBox content = new VBox(10);
        Label label = new Label("Select unit(s):");
        content.getChildren().addAll(label, unitListView);
        content.setStyle("-fx-padding: 10;");

        getDialogPane().setContent(content);
        getDialogPane()
            .getButtonTypes()
            .addAll(ButtonType.OK, ButtonType.CANCEL);

        // Convert result
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                List<Unit> selectedUnits = new ArrayList<>();
                for (UnitListItem item : unitListView
                    .getSelectionModel()
                    .getSelectedItems()) {
                    selectedUnits.add(item.getUnit());
                }
                return selectedUnits;
            }
            return null;
        });

        // Handle double-click
        unitListView.setOnMouseClicked(event -> {
            if (
                event.getClickCount() == 2 &&
                !unitListView.getSelectionModel().isEmpty()
            ) {
                (
                    (javafx.scene.control.Button) getDialogPane().lookupButton(
                        ButtonType.OK
                    )
                ).fire();
            }
        });
    }

    /**
     * Wrapper class for displaying units in the list.
     */
    private static class UnitListItem {

        private final Unit unit;

        public UnitListItem(Unit unit) {
            this.unit = unit;
        }

        public Unit getUnit() {
            return unit;
        }

        @Override
        public String toString() {
            return unit.toUIString();
        }
    }
}
