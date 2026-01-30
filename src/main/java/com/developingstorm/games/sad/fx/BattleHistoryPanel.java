package com.developingstorm.games.sad.fx;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.CombatResult;
import com.developingstorm.games.sad.Player;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Panel that displays a scrollable history of recent combat encounters.
 * Each battle is shown as a single row with information about attacker and defender.
 *
 * Subscribes to CombatResolvedEvent to update automatically.
 */
public class BattleHistoryPanel extends VBox {

    private final List<CombatResult> battleHistory;
    private final VBox battleListPanel;
    private final ScrollPane scrollPane;

    // Optional listener for when user clicks on a battle
    public interface BattleSelectionListener {
        void battleSelected(Location location);
    }

    private BattleSelectionListener selectionListener;

    public BattleHistoryPanel() {
        this.battleHistory = new ArrayList<>();
        this.battleListPanel = new VBox(5);
        this.battleListPanel.setPadding(new Insets(5));

        // Initialize UI
        setSpacing(5);
        setPadding(new Insets(10));
        setStyle("-fx-background-color: #2b2b2b;");

        // Title
        Label titleLabel = new Label("Battle History");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        // Separator
        Region separator = createSeparator();

        // Battle list in scroll pane
        scrollPane = new ScrollPane(battleListPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background: #2b2b2b; -fx-background-color: #2b2b2b;"
        );
        scrollPane.setPrefHeight(200);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(titleLabel, separator, scrollPane);

        // Show empty state initially
        showEmptyState();
    }

    private Region createSeparator() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #555;");
        return sep;
    }

    /**
     * Set listener for battle selection clicks.
     */
    public void setBattleSelectionListener(BattleSelectionListener listener) {
        this.selectionListener = listener;
    }

    private void showEmptyState() {
        battleListPanel.getChildren().clear();
        Label emptyLabel = new Label("No battles yet");
        emptyLabel.setFont(Font.font("System", FontWeight.LIGHT, 12));
        emptyLabel.setTextFill(Color.GRAY);
        emptyLabel.setAlignment(Pos.CENTER);
        emptyLabel.setMaxWidth(Double.MAX_VALUE);
        emptyLabel.setPadding(new Insets(20));
        battleListPanel.getChildren().add(emptyLabel);
    }

    /**
     * Add a new battle to the history.
     * Most recent battles appear at the top.
     */
    public void addBattle(CombatResult result) {
        if (result == null) {
            return;
        }

        // If this is the first battle, clear empty state
        if (battleHistory.isEmpty()) {
            battleListPanel.getChildren().clear();
        }

        // Add to history list
        battleHistory.add(result);

        // Create battle row and add to top of display
        VBox battleRow = createBattleRow(result);
        battleListPanel.getChildren().add(0, battleRow);

        // Limit history to 50 battles
        if (battleHistory.size() > 50) {
            battleHistory.remove(0);
            battleListPanel
                .getChildren()
                .remove(battleListPanel.getChildren().size() - 1);
        }

        // Auto-scroll to top to show latest battle
        scrollPane.setVvalue(0);
    }

    private VBox createBattleRow(CombatResult result) {
        VBox row = new VBox(3);
        row.setPadding(new Insets(5));
        row.setStyle(
            "-fx-background-color: #333; -fx-border-color: #555; -fx-border-width: 0 0 1 0;"
        );

        // Attacker info
        Label attackerLabel = new Label(
            result.getAttackerName() + " (" + result.getAttackerOwner() + ")"
        );
        attackerLabel.setTextFill(
            result.attackerWon() ? Color.LIGHTGREEN : Color.LIGHTCORAL
        );
        attackerLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

        // Health info for attacker
        String attackerHealth = String.format(
            "  %d → %d hits",
            result.getAttackerInitialHits(),
            result.getAttackerFinalHits()
        );
        Label attackerHealthLabel = new Label(attackerHealth);
        attackerHealthLabel.setTextFill(Color.LIGHTGRAY);
        attackerHealthLabel.setFont(Font.font("System", 10));

        // VS
        Label vsLabel = new Label("    VS");
        vsLabel.setTextFill(Color.GRAY);
        vsLabel.setFont(Font.font("System", FontWeight.BOLD, 10));

        // Defender info
        Label defenderLabel = new Label(
            result.getDefenderName() + " (" + result.getDefenderOwner() + ")"
        );
        defenderLabel.setTextFill(
            !result.attackerWon() ? Color.LIGHTGREEN : Color.LIGHTCORAL
        );
        defenderLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

        // Health info for defender
        String defenderHealth = String.format(
            "  %d → %d hits",
            result.getDefenderInitialHits(),
            result.getDefenderFinalHits()
        );
        Label defenderHealthLabel = new Label(defenderHealth);
        defenderHealthLabel.setTextFill(Color.LIGHTGRAY);
        defenderHealthLabel.setFont(Font.font("System", 10));

        // Outcome
        String outcome = result.attackerWon() ? "Attacker Won" : "Defender Won";
        Label outcomeLabel = new Label("  → " + outcome);
        outcomeLabel.setTextFill(Color.YELLOW);
        outcomeLabel.setFont(Font.font("System", FontWeight.BOLD, 10));

        row
            .getChildren()
            .addAll(
                attackerLabel,
                attackerHealthLabel,
                vsLabel,
                defenderLabel,
                defenderHealthLabel,
                outcomeLabel
            );

        // Make clickable if listener is set
        if (selectionListener != null) {
            row.setOnMouseClicked(e -> {
                selectionListener.battleSelected(result.getBattleLocation());
            });
            row.setStyle(row.getStyle() + "; -fx-cursor: hand;");
        }

        return row;
    }

    /**
     * Clear all battle history.
     */
    public void clearHistory() {
        battleHistory.clear();
        battleListPanel.getChildren().clear();
        showEmptyState();
    }

    /**
     * Get number of battles in history.
     */
    public int getBattleCount() {
        return battleHistory.size();
    }
}
