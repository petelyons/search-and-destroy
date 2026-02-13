package com.developingstorm.games.sad.ui;

import com.developingstorm.games.sad.util.json.JsonObj;
import com.developingstorm.games.sad.util.json.JsonParser;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

/**
 * Dialog for loading saved games.
 * Groups save files by game name and shows only the most recent by default.
 */
public class SaveGameDialog extends JDialog {

    private File selectedFile;
    private final String saveDirectory;
    private JList<SaveGameEntry> gameList;
    private DefaultListModel<SaveGameEntry> listModel;
    private JCheckBox showAllVersionsCheckbox;
    private List<SaveGameEntry> allSaves;

    public SaveGameDialog(JFrame parent, String saveDirectory) {
        super(parent, "Load Game", true);
        this.saveDirectory = saveDirectory;
        initComponents();
        loadSaveFiles();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 400);
        setLocationRelativeTo(getParent());

        // Top panel with checkbox
        JPanel topPanel = new JPanel(new BorderLayout());
        showAllVersionsCheckbox = new JCheckBox("Show all versions");
        showAllVersionsCheckbox.addActionListener(e -> refreshList());
        topPanel.add(showAllVersionsCheckbox, BorderLayout.WEST);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(topPanel, BorderLayout.NORTH);

        // Center panel with list
        listModel = new DefaultListModel<>();
        gameList = new JList<>(listModel);
        gameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gameList.setCellRenderer(new SaveGameCellRenderer());

        JScrollPane scrollPane = new JScrollPane(gameList);
        scrollPane.setBorder(
            BorderFactory.createTitledBorder("Available Saved Games")
        );
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with buttons
        JPanel buttonPanel = new JPanel();

        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> onLoad());
        buttonPanel.add(loadButton);

        JButton deleteButton = new JButton("Delete");
        deleteButton.addActionListener(e -> onDelete());
        buttonPanel.add(deleteButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> onCancel());
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Double-click to load
        gameList.addMouseListener(
            new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        onLoad();
                    }
                }
            }
        );
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
        listModel.clear();

        if (showAllVersionsCheckbox.isSelected()) {
            // Show all versions
            for (SaveGameEntry entry : allSaves) {
                listModel.addElement(entry);
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
                listModel.addElement(entry);
            }
        }
    }

    private void onLoad() {
        SaveGameEntry selected = gameList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a game to load.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        selectedFile = selected.file;
        dispose();
    }

    private void onDelete() {
        SaveGameEntry selected = gameList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a game to delete.",
                "No Selection",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this save?\n" + selected.gameName,
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (selected.file.delete()) {
                allSaves.remove(selected);
                refreshList();
                JOptionPane.showMessageDialog(
                    this,
                    "Save file deleted successfully.",
                    "Deleted",
                    JOptionPane.INFORMATION_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Failed to delete save file.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void onCancel() {
        selectedFile = null;
        dispose();
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
    private static class SaveGameCellRenderer extends DefaultListCellRenderer {

        private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss"
        );

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(
                list,
                value,
                index,
                isSelected,
                cellHasFocus
            );

            if (value instanceof SaveGameEntry) {
                SaveGameEntry entry = (SaveGameEntry) value;
                String dateStr = dateFormat.format(new Date(entry.timestamp));
                String text = String.format(
                    "<html><b>%s</b><br/><small>Turn %d | %s | %s</small></html>",
                    entry.gameName,
                    entry.turn,
                    entry.currentPlayer,
                    dateStr
                );
                setText(text);
            }

            return this;
        }
    }
}
