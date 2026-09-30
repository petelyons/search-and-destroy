package com.developingstorm.games.sad.fx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Small modal dialog shown while a game is being loaded from disk.
 */
public class LoadingDialog {

    private final Stage stage;

    public LoadingDialog(Window owner, String message) {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);

        Label label = new Label(message);

        VBox root = new VBox(12, spinner, label);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24, 36, 24, 36));

        stage = new Stage(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Loading");
        stage.setResizable(false);
        stage.setScene(new Scene(root));
    }

    public void show() {
        stage.show();
    }

    public void close() {
        stage.close();
    }
}
