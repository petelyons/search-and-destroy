package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.mapgen.MapSize;
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
        public MapSize mapSize;
    }

    public NewGameDialog(Window owner) {
        setTitle("New Game");
        setHeaderText("Create a new game. Pick the players and the map size.");
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

        // Map size: a fresh map is generated for every game
        ComboBox<MapSize> mapSize = new ComboBox<>();
        mapSize.getItems().addAll(MapSize.values());
        mapSize.setValue(MapSize.MEDIUM);

        grid.add(new Label("Map size:"), 0, 2);
        grid.add(mapSize, 1, 2);

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
                settings.mapSize = mapSize.getValue();
                return settings;
            }
            return null;
        });
    }
}
