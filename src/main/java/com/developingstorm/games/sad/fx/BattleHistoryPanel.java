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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Panel that displays a scrollable history of recent combat encounters.
 * Each battle is shown as a compact icon-based row with attacker and defender.
 */
public class BattleHistoryPanel extends VBox {

    private final List<CombatResult> battleHistory;
    private final VBox battleListPanel;
    private final ScrollPane scrollPane;

    public interface BattleSelectionListener {
        void battleSelected(Location location);
    }

    private BattleSelectionListener selectionListener;

    public BattleHistoryPanel() {
        this.battleHistory = new ArrayList<>();
        this.battleListPanel = new VBox(3);
        this.battleListPanel.setPadding(new Insets(5));

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
        Region separator = new Region();
        separator.setPrefHeight(1);
        separator.setStyle("-fx-background-color: #555;");

        // Battle list in scroll pane
        scrollPane = new ScrollPane(battleListPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle(
            "-fx-background: #2b2b2b; -fx-background-color: #2b2b2b;"
        );
        scrollPane.setPrefHeight(200);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(titleLabel, separator, scrollPane);

        showEmptyState();
    }

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
     * Add a new battle to the history. Most recent at top.
     */
    public void addBattle(CombatResult result) {
        if (result == null) {
            return;
        }

        if (battleHistory.isEmpty()) {
            battleListPanel.getChildren().clear();
        }

        battleHistory.add(result);

        HBox battleRow = createBattleRow(result);
        battleListPanel.getChildren().add(0, battleRow);

        // Limit history to 50 battles
        if (battleHistory.size() > 50) {
            battleHistory.remove(0);
            battleListPanel
                .getChildren()
                .remove(battleListPanel.getChildren().size() - 1);
        }

        scrollPane.setVvalue(0);
    }

    private Color getPlayerColor(Player player) {
        int playerId = player.getId();
        if (playerId == 1) {
            return Color.rgb(250, 100, 100);
        } else if (playerId == 2) {
            return Color.rgb(150, 150, 250);
        } else {
            return Color.rgb(100, 250, 100);
        }
    }

    private HBox createBattleRow(CombatResult result) {
        HBox row = new HBox(6);
        row.setPadding(new Insets(4));
        row.setAlignment(Pos.CENTER);
        row.setStyle("-fx-background-color: #333;");
        row.setMinHeight(45);

        boolean attackerWon = result.attackerWon();

        // Attacker side
        VBox attackerBox = createUnitBox(
            result.getAttackerTypeEnum(),
            result.getAttackerOwner(),
            result.getAttackerInitialHits(),
            result.getAttackerFinalHits(),
            attackerWon
        );

        // Arrow indicator pointing toward the loser
        String arrowText = attackerWon ? ">>>" : "<<<";
        Label arrowLabel = new Label(arrowText);
        arrowLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        arrowLabel.setTextFill(Color.GOLD);

        // Defender side
        VBox defenderBox = createUnitBox(
            result.getDefenderTypeEnum(),
            result.getDefenderOwner(),
            result.getDefenderInitialHits(),
            result.getDefenderFinalHits(),
            !attackerWon
        );

        row.getChildren().addAll(attackerBox, arrowLabel, defenderBox);

        if (selectionListener != null) {
            row.setOnMouseClicked(e -> {
                selectionListener.battleSelected(result.getBattleLocation());
            });
            row.setCursor(javafx.scene.Cursor.HAND);
        }

        return row;
    }

    private VBox createUnitBox(
        com.developingstorm.games.sad.Type unitType,
        Player owner,
        int initialHits,
        int finalHits,
        boolean isWinner
    ) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);

        // Unit icon
        Image unitImage = TerrainImages.getInstance().getUnitImage(unitType);
        ImageView iconView = new ImageView(unitImage);
        iconView.setFitWidth(24);
        iconView.setFitHeight(24);
        iconView.setPreserveRatio(true);
        iconView.setSmooth(true);

        StackPane iconPane = new StackPane(iconView);
        iconPane.setPrefSize(28, 28);
        iconPane.setMinSize(28, 28);
        iconPane.setMaxSize(28, 28);
        iconPane.setAlignment(Pos.CENTER);

        Color playerColor = getPlayerColor(owner);
        iconPane.setBackground(
            new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(
                    playerColor,
                    new javafx.scene.layout.CornerRadii(2),
                    Insets.EMPTY
                )
            )
        );

        if (isWinner) {
            iconPane.setStyle(
                "-fx-border-color: gold; -fx-border-width: 2; -fx-border-radius: 2;"
            );
        } else {
            iconPane.setStyle(
                "-fx-border-color: #555; -fx-border-width: 1; -fx-border-radius: 2;"
            );
        }

        // Health text: initialHits→finalHits
        Label hitsLabel = new Label(initialHits + "\u2192" + finalHits);
        hitsLabel.setFont(Font.font("System", 9));
        hitsLabel.setTextFill(isWinner ? Color.LIGHTGREEN : Color.LIGHTCORAL);

        box.getChildren().addAll(iconPane, hitsLabel);
        return box;
    }

    public void clearHistory() {
        battleHistory.clear();
        battleListPanel.getChildren().clear();
        showEmptyState();
    }

    public int getBattleCount() {
        return battleHistory.size();
    }
}
