package com.developingstorm.games.sad.fx;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

/**
 * JavaFX dialog for creating a new game.
 * Collects player names, types (Human/Robot), and map selection.
 */
public class NewGameDialog extends Dialog<NewGameDialog.NewGameSettings> {

    public static class NewGameSettings {
        public String player1Name;
        public String player2Name;
        public int player1Type; // 0=Human, 1=Robot
        public int player2Type;
        public String mapResource; // e.g. "MedMap.sdm"
    }

    private static class MapOption {
        final String displayName;
        final String resource;

        MapOption(String displayName, String resource) {
            this.displayName = displayName;
            this.resource = resource;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public NewGameDialog(Window owner) {
        setTitle("New Game");
        setHeaderText("Create a new game. Pick the players and the map.");
        initOwner(owner);

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 20));

        // Player 1
        TextField player1Name = new TextField("Player 1");
        ComboBox<String> player1Type = new ComboBox<>();
        player1Type.getItems().addAll("Human", "Robot");
        player1Type.setValue("Human");

        grid.add(new Label("Player 1:"), 0, 0);
        grid.add(player1Name, 1, 0);
        grid.add(player1Type, 2, 0);

        // Player 2
        TextField player2Name = new TextField("AI Player");
        ComboBox<String> player2Type = new ComboBox<>();
        player2Type.getItems().addAll("Human", "Robot");
        player2Type.setValue("Robot");

        grid.add(new Label("Player 2:"), 0, 1);
        grid.add(player2Name, 1, 1);
        grid.add(player2Type, 2, 1);

        // Map selection
        ComboBox<MapOption> mapChoice = new ComboBox<>();
        mapChoice.getItems().addAll(
            new MapOption("MedMap (Small)", "MedMap.sdm"),
            new MapOption("BorderLand (Large)", "BorderLand.sdm"),
            new MapOption("SampleMap (Large)", "SampleMap.sdm")
        );
        mapChoice.setValue(mapChoice.getItems().get(0));

        grid.add(new Label("Map:"), 0, 2);
        grid.add(mapChoice, 1, 2);

        getDialogPane().setContent(grid);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                NewGameSettings settings = new NewGameSettings();
                settings.player1Name = player1Name.getText().trim();
                settings.player2Name = player2Name.getText().trim();
                if (settings.player1Name.isEmpty()) settings.player1Name = "Player 1";
                if (settings.player2Name.isEmpty()) settings.player2Name = "Player 2";
                settings.player1Type = "Robot".equals(player1Type.getValue()) ? 1 : 0;
                settings.player2Type = "Robot".equals(player2Type.getValue()) ? 1 : 0;
                settings.mapResource = mapChoice.getValue().resource;
                return settings;
            }
            return null;
        });
    }
}
