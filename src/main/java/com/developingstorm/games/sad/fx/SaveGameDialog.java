package com.developingstorm.games.sad.fx;

import com.developingstorm.games.sad.util.json.JsonObj;
import com.developingstorm.games.sad.util.json.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * JavaFX dialog for loading saved games.
 * Groups save files by game name and shows only the most recent by default.
 */
public class SaveGameDialog {

    private File selectedFile;
    private final String saveDirectory;
    private ListView<SaveGameEntry> gameList;
    private CheckBox showAllVersionsCheckbox;
    private List<SaveGameEntry> allSaves;
    private Stage dialog;

    public SaveGameDialog(Window parent, String saveDirectory) {
        this.saveDirectory = saveDirectory;
        createDialog(parent);
        loadSaveFiles();
    }

    private void createDialog(Window parent) {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parent);
        dialog.setTitle("Load Game");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Top panel with checkbox
        showAllVersionsCheckbox = new CheckBox("Show all versions");
        showAllVersionsCheckbox.setOnAction(e -> refreshList());
        BorderPane.setMargin(showAllVersionsCheckbox, new Insets(0, 0, 10, 0));
        root.setTop(showAllVersionsCheckbox);

        // Center panel with list
        gameList = new ListView<>();
        gameList.setCellFactory(lv -> new SaveGameCell());
        gameList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                onLoad();
            }
        });
        VBox listContainer = new VBox(gameList);
        VBox.setVgrow(gameList, Priority.ALWAYS);
        root.setCenter(listContainer);

        // Bottom panel with buttons
        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER_LEFT);
        buttonPanel.setPadding(new Insets(10, 0, 0, 0));

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> onDelete());

        // Spacer to push Load/Cancel to the right
        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button loadButton = new Button("Load");
        loadButton.setOnAction(e -> onLoad());

        Button cancelButton = new Button("Cancel");
        cancelButton.setOnAction(e -> onCancel());

        buttonPanel
            .getChildren()
            .addAll(deleteButton, spacer, loadButton, cancelButton);
        root.setBottom(buttonPanel);

        Scene scene = new Scene(root, 600, 400);
        dialog.setScene(scene);
    }

    private void loadSaveFiles() {
        allSaves = new ArrayList<>();
        File dir = new File(saveDirectory);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles(
            (d, name) -> name.endsWith(".sav") || name.endsWith(".json")
        );

        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                SaveGameEntry entry = parseSaveFile(file);
                if (entry != null) {
                    allSaves.add(entry);
                }
            } catch (Exception e) {
                System.err.println(
                    "Failed to parse save file: " + file.getName()
                );
                e.printStackTrace();
            }
        }

        // Sort by timestamp descending (newest first)
        Collections.sort(
            allSaves,
            Comparator.comparing((SaveGameEntry e) -> e.timestamp).reversed()
        );

        refreshList();
    }

    private SaveGameEntry parseSaveFile(File file) throws IOException {
        String fileName = file.getName();
        boolean isZipFormat = fileName.endsWith(".sav");

        String gameName = null;
        long timestamp = 0;
        int turn = 0;
        String currentPlayer = "";

        if (isZipFormat) {
            // Extract from ZIP
            try (
                FileInputStream fis = new FileInputStream(file);
                ZipInputStream zis = new ZipInputStream(fis)
            ) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("game.json")) {
                        String json = new String(
                            readAllBytes(zis),
                            StandardCharsets.UTF_8
                        );
                        JsonObj root = (JsonObj) JsonParser.parse(json);

                        JsonObj boardInfo = root.getObj("board");
                        gameName = boardInfo.getString("saveName");
                        if (gameName == null) {
                            // Fallback: extract from filename
                            gameName = extractGameNameFromFilename(fileName);
                        }

                        timestamp = root.getLong("savedAt");
                        turn = root.getInteger("turn");

                        int currentPlayerIndex = root.getInteger(
                            "currentPlayerIndex"
                        );
                        Object[] playersArray = root.getArray("players");
                        if (
                            playersArray != null &&
                            currentPlayerIndex < playersArray.length
                        ) {
                            JsonObj playerJson =
                                (JsonObj) playersArray[currentPlayerIndex];
                            currentPlayer = playerJson.getString("name");
                        }
                        break;
                    }
                }
            }
        } else {
            // Legacy JSON format
            String json = new String(
                java.nio.file.Files.readAllBytes(file.toPath())
            );
            JsonObj root = (JsonObj) JsonParser.parse(json);

            gameName = extractGameNameFromFilename(fileName);
            timestamp = root.getLong("savedAt");
            turn = root.getInteger("turn");

            int currentPlayerIndex = root.getInteger("currentPlayerIndex");
            Object[] playersArray = root.getArray("players");
            if (
                playersArray != null && currentPlayerIndex < playersArray.length
            ) {
                JsonObj playerJson = (JsonObj) playersArray[currentPlayerIndex];
                currentPlayer = playerJson.getString("name");
            }
        }

        return new SaveGameEntry(
            file,
            gameName,
            timestamp,
            turn,
            currentPlayer
        );
    }

    private String extractGameNameFromFilename(String filename) {
        // Remove extension
        filename = filename.replace(".sav", "").replace(".json", "");

        // Remove timestamp pattern: _yyyy-MM-dd_HH-mm-ss
        // Pattern: _2024-01-16_14-30-45
        int lastUnderscore = filename.lastIndexOf('_');
        if (lastUnderscore > 0) {
            String potentialTimestamp = filename.substring(lastUnderscore + 1);
            // Check if it looks like HH-mm-ss
            if (potentialTimestamp.matches("\\d{2}-\\d{2}-\\d{2}")) {
                filename = filename.substring(0, lastUnderscore);
                // Remove date part too
                lastUnderscore = filename.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String potentialDate = filename.substring(
                        lastUnderscore + 1
                    );
                    if (potentialDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        filename = filename.substring(0, lastUnderscore);
                    }
                }
            }
        }

        return filename;
    }

    private byte[] readAllBytes(ZipInputStream zis) throws IOException {
        java.io.ByteArrayOutputStream buffer =
            new java.io.ByteArrayOutputStream();
        byte[] temp = new byte[4096];
        int bytesRead;
        while ((bytesRead = zis.read(temp)) != -1) {
            buffer.write(temp, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

    private void refreshList() {
        gameList.getItems().clear();

        if (showAllVersionsCheckbox.isSelected()) {
            // Show all versions
            for (SaveGameEntry entry : allSaves) {
                gameList.getItems().add(entry);
            }
        } else {
            // Show only most recent version of each game
            Map<String, SaveGameEntry> latestByName = new HashMap<>();
            for (SaveGameEntry entry : allSaves) {
                String name = entry.gameName;
                if (
                    !latestByName.containsKey(name) ||
                    entry.timestamp > latestByName.get(name).timestamp
                ) {
                    latestByName.put(name, entry);
                }
            }

            // Add to list, sorted by name
            List<SaveGameEntry> sortedList = new ArrayList<>(
                latestByName.values()
            );
            Collections.sort(sortedList, Comparator.comparing(e -> e.gameName));

            for (SaveGameEntry entry : sortedList) {
                gameList.getItems().add(entry);
            }
        }
    }

    private void onLoad() {
        SaveGameEntry selected = gameList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a game to load.");
            alert.showAndWait();
            return;
        }

        selectedFile = selected.file;
        dialog.close();
    }

    private void onDelete() {
        SaveGameEntry selected = gameList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a game to delete.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Are you sure you want to delete this save?\n" + selected.gameName,
            ButtonType.YES,
            ButtonType.NO
        );
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            if (selected.file.delete()) {
                allSaves.remove(selected);
                refreshList();
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Deleted");
                success.setHeaderText(null);
                success.setContentText("Save file deleted successfully.");
                success.showAndWait();
            } else {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error");
                error.setHeaderText(null);
                error.setContentText("Failed to delete save file.");
                error.showAndWait();
            }
        }
    }

    private void onCancel() {
        selectedFile = null;
        dialog.close();
    }

    public void showAndWait() {
        dialog.showAndWait();
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    /**
     * Represents a saved game entry.
     */
    private static class SaveGameEntry {

        final File file;
        final String gameName;
        final long timestamp;
        final int turn;
        final String currentPlayer;

        SaveGameEntry(
            File file,
            String gameName,
            long timestamp,
            int turn,
            String currentPlayer
        ) {
            this.file = file;
            this.gameName = gameName;
            this.timestamp = timestamp;
            this.turn = turn;
            this.currentPlayer = currentPlayer;
        }
    }

    /**
     * Custom cell renderer for save game entries.
     */
    private static class SaveGameCell extends ListCell<SaveGameEntry> {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss"
        );

        @Override
        protected void updateItem(SaveGameEntry entry, boolean empty) {
            super.updateItem(entry, empty);

            if (empty || entry == null) {
                setText(null);
                setGraphic(null);
            } else {
                VBox content = new VBox(2);

                Label nameLabel = new Label(entry.gameName);
                nameLabel.setFont(Font.font(null, FontWeight.BOLD, 12));

                String dateStr = dateFormat.format(new Date(entry.timestamp));
                Label detailsLabel = new Label(
                    String.format(
                        "Turn %d | %s | %s",
                        entry.turn,
                        entry.currentPlayer,
                        dateStr
                    )
                );
                detailsLabel.setFont(Font.font(10));
                detailsLabel.setStyle("-fx-text-fill: gray;");

                content.getChildren().addAll(nameLabel, detailsLabel);
                setGraphic(content);
                setText(null);
            }
        }
    }
}
